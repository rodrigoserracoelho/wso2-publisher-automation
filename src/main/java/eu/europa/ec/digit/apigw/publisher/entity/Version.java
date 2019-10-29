package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

import java.util.List;

@Data
public class Version {

    private int count;
    private List<Api>  list;
    private boolean callError;
    private String callErrorMessage;
    private String callID;

}
