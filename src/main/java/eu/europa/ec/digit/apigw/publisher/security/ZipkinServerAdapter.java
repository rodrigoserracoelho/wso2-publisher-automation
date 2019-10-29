package eu.europa.ec.digit.apigw.publisher.security;

import brave.http.HttpServerAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ZipkinServerAdapter extends HttpServerAdapter<HttpServletRequest, HttpServletResponse> {

    @Override
    public String method(HttpServletRequest request) {
        return request.getMethod();
    }

    @Override
    public String url(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Override
    public String requestHeader(HttpServletRequest request, String key) {
        return request.getHeader(key);
    }

    @Override
    public Integer statusCode(HttpServletResponse response) {
        return response.getStatus();
    }
}
