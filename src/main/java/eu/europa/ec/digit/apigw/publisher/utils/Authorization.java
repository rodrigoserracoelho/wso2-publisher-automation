package eu.europa.ec.digit.apigw.publisher.utils;

import eu.europa.ec.digit.apigw.publisher.entity.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class Authorization {

    @Autowired
    RestTemplate restTemplate;

    @Value("${wso2.api.publisher.authentication.endpoint}")
    private String wso2AuthenticationEndpoint;

   public String getViewAccessToken(HttpServletRequest request) {
       try {
           HttpEntity<MultiValueMap<String, String>> theRequest = new HttpEntity<>(buildCallParameters(Constants.WSO2_VIEW_SCOPE), buildHeader(request));
           ResponseEntity response = restTemplate.postForEntity(wso2AuthenticationEndpoint, theRequest , AccessToken.class);
           if(response.getStatusCode().equals(HttpStatus.OK)) {
               AccessToken theToken = (AccessToken) response.getBody();
               return theToken.getAccess_token();
           }
       } catch (Exception e) {
            log.error(e.getMessage(), e);
       }
       return null;
   }

   public String getCreateAccessToken(HttpServletRequest request) {
       try {
           HttpEntity<MultiValueMap<String, String>> theRequest = new HttpEntity<>(buildCallParameters(Constants.WSO2_CREATE_SCOPE), buildHeader(request));
           ResponseEntity response = restTemplate.postForEntity(wso2AuthenticationEndpoint, theRequest , AccessToken.class);
           if(response.getStatusCode().equals(HttpStatus.OK)) {
               AccessToken theToken = (AccessToken) response.getBody();
               return theToken.getAccess_token();
           }
       } catch (Exception e) {
            log.error(e.getMessage(), e);
       }
       return null;
   }

   public String getPublishAccessToken(HttpServletRequest request) {
       try {
           HttpEntity<MultiValueMap<String, String>> theRequest = new HttpEntity<>(buildCallParameters(Constants.WSO2_PUBLISH_SCOPE), buildHeader(request));
           ResponseEntity response = restTemplate.postForEntity(wso2AuthenticationEndpoint, theRequest , AccessToken.class);
           if(response.getStatusCode().equals(HttpStatus.OK)) {
               AccessToken theToken = (AccessToken) response.getBody();
               return theToken.getAccess_token();
           }
       } catch (Exception e) {
            log.error(e.getMessage(), e);
       }
       return null;
   }

    public String getSubscribeAccessToken(HttpServletRequest request) {
        try {
            HttpEntity<MultiValueMap<String, String>> theRequest = new HttpEntity<>(buildCallParameters(Constants.WSO2_SUBSCRIBE_SCOPE), buildHeader(request));
            ResponseEntity response = restTemplate.postForEntity(wso2AuthenticationEndpoint, theRequest , AccessToken.class);
            if(response.getStatusCode().equals(HttpStatus.OK)) {
                AccessToken theToken = (AccessToken) response.getBody();
                return theToken.getAccess_token();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private MultiValueMap<String, String> buildCallParameters(String scopeType) {
        MultiValueMap<String, String> callParameters = new LinkedMultiValueMap<>();
        callParameters.add("grant_type", Constants.WSO2_CLIENT_CREDENTIALS);
        callParameters.add("scope", scopeType);
        return callParameters;
    }

    private HttpHeaders buildHeader(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", request.getHeader("Authorization"));
        return headers;
    }
}
