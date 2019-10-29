package eu.europa.ec.digit.apigw.publisher.entity;

import java.util.List;

public class CorsConfiguration {

    private List<String> accessControlAllowOrigins;
    private List<String> accessControlAllowHeaders;
    private List<String> accessControlAllowMethods;

    public List<String> getAccessControlAllowOrigins() {
        return accessControlAllowOrigins;
    }

    public void setAccessControlAllowOrigins(List<String> accessControlAllowOrigins) {
        this.accessControlAllowOrigins = accessControlAllowOrigins;
    }

    public List<String> getAccessControlAllowHeaders() {
        return accessControlAllowHeaders;
    }

    public void setAccessControlAllowHeaders(List<String> accessControlAllowHeaders) {
        this.accessControlAllowHeaders = accessControlAllowHeaders;
    }

    public List<String> getAccessControlAllowMethods() {
        return accessControlAllowMethods;
    }

    public void setAccessControlAllowMethods(List<String> accessControlAllowMethods) {
        this.accessControlAllowMethods = accessControlAllowMethods;
    }
}
