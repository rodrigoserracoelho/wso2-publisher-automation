package eu.europa.ec.digit.apigw.publisher.entity;

import lombok.Data;

@Data
public class AliasInfo {
    private String alias;
    private String issuerDN;
    private String subjectDN;
}