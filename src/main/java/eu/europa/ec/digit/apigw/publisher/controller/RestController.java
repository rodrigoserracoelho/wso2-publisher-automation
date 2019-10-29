package eu.europa.ec.digit.apigw.publisher.controller;

import eu.europa.ec.digit.apigw.publisher.entity.Api;
import eu.europa.ec.digit.apigw.publisher.entity.NewApi;
import eu.europa.ec.digit.apigw.publisher.entity.Version;
import eu.europa.ec.digit.apigw.publisher.security.CallTracer;
import eu.europa.ec.digit.apigw.publisher.utils.Constants;
import eu.europa.ec.digit.apigw.publisher.utils.RESTServiceUtils;
import eu.europa.ec.digit.apigw.publisher.utils.ThymeleafConfiguration;
import eu.europa.ec.digit.apigw.publisher.utils.WSO2Caller;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/rest")
@io.swagger.annotations.Api(value = "REST Publisher", tags = {"REST Publisher"}, description = "Use these services to manage your REST deployments on the API Gateway")
@Slf4j
public class RestController {

    @Autowired
    RESTServiceUtils restServiceUtils;

    @Autowired
    SpringTemplateEngine templateEngine;

    @Autowired
    WSO2Caller wso2Caller;

    @Autowired
    CallTracer callTracer;

    @Autowired
    RestTemplate restTemplate;

