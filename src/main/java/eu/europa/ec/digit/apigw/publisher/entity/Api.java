package eu.europa.ec.digit.apigw.publisher.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Api {

    private String id;
    private String name;
    private String description;
    private String context;
    private String version;
    private String provider;
    private String status;
    private String visibility;
    private List<Sequence> sequences;
    private CorsConfiguration corsConfiguration;
    private BusinessInformation businessInformation;
    private String gatewayEnvironments;
    private String endpointConfig;
    @JsonProperty(value="isDefaultVersion")
    private boolean defaultVersion;
    private boolean callError;
    private String callErrorMessage;
    private String callID;
    private String wsdlUri;
}
