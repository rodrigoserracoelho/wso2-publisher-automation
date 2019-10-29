package eu.europa.ec.digit.apigw.publisher.controller;

import eu.europa.ec.digit.apigw.publisher.entity.Application;
import eu.europa.ec.digit.apigw.publisher.entity.ApplicationKey;
import eu.europa.ec.digit.apigw.publisher.entity.ApplicationList;
import eu.europa.ec.digit.apigw.publisher.security.CallTracer;
import eu.europa.ec.digit.apigw.publisher.utils.Constants;
import eu.europa.ec.digit.apigw.publisher.utils.WSO2Caller;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@RestController
@RequestMapping("/application")
@io.swagger.annotations.Api(value = "Application Publisher", tags = {"Application Publisher"}, description = "Use these services to manage your Applications on the API Gateway")
@Slf4j
public class ApplicationController {

    @Autowired
    private WSO2Caller wso2Caller;

    @Autowired
    private CallTracer callTracer;

    @ApiOperation(value = "Create an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = Application.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "string", name = "application_name", paramType = "form"),
    })
    @PostMapping
    public ResponseEntity<Application> createApplication(HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        if(!request.getParameterMap().containsKey(Constants.APPLICATION_NAME_PARAMETER)) {
            Application errorApplication = new Application();
            errorApplication.setCallError(true);
            errorApplication.setCallErrorMessage("Missing parameters");
            errorApplication.setCallID(callID);
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(errorApplication, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<Application> createCall = wso2Caller.createApplication(request.getParameter(Constants.APPLICATION_NAME_PARAMETER), request);
        if(createCall.getStatusCode().is2xxSuccessful()) {
            callTracer.fromResponse(request, response, createCall.getStatusCode());
            createCall.getBody().setCallID(callID);
            return createCall;
        } else if(createCall.getStatusCode().equals(HttpStatus.CONFLICT) || createCall.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            Application errorApplication = new Application();
            errorApplication.setCallError(true);
            errorApplication.setCallErrorMessage("There is already an application with the same name for your user.");
            errorApplication.setCallID(callID);
            callTracer.fromResponse(request, response, createCall.getStatusCode());
            return new ResponseEntity<>(errorApplication, createCall.getStatusCode());
        } else {
            Application errorApplication = new Application();
            errorApplication.setCallError(true);
            errorApplication.setCallErrorMessage("There was a problem creating your application, please contact the support team.");
            errorApplication.setCallID(callID);
            callTracer.fromResponse(request, response, createCall.getStatusCode());
            return new ResponseEntity<>(errorApplication, createCall.getStatusCode());
        }

    }

    @ApiOperation(value = "Generate Keys for given Application ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Key Info", response = ApplicationKey.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(required = true, dataType = "string", name = "application_id", paramType = "form"),
            @ApiImplicitParam(required = true, dataType = "string", name = "token_validity_time", paramType = "form", example="3600")
    })
    @PostMapping(path="/keys")
    public ResponseEntity<ApplicationKey> generateKeysForApplication(HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        if(!request.getParameterMap().containsKey(Constants.APPLICATION_ID_PARAMETER) || !request.getParameterMap().containsKey(Constants.TOKEN_VALIDITY_TIME_PARAMETER)) {
            ApplicationKey errorApplicationKey = new ApplicationKey();
            errorApplicationKey.setCallError(true);
            errorApplicationKey.setCallErrorMessage("Missing parameters");
            errorApplicationKey.setCallID(callID);
            callTracer.fromResponse(request, response, HttpStatus.BAD_REQUEST);
            return new ResponseEntity<>(errorApplicationKey, HttpStatus.BAD_REQUEST);
        }

        int validityInTime = 3600;
        try {
          validityInTime = Integer.parseInt(request.getParameter(Constants.TOKEN_VALIDITY_TIME_PARAMETER));
        } catch (NumberFormatException nfe) {
            callTracer.setSpanTag(request, "http.call.parameter.invalid", Constants.TOKEN_VALIDITY_TIME_PARAMETER);
        }


        ResponseEntity<ApplicationKey> generateKeysCall = wso2Caller.generateKey(request.getParameter(Constants.APPLICATION_ID_PARAMETER), validityInTime, request);
        generateKeysCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, generateKeysCall.getStatusCode());
        return generateKeysCall;
    }

    @ApiOperation(value = "Delete an application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = String.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @DeleteMapping(path="/{applicationID}")
    public ResponseEntity<String> removeApplication(@PathVariable String applicationID, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<String> removeCall = wso2Caller.removeApplication(applicationID, request);
        callTracer.fromResponse(request, response, removeCall.getStatusCode());
        return removeCall;
    }

    @ApiOperation(value = "Get all the applications for the user")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application Info", response = ApplicationList.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping
    public ResponseEntity<ApplicationList> getAllApplications(HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<ApplicationList> searchCall = wso2Caller.searchApplications(request);
        searchCall.getBody().setCallID(callID);
        callTracer.fromResponse(request, response, searchCall.getStatusCode());
        return searchCall;
    }

    @ApiOperation(value = "Get the details of an Application")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Application detail", response = Application.class),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(path="/{applicationID}")
    public ResponseEntity<Application> getApplicationDetail(@PathVariable String applicationID, HttpServletRequest request, HttpServletResponse response) {

        String callID = UUID.randomUUID().toString();
        callTracer.fromRequest(request, callID);

        ResponseEntity<Application> getDetailCall = wso2Caller.getApplicationDetail(applicationID, request);
        getDetailCall.getBody().setCallID(callID);
        return getDetailCall;
    }
}
