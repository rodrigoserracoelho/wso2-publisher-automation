package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

@Data
public class ApplicationKey {

    private String consumerSecret;
    private String consumerKey;
    private String keyState;
    private String keyType;
    private boolean callError;
    private String callErrorMessage;
    private String callID;

}
