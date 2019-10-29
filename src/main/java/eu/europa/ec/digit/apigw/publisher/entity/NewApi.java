package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

import java.util.List;

@Data
public class NewApi {

    private String apiId;
    private String apiName;
    private String apiDescription;
    private String apiContext;
    private String apiVersion;
    private String apiAllowedOrigins;
    private String endpoint;
    private boolean defaultVersion;
    private boolean callError;
    private String swaggerEndpoint;
    private String callErrorMessage;
    private String callID;
    private String parentApiId;
}
