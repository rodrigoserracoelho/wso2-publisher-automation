package eu.europa.ec.digit.apigw.publisher.controller;

import eu.europa.ec.digit.apigw.publisher.entity.Api;
import eu.europa.ec.digit.apigw.publisher.entity.SOAPApi;
import eu.europa.ec.digit.apigw.publisher.entity.Version;
import eu.europa.ec.digit.apigw.publisher.security.CallTracer;
import eu.europa.ec.digit.apigw.publisher.utils.Constants;
import eu.europa.ec.digit.apigw.publisher.utils.SOAPServiceUtils;
import eu.europa.ec.digit.apigw.publisher.utils.ThymeleafConfiguration;
import eu.europa.ec.digit.apigw.publisher.utils.WSO2Caller;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/soap")
@io.swagger.annotations.Api(value = "SOAP Publisher", tags = {"SOAP Publisher"}, description = "Use these services to manage your SOAP deployments on the API Gateway")
@Slf4j
public class SOAPController {

    @Autowired
    private SOAPServiceUtils soapServiceUtils;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private WSO2Caller wso2Caller;

    @Autowired
    private CallTracer callTracer;

    @ApiOperation(value = "Publish a created SOAP API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "API Published"),
            @ApiResponse(code = 400, message = "Bad request")
    })

    @PutMapping(path="/publish/{apiId}")
    public ResponseEntity<String> publishApi(@PathVariable String apiId, HttpServletRequest request) {
        return wso2Caller.publishApi(apiId, request);
    }

    @ApiOperation(value = "Publish SOAP API to WSO2")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "SOAP API Deployed"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PostMapping(consumes = {"application/json"})
    public ResponseEntity<Api> publishSoap(@RequestBody SOAPApi soapApi, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Api apiToPublish;
        log.info("INCOMING REQUEST TO PUBLISH SOAP");

        try {
            if(!soapServiceUtils.isCallValid(soapApi, false)) {
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Missing parameters.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<Version> previousVersionsCall = wso2Caller.searchForVersion(soapApi.getApiName(), request);
            if(previousVersionsCall.getStatusCode().is2xxSuccessful()) {
                List<Api> existingApis = previousVersionsCall.getBody().getList();
                for(Api api : existingApis) {
                    if(api.getName().equals(soapApi.getApiName())) {
                        apiToPublish = api;
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("There is already a published version with the same context, please create new version from this ID");
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.PRECONDITION_FAILED);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.PRECONDITION_FAILED);
                    }
                }

                Context apiTemplateContext = new Context();
                apiTemplateContext.setVariable(ThymeleafConfiguration.NAME, soapApi.getApiName());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DESCRIPTION, soapApi.getApiDescription() == null ? "---" : soapApi.getApiDescription());
                apiTemplateContext.setVariable(ThymeleafConfiguration.CONTEXT, soapApi.getApiContext());
                apiTemplateContext.setVariable(ThymeleafConfiguration.VERSION, soapApi.getApiVersion());
                apiTemplateContext.setVariable(ThymeleafConfiguration.PRODUCTION_ENDPOINT, soapApi.getEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.WSDL_URI, soapApi.getWsdlEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DEFAULT_VERSION, soapApi.isDefaultVersion());

                String jsonApiTemplate = templateEngine.process("create-soap-api-template.json", apiTemplateContext);

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

                if(wso2Caller.publishApi(apiToPublish.getId(), request).getStatusCode().equals(HttpStatus.OK)) {
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.OK);
                    return new ResponseEntity<>(apiToPublish, HttpStatus.OK);
                } else {
                    /* The user must delete the returned ID and try again the operation */
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("The API was created but failed to publish");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                /* The user must retry again, probably a communication problem with the API Gateway */
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Error looking for previous versions.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.SERVICE_UNAVAILABLE);
                return new ResponseEntity<>(apiToPublish, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            apiToPublish = new Api();
            apiToPublish.setCallError(true);
            apiToPublish.setCallErrorMessage("Unknown error, please contact support team with the call ID");
            apiToPublish.setCallID(callID);
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Create a new version of an existing SOAP API to WSO2")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New SOAP Version Deployed"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @PutMapping(consumes = {"application/json"})
    public ResponseEntity<Api> createNewSoapVersion(@RequestBody SOAPApi soapApi, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Api apiToPublish = null;
        log.info("INCOMING REQUEST TO CREATE A NEW VERSION OF EXISTING SOAP SERVICE");

        try {
            if(!soapServiceUtils.isCallValid(soapApi, true)) {
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Missing parameters.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<Api> createNewVersionCall = wso2Caller.createNewVersion(soapApi.getParentApiId(), soapApi.getApiVersion(), request);
            if(createNewVersionCall.getStatusCode().is2xxSuccessful()) {
                Context apiTemplateContext = new Context();
                apiTemplateContext.setVariable(ThymeleafConfiguration.NAME, soapApi.getApiName());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DESCRIPTION, soapApi.getApiDescription() == null ? "---" : soapApi.getApiDescription());
                apiTemplateContext.setVariable(ThymeleafConfiguration.CONTEXT, soapApi.getApiContext());
                apiTemplateContext.setVariable(ThymeleafConfiguration.VERSION, soapApi.getApiVersion());
                apiTemplateContext.setVariable(ThymeleafConfiguration.PRODUCTION_ENDPOINT, soapApi.getEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.WSDL_URI, soapApi.getWsdlEndpoint());
                apiTemplateContext.setVariable(ThymeleafConfiguration.DEFAULT_VERSION, soapApi.isDefaultVersion());

                String jsonApiTemplate = templateEngine.process("create-soap-api-template.json", apiTemplateContext);

                ResponseEntity<Api> updateNewVersionCall = wso2Caller.updateDefinition(createNewVersionCall.getBody().getId(), jsonApiTemplate, request);
                if(updateNewVersionCall.getStatusCode().is2xxSuccessful()) {
                    ResponseEntity<String> publishApiCall = wso2Caller.publishApi(updateNewVersionCall.getBody().getId(), request);
                    if(publishApiCall.getStatusCode().is2xxSuccessful()) {
                        apiToPublish = updateNewVersionCall.getBody();
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.OK);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.OK);
                    } else {
                        /* The user must delete the returned ID and try again the operation */
                        apiToPublish = new Api();
                        apiToPublish.setCallError(true);
                        apiToPublish.setCallErrorMessage("The API was updated but failed to publish");
                        apiToPublish.setCallID(callID);
                        callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                        return new ResponseEntity<>(apiToPublish, HttpStatus.BAD_REQUEST);
                    }
                } else {
                    apiToPublish = new Api();
                    apiToPublish.setCallError(true);
                    apiToPublish.setCallErrorMessage("Missing parameters.");
                    apiToPublish.setCallID(callID);
                    callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                    return new ResponseEntity<>(apiToPublish, updateNewVersionCall.getStatusCode());
                }
            } else {
                apiToPublish = new Api();
                apiToPublish.setCallError(true);
                apiToPublish.setCallErrorMessage("Missing parameters.");
                apiToPublish.setCallID(callID);
                callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(apiToPublish, createNewVersionCall.getStatusCode());
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

    @ApiOperation(value = "Delete an API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "API Deleted"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @DeleteMapping(path="/{apiId}")
    public ResponseEntity<Api> deleteApi(@PathVariable String apiId, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);
        Api deletedApi = new Api();
        deletedApi.setId(apiId);
        deletedApi.setCallID(callID);

        ResponseEntity<String> deleteApiResult = wso2Caller.deleteApi(apiId, request);
        if(!deleteApiResult.getStatusCode().is2xxSuccessful()) {
            deletedApi.setCallError(true);
            deletedApi.setCallErrorMessage("There was a problem deleting your API with ID: " + apiId);
            callTracer.fromResponse(request, response, deleteApiResult.getStatusCode());
            return new ResponseEntity<>(deletedApi, deleteApiResult.getStatusCode());
        }
        callTracer.fromResponse(request, response, deleteApiResult.getStatusCode());
        return new ResponseEntity<>(deletedApi, deleteApiResult.getStatusCode());
    }

    @ApiOperation(value = "Get details of a SOAP API by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Api Info", response = Api.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(path="/{apiId}")
    public ResponseEntity<Api> getApiById(@PathVariable String apiId, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Api apiInfo = new Api();
        ResponseEntity<Api> getApiResult = wso2Caller.getApi(apiId, request);
        if(!getApiResult.getStatusCode().is2xxSuccessful()) {
            apiInfo.setCallError(true);
        } else {
            apiInfo = getApiResult.getBody();
        }

        apiInfo.setCallID(callID);
        callTracer.fromResponse(request, response, getApiResult.getStatusCode());
        return new ResponseEntity<>(apiInfo, getApiResult.getStatusCode());
    }

    @ApiOperation(value = "Get details of a SOAP API by Name")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query",value = "Name or Name with *", required = true, dataType = "string", name = Constants.API_NAME_PARAMETER),
            @ApiImplicitParam(paramType = "query", value = "return limit size", dataType = "string", name = Constants.API_QUERY_LIMIT)
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Version returned", response = Version.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping
    public ResponseEntity<Version> getApiByName(HttpServletRequest request, HttpServletResponse response) {
        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        Map<String, String> searchParameters = soapServiceUtils.getSearchParameters(request);
        if(searchParameters == null) {
            Version version = new Version();
            version.setCallError(true);
            version.setCallErrorMessage("Missing parameters.");
            version.setCallID(callID);
            return new ResponseEntity<>(version, HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<Version> searchCall = wso2Caller.searchForVersion(searchParameters, soapServiceUtils.getLimit(searchParameters), request);
        Objects.requireNonNull(searchCall.getBody()).setCallID(callID);
        callTracer.fromResponse(request, response, searchCall.getStatusCode());
        return searchCall;
    }
}
