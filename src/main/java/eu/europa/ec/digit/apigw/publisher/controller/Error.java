package eu.europa.ec.digit.apigw.publisher.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;

@RestController
public class Error implements ErrorController {

    private static final String PATH = "/error";

    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping(value = "/error")
    public String error(HttpServletRequest request, WebRequest webRequest, HttpServletResponse response) {
        Map<String, Object> attributes = getErrorAttributes(webRequest, true);
        JSONObject error = new JSONObject();
        error.put("status", response.getStatus());
        Iterator<String> iterator = attributes.keySet().iterator();
        while(iterator.hasNext()) {
            String key = iterator.next();
            if(attributes.get(key) instanceof String) {
                error.put(key, attributes.get(key));
            }

        }
        return  error.toString();
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }

    private Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
        return this.errorAttributes.getErrorAttributes(webRequest, includeStackTrace);

    }
}