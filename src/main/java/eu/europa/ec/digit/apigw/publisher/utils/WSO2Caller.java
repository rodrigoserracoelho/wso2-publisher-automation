package eu.europa.ec.digit.apigw.publisher.utils;

import eu.europa.ec.digit.apigw.publisher.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;


@Component
@Slf4j
public class WSO2Caller {

    @Autowired
    Authorization authorization;

    @Autowired
    RestTemplate restTemplate;

    @Value("${wso2.api.publisher.endpoint}")
    private String wso2ApiPublisherEndpoint;

    @Value("${wso2.api.store.endpoint}")
    private String wso2ApiStoreEndpoint;

    public ResponseEntity<Version> searchForVersion(String apiName, HttpServletRequest request) {
        return searchForVersion(apiName, 100, request);
    }

    /*public ResponseEntity<Version> searchForVersion(Map<String, String> callParameters, HttpServletRequest request) {
        return searchForVersion(callParameters, 100, request);
    }*/

    public ResponseEntity<Version> searchForVersion(Map<String, String> callParameters, int limit, HttpServletRequest request){
        try {
            if(limit <= 0) limit = 25;
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getViewAccessToken(request);
            if(encodedAuthorization == null) {
                Version errorVersion = new Version();
                errorVersion.setCallError(true);
                errorVersion.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorVersion, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            String queryParam = "name:" + callParameters.get(Constants.API_NAME_PARAMETER);
            String query = wso2ApiPublisherEndpoint + "?query=" + queryParam + "&limit=" + limit;
            return restTemplate.exchange(query, HttpMethod.GET, new HttpEntity(headers), Version.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<Version> searchForVersion(String apiName, int limit, HttpServletRequest request){
        try {
            if(limit <= 0) limit = 25;
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getViewAccessToken(request);
            if(encodedAuthorization == null) {
                Version errorVersion = new Version();
                errorVersion.setCallError(true);
                errorVersion.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorVersion, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            String queryParam = "name:" + apiName;
            String query = wso2ApiPublisherEndpoint + "?query=" + queryParam + "&limit=" + limit;
            return restTemplate.exchange(query, HttpMethod.GET, new HttpEntity(headers), Version.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> getApiDetails(String apiId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getViewAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing credentials.", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/" + apiId, HttpMethod.GET, new HttpEntity(headers), String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("There was a problem getting the API: " + apiId + " , try again later on.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /*public ResponseEntity<String> getApiDetailsByName(String apiIdentifier, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getViewAccessToken(request);

            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing credentials.", HttpStatus.UNAUTHORIZED);
            }
            log.info(encodedAuthorization);
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return getRestTemplate().exchange(wso2ApiPublisherEndpoint + "/" + apiIdentifier, HttpMethod.GET, new HttpEntity(headers), String.class);
        } catch (Exception e) {
            return new ResponseEntity<>("There was a problem getting the API: " + apiIdentifier + " , try again later on.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }*/

    public ResponseEntity<Api> getApi(String apiId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getViewAccessToken(request);
            if(encodedAuthorization == null) {
                Api errorApi = new Api();
                errorApi.setCallError(true);
                errorApi.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApi, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/" + apiId, HttpMethod.GET, new HttpEntity(headers), Api.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("There was a problem getting the API, try again later on.");
            return new ResponseEntity<>(errorApi, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public ResponseEntity<Api> createNewVersion(String apiId, String newVersion, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getCreateAccessToken(request);
            if(encodedAuthorization == null) {
                Api errorApi = new Api();
                errorApi.setCallError(true);
                errorApi.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApi, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/copy-api?apiId=" + apiId + "&newVersion=" + newVersion, HttpMethod.POST, new HttpEntity(headers), Api.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("Version could not be created, check if this version exists already: " + newVersion);
            return new ResponseEntity<>(errorApi, HttpStatus.CONFLICT);
        }
    }

    public ResponseEntity<Api> updateDefinition(String apiId, String templatePayload, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getCreateAccessToken(request);
            if(encodedAuthorization == null) {
                Api errorApi = new Api();
                errorApi.setCallError(true);
                errorApi.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApi, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(templatePayload, headers);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/" + apiId, HttpMethod.PUT, entity, Api.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("New version with ID: " + apiId + " could not be updated, please check the configuration.");
            return new ResponseEntity<>(errorApi, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> publishApi(String apiId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getPublishAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing credentials", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/change-lifecycle?apiId=" + apiId + "&action=Publish", HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("API with ID: " + apiId + " could not be published", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> deleteApi(String apiId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getCreateAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing credentials", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/" + apiId, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("Requested API could not be deleted", HttpStatus.CONFLICT);
        }
    }

    public ResponseEntity<Api> createApi(String templatePayload, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getCreateAccessToken(request);
            if(encodedAuthorization == null) {
                Api errorApi = new Api();
                errorApi.setCallError(true);
                errorApi.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApi, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(templatePayload, headers);
            return restTemplate.exchange(wso2ApiPublisherEndpoint, HttpMethod.POST, entity, Api.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Api errorApi = new Api();
            errorApi.setCallError(true);
            errorApi.setCallErrorMessage("Requested API could not be created, please check your parameters.");
            return new ResponseEntity<>(errorApi, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> updateSwagger(String apiId, String swaggerDefinition, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getCreateAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing Credentials", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("apiDefinition", swaggerDefinition);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(wso2ApiPublisherEndpoint + "/" + apiId + "/swagger", HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("Requested API swagger could not be updated", HttpStatus.CONFLICT);
        }
    }

    public ResponseEntity<ApplicationList> searchApplications(HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                ApplicationList errorApplicationList = new ApplicationList();
                errorApplicationList.setCallError(true);
                errorApplicationList.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApplicationList, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/applications", HttpMethod.GET, new HttpEntity(headers), ApplicationList.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ApplicationList errorApplicationList = new ApplicationList();
            errorApplicationList.setCallError(true);
            errorApplicationList.setCallErrorMessage("Application list could be retrieved, try again later.");
            return new ResponseEntity<>(errorApplicationList, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<SubscriptionList> searchSubscriptionsForApplication(String applicationID, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                SubscriptionList errorList = new SubscriptionList();
                errorList.setCallError(true);
                errorList.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorList, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/subscriptions?applicationId=" + applicationID, HttpMethod.GET, new HttpEntity(headers), SubscriptionList.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            SubscriptionList errorList = new SubscriptionList();
            errorList.setCallError(true);
            errorList.setCallErrorMessage("Subscription list could be retrieved, try again later.");
            return new ResponseEntity<>(errorList, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<Application> getApplicationDetail(String applicationID, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                Application errorApplication = new Application();
                errorApplication.setCallError(true);
                errorApplication.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorApplication, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/applications/" + applicationID, HttpMethod.GET, new HttpEntity(headers), Application.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Application errorApplication = new Application();
            errorApplication.setCallError(true);
            errorApplication.setCallErrorMessage("Application could be retrieved, try again later.");
            return new ResponseEntity<>(errorApplication, HttpStatus.BAD_REQUEST);
        }
    }

    /*public ResponseEntity<SubscriptionList> searchSubscriptionsByApi(String apiId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                SubscriptionList errorSubscriptionList = new SubscriptionList();
                errorSubscriptionList.setCallError(true);
                errorSubscriptionList.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorSubscriptionList, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/subscriptions?apiId=" + apiId, HttpMethod.GET, new HttpEntity(headers), SubscriptionList.class);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }*/

    public ResponseEntity<Subscription> subscribe(String applicationId, String apiId, HttpServletRequest request) {
        try {
            JSONObject subscription = new JSONObject();
            subscription.put("tier", "Unlimited");
            subscription.put("apiIdentifier", apiId);
            subscription.put("applicationId", applicationId);

            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                Subscription errorSubscription = new Subscription();
                errorSubscription.setCallError(true);
                errorSubscription.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(errorSubscription, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(subscription.toString(), headers);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/subscriptions", HttpMethod.POST, entity, Subscription.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Subscription errorSubscription = new Subscription();
            errorSubscription.setCallError(true);
            errorSubscription.setCallErrorMessage("We were not able to subscribe to the API, please try again later.");
            return new ResponseEntity<>(errorSubscription, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> unsubscribe(String subscriptionId, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing authentication", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/subscriptions/" + subscriptionId, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("Requested API could not be unsubscribed", HttpStatus.CONFLICT);
        }
    }

    public ResponseEntity<ApplicationKey> generateKey(String applicationID, int validityTime, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                ApplicationKey applicationKey = new ApplicationKey();
                applicationKey.setCallError(true);
                applicationKey.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(applicationKey, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONArray accessDomains = new JSONArray();
            accessDomains.put("ALL");
            JSONObject application = new JSONObject();
            application.put("validityTime", validityTime+"");
            application.put("keyType", "PRODUCTION");
            application.put("accessAllowDomains", accessDomains);

            HttpEntity<String> entity = new HttpEntity<>(application.toString(), headers);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/applications/generate-keys?applicationId=" + applicationID, HttpMethod.POST, entity, ApplicationKey.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ApplicationKey applicationKey = new ApplicationKey();
            applicationKey.setCallError(true);
            applicationKey.setCallErrorMessage("Application count not be created");
            return new ResponseEntity<>(applicationKey, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<Application> createApplication(String applicationName, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                Application application = new Application();
                application.setCallError(true);
                application.setCallErrorMessage("Missing authentication");
                return new ResponseEntity<>(application, HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject application = new JSONObject();
            application.put("throttlingTier", "Unlimited");
            application.put("name", applicationName);

            HttpEntity<String> entity = new HttpEntity<>(application.toString(), headers);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/applications", HttpMethod.POST, entity, Application.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Application application = new Application();
            application.setCallError(true);
            application.setCallErrorMessage("Application count not be created");
            return new ResponseEntity<>(application, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> removeApplication(String applicationID, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String encodedAuthorization = authorization.getSubscribeAccessToken(request);
            if(encodedAuthorization == null) {
                return new ResponseEntity<>("Missing authentication", HttpStatus.UNAUTHORIZED);
            }
            headers.set("Authorization", "Bearer " + encodedAuthorization);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(wso2ApiStoreEndpoint + "/applications/" + applicationID, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>("Application count not be created", HttpStatus.BAD_REQUEST);
        }
    }
}