    @ApiOperation(value = "Publish a created REST API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "API Published"),
            @ApiResponse(code = 400, message = "Bad request")
    })

    @PutMapping(path="/publish/{apiId}")
    public ResponseEntity<String> publishApi(@PathVariable String apiId, HttpServletRequest request, HttpServletResponse response) {
        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<String> publishCall = wso2Caller.publishApi(apiId, request);
        callTracer.fromResponse(request, response, publishCall.getStatusCode());
        return wso2Caller.publishApi(apiId, request);
    }

    @ApiOperation(value = "Publish REST API to WSO2")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "API Deployed"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 412, message = "Pre Condition failed")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Api> create(@RequestBody NewApi api, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Api apiToPublish = null;
        log.info("INCOMING REQUEST TO PUBLISH");

        try {
            if(!restServiceUtils.isCallValid(api, false)) {
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Missing parameters.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<Version> previousVersionsCall = wso2Caller.searchForVersion(api.getApiName(), request);
            if(previousVersionsCall.getStatusCode().is2xxSuccessful()) {
                List<Api> existingApis = previousVersionsCall.getBody().getList();
                for(Api existingApi : existingApis) {
                    if(existingApi.getName().equals(api.getApiName())) {
                        apiToPublish = existingApi;
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("There is already a published version with the same context, please create new version from this ID");
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.PRECONDITION_FAILED);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.PRECONDITION_FAILED);
                    }
                }

                Context apiTemplateContext = new Context();
                apiTemplateContext.setVariable(ThymeleafConfiguration.NAME, api.getApiName());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DESCRIPTION, api.getApiDescription());
                apiTemplateContext.setVariable(ThymeleafConfiguration.CONTEXT, api.getApiContext());
                apiTemplateContext.setVariable(ThymeleafConfiguration.VERSION, api.getApiVersion());
                apiTemplateContext.setVariable(ThymeleafConfiguration.PRODUCTION_ENDPOINT, api.getEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DEFAULT_VERSION, api.isDefaultVersion());
                apiTemplateContext.setVariable(Constants.API_ALLOWED_ORIGIN, api.getApiAllowedOrigins());
                String jsonApiTemplate = templateEngine.process("create-api-template.json", apiTemplateContext);

                Context swaggerTemplateContext = new Context();
                swaggerTemplateContext.setVariable(ThymeleafConfiguration.NAME, api.getApiName());
                String swaggerTemplate = templateEngine.process("default_swagger_definition.json", swaggerTemplateContext);

                log.info("Template and Swagger ready!");

                ResponseEntity<Api> createApiCall = wso2Caller.createApi(jsonApiTemplate, request);
                if(!createApiCall.getStatusCode().is2xxSuccessful()) {
                    apiToPublish = new Api();
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("There was a problem creating your API, please try again.");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, createApiCall.getStatusCode());
                    return new ResponseEntity<>(apiToPublish, createApiCall.getStatusCode());
                }

                apiToPublish = createApiCall.getBody();

                if(api.getSwaggerEndpoint() != null) {
                    ResponseEntity<String> swaggerDefinition = getSwaggerDefinition(api.getSwaggerEndpoint());
                    if(swaggerDefinition.getStatusCode().is2xxSuccessful()) {
                        swaggerTemplate = swaggerDefinition.getBody();
                    } else {
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("There was a problem calling your swagger endpoint, but the REST API was created, please check your Swagger Endpoint and update the API with ID: " + apiToPublish.getId());
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.PRECONDITION_FAILED);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.PRECONDITION_FAILED);
                    }
                }

                log.info("Updating swagger difinition for API: {}", apiToPublish.getId());

                ResponseEntity<String> updateSwaggerCall = wso2Caller.updateSwagger(apiToPublish.getId(), swaggerTemplate, request);
                if(!updateSwaggerCall.getStatusCode().is2xxSuccessful()) {
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("There was a problem updating API with ID: " + apiToPublish.getId() + " with the new Swagger definition, please review your Swagger and try again by updating the API.");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                    return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
                }

                ResponseEntity<String> publishResult = wso2Caller.publishApi(apiToPublish.getId(), request);
                if(publishResult.getStatusCode().is2xxSuccessful()) {
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.OK);
                    return new ResponseEntity<>(apiToPublish, HttpStatus.OK);
                } else {
                    /** The user must delete the returned ID and try again the operation */
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("The API was created but failed to publish");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                /** The user must retry again, probably a communication problem with the API Gateway */
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Error looking for previous versions.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.SERVICE_UNAVAILABLE);
                return new ResponseEntity<>(apiToPublish, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch(Exception e) {
            apiToPublish = new Api();
            apiToPublish.setCallError(true);
            apiToPublish.setCallErrorMessage("Unknown error, please contact support team with the call ID");
            apiToPublish.setCallID(callID);
            callTracer.setSpanTag(request,"http.exception", e.getMessage());
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Create a new version of an existing REST API to WSO2")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New REST Version Deployed"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Api> createNewRestVersion(@RequestBody NewApi api, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Api apiToPublish = null;
        log.info("INCOMING REQUEST TO CREATE A NEW VERSION OF EXISTING REST SERVICE");

        try {
            if(!restServiceUtils.isCallValid(api, true)) {
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Missing parameters.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<Api> createNewVersionCall = wso2Caller.createNewVersion(api.getParentApiId(), api.getApiVersion(), request);
            if(createNewVersionCall.getStatusCode().is2xxSuccessful()) {
                Context apiTemplateContext = new Context();
                apiTemplateContext.setVariable(ThymeleafConfiguration.NAME, api.getApiName());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DESCRIPTION, api.getApiDescription());
                apiTemplateContext.setVariable(ThymeleafConfiguration.CONTEXT, api.getApiContext());
                apiTemplateContext.setVariable(ThymeleafConfiguration.VERSION, api.getApiVersion());
                apiTemplateContext.setVariable(ThymeleafConfiguration.PRODUCTION_ENDPOINT, api.getEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DEFAULT_VERSION, api.isDefaultVersion());
                apiTemplateContext.setVariable(Constants.API_ALLOWED_ORIGIN, api.getApiAllowedOrigins());
                String jsonApiTemplate = templateEngine.process("create-api-template.json", apiTemplateContext);

                Context swaggerTemplateContext = new Context();
                swaggerTemplateContext.setVariable(ThymeleafConfiguration.NAME, api.getApiName());
                String swaggerTemplate = templateEngine.process("default_swagger_definition.json", swaggerTemplateContext);

                ResponseEntity<Api> updateNewVersionCall = wso2Caller.updateDefinition(createNewVersionCall.getBody().getId(), jsonApiTemplate, request);
                if(updateNewVersionCall.getStatusCode().is2xxSuccessful()) {

                    if(api.getSwaggerEndpoint() != null) {
                        ResponseEntity<String> swaggerDefinition = getSwaggerDefinition(api.getSwaggerEndpoint());
                        if(swaggerDefinition.getStatusCode().is2xxSuccessful()) {
                            swaggerTemplate = swaggerDefinition.getBody();
                        } else {
                            apiToPublish.setCallError(true);
                            apiToPublish.setCallErrorMessage("There was a problem calling your swagger endpoint, but the REST API was created, please check your Swagger Endpoint and update the API with ID: " + apiToPublish.getId());
                            apiToPublish.setCallID(callID);
                            callTracer.fromResponse(request, response, HttpStatus.PRECONDITION_FAILED);
                            return new ResponseEntity<>(apiToPublish, HttpStatus.PRECONDITION_FAILED);
                        }
                    }

                    log.info("Updating swagger difinition for API: {}", updateNewVersionCall.getBody().getId());

                    ResponseEntity<String> updateSwaggerCall = wso2Caller.updateSwagger(updateNewVersionCall.getBody().getId(), swaggerTemplate, request);
                    if(!updateSwaggerCall.getStatusCode().is2xxSuccessful()) {
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("There was a problem updating API with ID: " + apiToPublish.getId() + " with the new Swagger definition, please review your Swagger and try again by updating the API.");
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
                    }

                    ResponseEntity<String> publishApiCall = wso2Caller.publishApi(updateNewVersionCall.getBody().getId(), request);
                    if(publishApiCall.getStatusCode().is2xxSuccessful()) {
                        return new ResponseEntity<>(updateNewVersionCall.getBody(), HttpStatus.OK);
                    } else {
                        /** The user must delete the returned ID and try again the operation */
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("The API was updated but failed to publish");
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    }
                } else {
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("There was a problem updating your new version, please delete and try to create again.");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, updateNewVersionCall.getStatusCode());
                    return updateNewVersionCall;
                }
            } else {
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("There was a problem creating a new version of the API, please try again.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, createNewVersionCall.getStatusCode());
                return createNewVersionCall;
            }
        } catch(Exception e) {
            apiToPublish = new Api();
            apiToPublish.setCallError(true);
            apiToPublish.setCallErrorMessage("Unknown error, please contact support team with the call ID");
            apiToPublish.setCallID(callID);
            //if(e != null)  callTracer.setSpanTag(request,"http.exception", e.getMessage());
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Delete an API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "API Deleted"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @DeleteMapping(path="/{apiId}")
    public ResponseEntity<Api> deleteApi(@PathVariable String apiId, HttpServletRequest request, HttpServletResponse response) {
        Api deletedApi = new Api();

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<String> deleteCall =  wso2Caller.deleteApi(apiId, request);
        if(deleteCall.getStatusCode().is2xxSuccessful()) {
            deletedApi.setCallID(callID);
        } else {
            deletedApi.setCallError(true);
            deletedApi.setCallErrorMessage("There was an error deleting the API, please try again later.");
            deletedApi.setCallID(callID);
        }
        callTracer.fromResponse(request, response, deleteCall.getStatusCode());
        return new ResponseEntity<>(deletedApi, deleteCall.getStatusCode());
    }

    @ApiOperation(value = "Add an Origin to your CORS configuration")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added origin"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PutMapping(path="/{apiId}/origin")
    public ResponseEntity<Api> addCorsOrigin(@PathVariable String apiId, @RequestParam String origin, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        if(origin == null) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("Missing parameters.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(errorApi, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> detailsCall = wso2Caller.getApiDetails(apiId, request);
        if(!detailsCall.getStatusCode().is2xxSuccessful()) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("There was a problem getting your API, please try again.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, detailsCall.getStatusCode());
            return new ResponseEntity<>(errorApi, detailsCall.getStatusCode());
        }

        JSONObject apiObject = new JSONObject(detailsCall.getBody());
        if(apiObject.has("corsConfiguration")) {
            JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
            JSONArray accessControlAllowOrigins = corsConfiguration.getJSONArray("accessControlAllowOrigins");
            if(accessControlAllowOrigins.length() == 1 && accessControlAllowOrigins.get(0).equals("*")) {
                accessControlAllowOrigins.remove(0);
            }
            accessControlAllowOrigins.put(origin);

        }
        ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(apiId, apiObject.toString(), request);
        updateDefinitionCall.getBody().setCallID(callID);

        callTracer.fromResponse(request, response, updateDefinitionCall.getStatusCode());
        return updateDefinitionCall;
    }

    @ApiOperation(value = "Remove an Origin from your CORS configuration")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added origin"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @DeleteMapping(path="/{apiId}/origin")
    public ResponseEntity<Api> removeCorsOrigin(@PathVariable String apiId, @RequestParam String origin, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        if(origin == null) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("Missing parameters.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(errorApi, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> detailsCall = wso2Caller.getApiDetails(apiId, request);
        if(!detailsCall.getStatusCode().is2xxSuccessful()) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("There was a problem getting your API, please try again.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, detailsCall.getStatusCode());
            return new ResponseEntity<>(errorApi, detailsCall.getStatusCode());
        }

        JSONObject apiObject = new JSONObject(detailsCall.getBody());
        if(apiObject.has("corsConfiguration")) {
            JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
            JSONArray accessControlAllowOrigins = corsConfiguration.getJSONArray("accessControlAllowOrigins");
            for(int i=0; i<accessControlAllowOrigins.length(); i++) {
                String persistedOrigin = (String) accessControlAllowOrigins.get(i);
                if(persistedOrigin.equals(origin)) {
                    accessControlAllowOrigins.remove(i);
                }
            }
        }
        ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(apiId, apiObject.toString(), request);
        updateDefinitionCall.getBody().setCallID(callID);

        callTracer.fromResponse(request, response, updateDefinitionCall.getStatusCode());
        return updateDefinitionCall;
    }

    @ApiOperation(value = "Add an authorized header to your CORS configuration")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added header"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PutMapping(path="/{apiId}/header/{header}")
    public ResponseEntity<Api> addHeader(@PathVariable String apiId, @PathVariable String header, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<String> detailsCall = wso2Caller.getApiDetails(apiId, request);
        if(!detailsCall.getStatusCode().is2xxSuccessful()) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("There was a problem getting your API, please try again.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, detailsCall.getStatusCode());
            return new ResponseEntity<>(errorApi, detailsCall.getStatusCode());
        }

        JSONObject apiObject = new JSONObject(detailsCall.getBody());
        if(apiObject.has("corsConfiguration")) {
            JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
            JSONArray accessControlAllowHeaders = corsConfiguration.getJSONArray("accessControlAllowHeaders");
            boolean headerAlreadyExists = false;
            for(int i = 0; i < accessControlAllowHeaders.length(); i++){
                if(accessControlAllowHeaders.getString(i).equals(header))
                    headerAlreadyExists = true;
            }
            if(!headerAlreadyExists) {
                accessControlAllowHeaders.put(header);
            }
        }
        ResponseEntity<Api> updateCall = wso2Caller.updateDefinition(apiId, apiObject.toString(), request);
        if(updateCall.getStatusCode().is2xxSuccessful()) {
            updateCall.getBody().setCallID(callID);
        }
        callTracer.fromResponse(request, response, updateCall.getStatusCode());
        return updateCall;
    }

    @ApiOperation(value = "Remove an authorized header from your CORS configuration")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added header"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @DeleteMapping(path="/{apiId}/header/{header}")
    public ResponseEntity<Api> removeHeader(@PathVariable String apiId, @PathVariable String header, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<String> detailsCall = wso2Caller.getApiDetails(apiId, request);
        if(!detailsCall.getStatusCode().is2xxSuccessful()) {
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("There was a problem getting your API, please try again.");
            errorApi.setCallID(callID);
            callTracer.fromResponse(request, response, detailsCall.getStatusCode());
            return new ResponseEntity<>(errorApi, detailsCall.getStatusCode());
        }

        JSONObject apiObject = new JSONObject(detailsCall.getBody());
        if(apiObject.has("corsConfiguration")) {
            JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
            JSONArray accessControlAllowHeaders = corsConfiguration.getJSONArray("accessControlAllowHeaders");
            for(int i=0; i<accessControlAllowHeaders.length(); i++) {
                if(((String)accessControlAllowHeaders.get(i)).equalsIgnoreCase(header)) {
                    accessControlAllowHeaders.remove(i);
                }
            }
        }
        ResponseEntity<Api> updateCall = wso2Caller.updateDefinition(apiId, apiObject.toString(), request);
        if(updateCall.getStatusCode().is2xxSuccessful()) {
            updateCall.getBody().setCallID(callID);
        }
        callTracer.fromResponse(request, response, updateCall.getStatusCode());
        return updateCall;
    }

    @ApiOperation(value = "Get details of an API by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Api Info", response = Api.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(path="/{apiId}")
    public ResponseEntity<Api> getApiById(@PathVariable String apiId, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<Api> getApiCall = wso2Caller.getApi(apiId, request);

        callTracer.fromResponse(request, response, getApiCall.getStatusCode());
        return getApiCall;
    }

    @ApiOperation(value = "Get details of an API by Name")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query",value = "Name or Name with *", required = true, dataType = "string", name = Constants.API_NAME_PARAMETER),
            @ApiImplicitParam(paramType = "query", value = "return limit size", required = false, dataType = "string", name = Constants.API_QUERY_LIMIT)
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Version returned", response = Version.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping
    public ResponseEntity<Version> getApiByName(HttpServletRequest request, HttpServletResponse response) {
        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Map<String, String> searchParameters = restServiceUtils.getSearchParameters(request);
        if(searchParameters == null) {
            Version version = new Version();
            version.setCallError(true);
            version.setCallErrorMessage("Missing parameters.");
            version.setCallID(callID);
            return new ResponseEntity<>(version, HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<Version> searchCall = wso2Caller.searchForVersion(searchParameters, restServiceUtils.getLimit(searchParameters), request);
        searchCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, searchCall.getStatusCode());
        return searchCall;
    }

    private ResponseEntity<String> getSwaggerDefinition(String swaggerEndpoint) {
        try {
            return restTemplate.getForEntity(swaggerEndpoint, String.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
