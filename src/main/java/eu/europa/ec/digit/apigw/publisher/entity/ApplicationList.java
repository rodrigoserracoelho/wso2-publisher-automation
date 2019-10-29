package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationList {

    private int count;
    private List<Application>  list;
    private boolean callError;
    private String callErrorMessage;
    private String callID;

}
