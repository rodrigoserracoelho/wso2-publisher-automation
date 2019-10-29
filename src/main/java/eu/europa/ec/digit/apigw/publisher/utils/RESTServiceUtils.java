package eu.europa.ec.digit.apigw.publisher.utils;

import eu.europa.ec.digit.apigw.publisher.entity.NewApi;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
public class RESTServiceUtils {

    public boolean isCallValid(NewApi api, boolean newVersion) {
        if(api.getApiName() == null ||
                api.getApiContext() == null ||
                api.getApiVersion() == null ||
                api.getEndpoint() == null ||
                api.getApiDescription() == null) {
            return false;
        }
        if(newVersion && api.getParentApiId() == null) {
            return false;
        }
        if(api.getApiAllowedOrigins() == null || api.getApiAllowedOrigins().isEmpty()) {
            api.setApiAllowedOrigins("[]");
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

    public String getApiNameFromIdentifier(String apiIdentifier) {
        String[] identifierArray = apiIdentifier.split("-");
        if(identifierArray.length > 3) {
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<identifierArray.length; i++) {
                if(i>0 && i<identifierArray.length) {
                    builder.append(identifierArray[i]);
                }
            }
            return builder.toString();
        } else {
            return identifierArray[1];
        }
    }

    public String getApiVersionFromIdentifier(String apiIdentifier) {
        String[] identifierArray = apiIdentifier.split("-");
        return identifierArray[identifierArray.length-1];
    }
}
