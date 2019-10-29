package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

@Data
public class Subscription {

    private String subscriptionId;
    private String apiIdentifier;
    private String applicationId;
    private String tier;
    private boolean callError;
    private String callErrorMessage;
    private String callID;
}
