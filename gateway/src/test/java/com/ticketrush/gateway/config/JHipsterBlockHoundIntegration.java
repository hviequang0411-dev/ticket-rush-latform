package com.ticketrush.gateway.config;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

public class JHipsterBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("java.util.UUID", "randomUUID");
        builder.allowBlockingCallsInside("org.springframework.validation.beanvalidation.SpringValidatorAdapter", "validate");
        builder.allowBlockingCallsInside("com.ticketrush.gateway.service.MailService", "sendEmailFromTemplate");
        builder.allowBlockingCallsInside("com.ticketrush.gateway.security.DomainUserDetailsService", "createSpringSecurityUser");
        builder.allowBlockingCallsInside("org.springframework.web.reactive.result.method.InvocableHandlerMethod", "invoke");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "build");
        builder.allowBlockingCallsInside("org.springdoc.core.service.OpenAPIService", "getWebhooksClasses");
        builder.allowBlockingCallsInside("org.springdoc.core.service.AbstractRequestService", "build");
        // Lettuce reconnecting Redis's shared connection after it was lost. The connection is opened at startup
        // (RateLimitConfiguration), so this only runs after a disconnect, bounded by the 2 s connect timeout
        builder.allowBlockingCallsInside("org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory$SharedConnection", "getNativeConnection");
        // jhipster-needle-blockhound-integration - JHipster will add additional gradle plugins here
    }
}
