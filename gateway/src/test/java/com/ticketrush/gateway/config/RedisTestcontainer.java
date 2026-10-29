package com.ticketrush.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis for the rate limiter's token buckets, same image as docker-compose.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestcontainer {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:8.10.1").withExposedPorts(6379).withReuse(true);

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return REDIS_CONTAINER;
    }
}
