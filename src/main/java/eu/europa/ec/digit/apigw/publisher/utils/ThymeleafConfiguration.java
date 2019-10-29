package eu.europa.ec.digit.apigw.publisher.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import java.util.Collection;

@Configuration
public class ThymeleafConfiguration {

    public static final String TEMPLATES_BASE = "classpath:/templates/";

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CONTEXT = "context";
    public static final String VERSION = "version";
    public static final String PRODUCTION_ENDPOINT = "production_endpoint";
    public static final String WSDL_URI = "wsdlUri";
    public static final String DEFAULT_VERSION = "defaultVersion";

    @Bean
    public SpringResourceTemplateResolver jsonMessageTemplateResolver() {
        SpringResourceTemplateResolver theResourceTemplateResolver = new SpringResourceTemplateResolver();
        theResourceTemplateResolver.setPrefix(TEMPLATES_BASE);
        theResourceTemplateResolver.setSuffix(".json");
        theResourceTemplateResolver.setTemplateMode("json");
        theResourceTemplateResolver.setCharacterEncoding("UTF-8");
        theResourceTemplateResolver.setCacheable(false);
        theResourceTemplateResolver.setOrder(2);
        return theResourceTemplateResolver;
    }

    @Bean
    public SpringTemplateEngine messageTemplateEngine(final Collection<SpringResourceTemplateResolver> inTemplateResolvers) {
        final SpringTemplateEngine theTemplateEngine = new SpringTemplateEngine();
        for (SpringResourceTemplateResolver theTemplateResolver : inTemplateResolvers) {
            theTemplateEngine.addTemplateResolver(theTemplateResolver);
        }
        return theTemplateEngine;
    }
}