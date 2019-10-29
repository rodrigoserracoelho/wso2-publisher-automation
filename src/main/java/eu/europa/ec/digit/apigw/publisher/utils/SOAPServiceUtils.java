package eu.europa.ec.digit.apigw.publisher.utils;

import eu.europa.ec.digit.apigw.publisher.entity.SOAPApi;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
public class SOAPServiceUtils {

    public boolean isCallValid(SOAPApi soapAPi, boolean newVersion) {
        if(soapAPi.getApiName() == null ||
                soapAPi.getApiContext() == null ||
                soapAPi.getApiVersion() == null ||
                soapAPi.getEndpoint() == null ||
                soapAPi.getWsdlEndpoint() == null) {
            return false;
        }
        if(newVersion && soapAPi.getParentApiId() == null) {
            return false;
        }
        return true;
    }

    public Map<String, String> getSearchParameters(HttpServletRequest request) {

        Map<String, String[]> parameters = request.getParameterMap();
        Map<String, String> callParameters = new HashMap<>();
        if(parameters.containsKey(Constants.API_NAME_PARAMETER)) {
            callParameters.put(Constants.API_NAME_PARAMETER, parameters.get(Constants.API_NAME_PARAMETER)[0]);
            if(parameters.containsKey(Constants.API_QUERY_LIMIT)) {
                callParameters.put(Constants.API_QUERY_LIMIT, parameters.get(Constants.API_QUERY_LIMIT)[0]);
            }
            return callParameters;
        } else {
            return null;
        }
    }

    public int getLimit(Map<String, String> callParameters){
        if(callParameters.containsKey(Constants.API_QUERY_LIMIT)){
            return Integer.parseInt(callParameters.get(Constants.API_QUERY_LIMIT));
        }
        else {
            return 100;
        }
    }


}
