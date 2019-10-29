package eu.europa.ec.digit.apigw.publisher.security;

import brave.Span;
import brave.Tracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import eu.europa.ec.digit.apigw.publisher.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

@Slf4j
@Component
public class CallTracer {

    @Value("${zipkin.endpoint}")
    private String zipkinEndpoint;

    @Value("${zipkin.service.name}")
    private String zipkinServiceName;

    private HttpServerHandler handler;
    private HttpTracing tracing;
    private Tracing braveTracing;

    public void fromRequest(HttpServletRequest request, String uuid) {
        initTracers();
        TraceContext.Extractor extractor = braveTracing.propagation().extractor(MESSAGE_CONTEXT);
        Span span = handler.handleReceive(extractor, request);
        span.tag("http.call.id", uuid);
        String userID = getUserFromAuthorization(request);
        if(userID != null) {
            span.tag("http.basic.user", userID);
        }
        request.setAttribute("SPAN", span);
    }

    public void fromResponse(HttpServletRequest request, HttpServletResponse response, HttpStatus status) {
        Span span = (Span) request.getAttribute("SPAN");
        span.tag("http.code", status.toString());
        handler.handleSend(response, null, span);
    }

    public void setSpanTag(HttpServletRequest request, String key, String value) {
        Span span = (Span) request.getAttribute("SPAN");
        span.tag(key, value);
    }

    private synchronized void initTracers() {
        if (handler == null) {
            Sender sender = OkHttpSender.create(zipkinEndpoint + Constants.ZIPKIN_API_V2_URL);
            Reporter reporter = AsyncReporter.create(sender);
            braveTracing = Tracing.newBuilder()
                    .localServiceName(zipkinServiceName)
                    .propagationFactory(B3Propagation.FACTORY)
                    .spanReporter(reporter)
                    .supportsJoin(false)
                    .build();
            }
            tracing = HttpTracing.create(braveTracing);
            handler = HttpServerHandler.create(tracing, new ZipkinServerAdapter());
    }

    private String getUserFromAuthorization(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if(authorizationHeader != null) {
            String decodedAuthentication = new String(Base64.getDecoder().decode(authorizationHeader.substring("Basic ".length())));
            return decodedAuthentication.split(":")[0];
        } else {
            return null;
        }
    }

    static final Propagation.Getter<HttpServletRequest, String> MESSAGE_CONTEXT =
            (request, key) -> {
                if(request.getHeader(key) != null) {
                    return request.getHeader(key);
                }
                else {
                    return null;
                }
            };
}
