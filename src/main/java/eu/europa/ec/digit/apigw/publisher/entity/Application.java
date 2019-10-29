package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

import java.util.List;

@Data
public class Application {

    private String applicationId;
    private String name;
    private String subscriber;
    private String status;
    private String description;
    private boolean callError;
    private String callErrorMessage;
    private String callID;
    private List<ApplicationKey>  keys;

}
