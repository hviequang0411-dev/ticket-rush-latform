package com.ticketrush.gateway.config;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

/**
 * Who a rate limit bucket belongs to. Limits themselves are configured per route in application.yml.
 * <p>
 * Signed-in callers are limited per user, so one account cannot hammer checkout from many IPs and many buyers
 * behind one NAT do not share a bucket. Anonymous callers fall back to their IP address.
 * <p>
 * The Redis limiter fails open: if Redis is down, requests pass instead of the whole site going down with it.
 */
@Configuration
public class RateLimitConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitConfiguration.class);

    @Bean
    @Primary
    public KeyResolver rateLimitKeyResolver() {
        return exchange ->
            exchange
                .getPrincipal()
                .map(Principal::getName)
                .map(name -> "user:" + name)
                .switchIfEmpty(
                    Mono.fromSupplier(() ->
                        "ip:" +
                        Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(InetSocketAddress::getHostString)
                            .orElse("unknown")
                    )
                );
    }

    /**
     * Does at startup what would otherwise happen, blocking, on the first rate-limited request on a Netty
     * event-loop thread that every other request is waiting for: open Lettuce's shared connection, and load the
     * limiter's Lua script into Redis. Without the load, the first EVALSHA fails with NOSCRIPT and the fallback
     * EVAL reads the script from the jar again.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connectRedis(ApplicationReadyEvent event) {
        ReactiveRedisConnectionFactory connectionFactory = event.getApplicationContext().getBean(ReactiveRedisConnectionFactory.class);
        try {
            var connection = connectionFactory.getReactiveConnection();
            connection.ping().block(Duration.ofSeconds(5));
            for (RedisScript<?> script : event.getApplicationContext().getBeansOfType(RedisScript.class).values()) {
                script.getSha1();
                connection.scriptingCommands().scriptLoad(ByteBuffer.wrap(script.getScriptAsString().getBytes(StandardCharsets.UTF_8))).block(Duration.ofSeconds(5));
            }
            LOG.info("Connected to Redis for rate limiting");
        } catch (RuntimeException e) {
            LOG.warn("Redis unavailable at startup, rate limits are not enforced until it is back: {}", e.getMessage());
        }
    }
}
