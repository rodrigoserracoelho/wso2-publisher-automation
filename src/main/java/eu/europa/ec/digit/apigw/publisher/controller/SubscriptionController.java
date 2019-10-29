package eu.europa.ec.digit.apigw.publisher.controller;

import eu.europa.ec.digit.apigw.publisher.entity.*;
import eu.europa.ec.digit.apigw.publisher.security.CallTracer;
import eu.europa.ec.digit.apigw.publisher.utils.RESTServiceUtils;
import eu.europa.ec.digit.apigw.publisher.utils.WSO2Caller;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/subscription")
@io.swagger.annotations.Api(value = "Subscription", tags = {"Subscription"})
public class SubscriptionController {

    @Autowired
    private WSO2Caller wso2Caller;

    @Autowired
    private CallTracer callTracer;

    @Autowired
    private RESTServiceUtils restServiceUtils;

    @ApiOperation(value = "Get all the subscriptions of an Application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Subscriptions", response = SubscriptionList.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(path="/application/{applicationID}")
    public ResponseEntity<SubscriptionList> getSubscriptionsByApplication(@PathVariable String applicationID, HttpServletRequest request) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<SubscriptionList> listCall = wso2Caller.searchSubscriptionsForApplication(applicationID, request);
        listCall.getBody().setCallID(callID);
        return listCall;
    }

