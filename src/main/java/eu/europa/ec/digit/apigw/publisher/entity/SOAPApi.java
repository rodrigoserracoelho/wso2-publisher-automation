package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

@Data
public class SOAPApi {

    private String apiName;
    private String apiContext;
    private String apiVersion;
    private String apiDescription;
    private boolean defaultVersion;
    private String wsdlEndpoint;
    private String endpoint;
    private String parentApiId;

}
