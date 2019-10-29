package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionList {

    private int count;
    private List<Subscription>  list;
    private boolean callError;
    private String callErrorMessage;
    private String callID;
}