    @ApiOperation(value = "Subscribe an API with a given api ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PutMapping(path="/{applicationID}/apis")
    public ResponseEntity<SubscriptionList> subscribeAppToApi(@PathVariable String applicationID, @RequestBody ApiList apiList, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        SubscriptionList subscriptionList = new SubscriptionList();
        subscriptionList.setList(new ArrayList<>());
        if(apiList.getIdList() != null) {
            for(String apiID : apiList.getIdList()) {
                ResponseEntity<Subscription> subscribeCall = wso2Caller.subscribe(applicationID, apiID, request);
                subscribeCall.getBody().setCallID(callID);
                subscriptionList.getList().add(subscribeCall.getBody());
            }
        }
        callTracer.fromResponse(request, response, HttpStatus.OK);
        return new ResponseEntity<>(subscriptionList, HttpStatus.OK);
    }

    @ApiOperation(value = "Adds an origin to all the subscribed APIs by an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PutMapping(path="/{applicationID}/apis/origin")
    public ResponseEntity<SubscriptionList> addOriginToAllApis(@PathVariable String applicationID, @RequestParam String origin, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<SubscriptionList> listCall = wso2Caller.searchSubscriptionsForApplication(applicationID, request);
        if(!listCall.getStatusCode().is2xxSuccessful()) {
            listCall.getBody().setCallID(callID);
            return listCall;
        }

        for(Subscription subscription : listCall.getBody().getList()) {
            //start stupid workaround
            String apiName = restServiceUtils.getApiNameFromIdentifier(subscription.getApiIdentifier());
            String version = restServiceUtils.getApiVersionFromIdentifier(subscription.getApiIdentifier());
            //end stupid workaround
            ResponseEntity<Version> detailsCall = wso2Caller.searchForVersion(apiName, 10, request);
            if(!detailsCall.getStatusCode().is2xxSuccessful()) {
                subscription.setCallError(true);
                subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                subscription.setCallID(callID);
            } else {
                for(Api api : detailsCall.getBody().getList()) {
                    if(api.getVersion().equals(version)) {
                        log.info(api.getVersion() + " - " + version + " - " + api.getId());
                        ResponseEntity<String> apiCall = wso2Caller.getApiDetails(api.getId(), request);
                        log.info(apiCall.getStatusCode().toString());
                        if(apiCall.getStatusCode().is2xxSuccessful()) {
                            JSONObject apiObject = new JSONObject(apiCall.getBody());
                            if(apiObject.has("corsConfiguration")) {
                                JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
                                JSONArray accessControlAllowOrigins = corsConfiguration.getJSONArray("accessControlAllowOrigins");
                                if(accessControlAllowOrigins.length() == 1 && accessControlAllowOrigins.get(0).equals("*")) {
                                    accessControlAllowOrigins.remove(0);
                                }
                                accessControlAllowOrigins.put(origin);
                            }
                            ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(api.getId(), apiObject.toString(), request);
                            updateDefinitionCall.getBody().setCallID(callID);
                            if(!updateDefinitionCall.getStatusCode().is2xxSuccessful()) {
                                subscription.setCallError(true);
                                subscription.setCallErrorMessage("The api could not be updated");
                            }
                        } else {
                            subscription.setCallError(true);
                            subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                            subscription.setCallID(callID);
                        }
                    }
                }
            }
        }
        listCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, listCall.getStatusCode());
        return new ResponseEntity<>(listCall.getBody(), HttpStatus.OK);
    }

    @ApiOperation(value = "Removes an origin to all the subscribed APIs by an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @DeleteMapping(path="/{applicationID}/apis/origin")
    public ResponseEntity<SubscriptionList> removeOriginToAllApis(@PathVariable String applicationID, @RequestParam String origin, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<SubscriptionList> listCall = wso2Caller.searchSubscriptionsForApplication(applicationID, request);
        if(!listCall.getStatusCode().is2xxSuccessful()) {
            listCall.getBody().setCallID(callID);
            return listCall;
        }

        for(Subscription subscription : listCall.getBody().getList()) {
            //start stupid workaround
            String apiName = restServiceUtils.getApiNameFromIdentifier(subscription.getApiIdentifier());
            String version = restServiceUtils.getApiVersionFromIdentifier(subscription.getApiIdentifier());
            //end stupid workaround
            ResponseEntity<Version> detailsCall = wso2Caller.searchForVersion(apiName, 10, request);
            if(!detailsCall.getStatusCode().is2xxSuccessful()) {
                subscription.setCallError(true);
                subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                subscription.setCallID(callID);
            } else {
                for(Api api : detailsCall.getBody().getList()) {
                    if(api.getVersion().equals(version)) {
                        log.info(api.getVersion() + " - " + version + " - " + api.getId());
                        ResponseEntity<String> apiCall = wso2Caller.getApiDetails(api.getId(), request);
                        log.info(apiCall.getStatusCode().toString());
                        if(apiCall.getStatusCode().is2xxSuccessful()) {
                            JSONObject apiObject = new JSONObject(apiCall.getBody());
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
                            ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(api.getId(), apiObject.toString(), request);
                            updateDefinitionCall.getBody().setCallID(callID);
                            if(!updateDefinitionCall.getStatusCode().is2xxSuccessful()) {
                                subscription.setCallError(true);
                                subscription.setCallErrorMessage("The api could not be updated");
                            }
                        } else {
                            subscription.setCallError(true);
                            subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                            subscription.setCallID(callID);
                        }
                    }
                }
            }
        }
        listCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, listCall.getStatusCode());
        return new ResponseEntity<>(listCall.getBody(), HttpStatus.OK);
    }

    @ApiOperation(value = "Adds a header to all the subscribed APIs by an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @PutMapping(path="/{applicationID}/apis/header")
    public ResponseEntity<SubscriptionList> addHeaderToAllApis(@PathVariable String applicationID, @RequestParam String header, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<SubscriptionList> listCall = wso2Caller.searchSubscriptionsForApplication(applicationID, request);
        if(!listCall.getStatusCode().is2xxSuccessful()) {
            listCall.getBody().setCallID(callID);
            return listCall;
        }

        for(Subscription subscription : listCall.getBody().getList()) {
            //start stupid workaround
            String apiName = restServiceUtils.getApiNameFromIdentifier(subscription.getApiIdentifier());
            String version = restServiceUtils.getApiVersionFromIdentifier(subscription.getApiIdentifier());
            //end stupid workaround
            ResponseEntity<Version> detailsCall = wso2Caller.searchForVersion(apiName, 10, request);
            if(!detailsCall.getStatusCode().is2xxSuccessful()) {
                subscription.setCallError(true);
                subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                subscription.setCallID(callID);
            } else {
                for(Api api : detailsCall.getBody().getList()) {
                    if(api.getVersion().equals(version)) {
                        ResponseEntity<String> apiCall = wso2Caller.getApiDetails(api.getId(), request);
                        if(apiCall.getStatusCode().is2xxSuccessful()) {
                            JSONObject apiObject = new JSONObject(apiCall.getBody());
                            if(apiObject.has("corsConfiguration")) {
                                JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
                                JSONArray accessControlAllowHeaders = corsConfiguration.getJSONArray("accessControlAllowHeaders");
                                boolean headerAlreadyExists = false;
                                for(int i = 0; i < accessControlAllowHeaders.length(); i++){
                                    if(accessControlAllowHeaders.getString(i).equalsIgnoreCase(header.trim()))
                                        headerAlreadyExists = true;
                                }
                                if(!headerAlreadyExists) {
                                    accessControlAllowHeaders.put(header.trim());
                                }
                            }
                           ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(api.getId(), apiObject.toString(), request);
                            updateDefinitionCall.getBody().setCallID(callID);
                            if(!updateDefinitionCall.getStatusCode().is2xxSuccessful()) {
                                subscription.setCallError(true);
                                subscription.setCallErrorMessage("The api could not be updated");
                            }
                        } else {
                            subscription.setCallError(true);
                            subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                            subscription.setCallID(callID);
                        }
                    }
                }
            }
        }
        listCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, listCall.getStatusCode());
        return new ResponseEntity<>(listCall.getBody(), HttpStatus.OK);
    }

    @ApiOperation(value = "Removes a header from all the subscribed APIs by an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @DeleteMapping(path="/{applicationID}/apis/header")
    public ResponseEntity<SubscriptionList> removeHeaderToAllApis(@PathVariable String applicationID, @RequestParam String header, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<SubscriptionList> listCall = wso2Caller.searchSubscriptionsForApplication(applicationID, request);
        if(!listCall.getStatusCode().is2xxSuccessful()) {
            listCall.getBody().setCallID(callID);
            return listCall;
        }

        for(Subscription subscription : listCall.getBody().getList()) {
            //start stupid workaround
            String apiName = restServiceUtils.getApiNameFromIdentifier(subscription.getApiIdentifier());
            String version = restServiceUtils.getApiVersionFromIdentifier(subscription.getApiIdentifier());
            //end stupid workaround
            ResponseEntity<Version> detailsCall = wso2Caller.searchForVersion(apiName, 10, request);
            if(!detailsCall.getStatusCode().is2xxSuccessful()) {
                subscription.setCallError(true);
                subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                subscription.setCallID(callID);
            } else {
                for(Api api : detailsCall.getBody().getList()) {
                    if(api.getVersion().equals(version)) {
                        log.info(api.getVersion() + " - " + version + " - " + api.getId());
                        ResponseEntity<String> apiCall = wso2Caller.getApiDetails(api.getId(), request);
                        log.info(apiCall.getStatusCode().toString());
                        if(apiCall.getStatusCode().is2xxSuccessful()) {
                            JSONObject apiObject = new JSONObject(apiCall.getBody());
                            if(apiObject.has("corsConfiguration")) {
                                JSONObject corsConfiguration = apiObject.getJSONObject("corsConfiguration");
                                JSONArray accessControlAllowHeaders = corsConfiguration.getJSONArray("accessControlAllowHeaders");
                                for(int i=0; i<accessControlAllowHeaders.length(); i++) {
                                    if(((String)accessControlAllowHeaders.get(i)).equalsIgnoreCase(header)) {
                                        accessControlAllowHeaders.remove(i);
                                    }
                                }
                            }
                            ResponseEntity<Api> updateDefinitionCall = wso2Caller.updateDefinition(api.getId(), apiObject.toString(), request);
                            updateDefinitionCall.getBody().setCallID(callID);
                            if(!updateDefinitionCall.getStatusCode().is2xxSuccessful()) {
                                subscription.setCallError(true);
                                subscription.setCallErrorMessage("The api could not be updated");
                            }
                        } else {
                            subscription.setCallError(true);
                            subscription.setCallErrorMessage("There was a problem getting your API, please try again.");
                            subscription.setCallID(callID);
                        }
                    }
                }
            }
        }
        listCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, listCall.getStatusCode());
        return new ResponseEntity<>(listCall.getBody(), HttpStatus.OK);
    }
}