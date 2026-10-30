package com.ticketrush.order.config;

import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.client.PaymentClient;
import com.ticketrush.order.client.ResilientClient;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Clients for calling other services by their Consul name, authenticated as this service.
 * <p>
 * Requests carry a client-credentials token from the {@value #SERVICE_REGISTRATION} registration (Keycloak client
 * {@code internal}, which has {@code ROLE_SERVICE}). The end user's token is deliberately not relayed: inventory's
 * reservation API must not be callable by buyers directly.
 */
@Configuration
public class ServiceClientsConfiguration {

    static final String SERVICE_REGISTRATION = "service";

    /**
     * Not a default candidate: only injection points marked {@code @LoadBalanced} get it, and Spring Boot keeps its
     * own plain builder for everything else (Keycloak, for one, is not registered in Consul).
     */
    @Bean(defaultCandidate = false)
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    /**
     * Fetches and caches client-credentials tokens outside of any HTTP request, so it also works from Kafka
     * consumers and scheduled jobs.
     */
    @Bean
    public OAuth2AuthorizedClientManager serviceAuthorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService
    ) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    public InventoryClient inventoryClient(ResilientClientFactory factory) {
        return factory.create("inventory", InventoryClient.class);
    }

    @Bean
    public CatalogClient catalogClient(ResilientClientFactory factory) {
        return factory.create("catalog", CatalogClient.class);
    }

    @Bean
    public PaymentClient paymentClient(ResilientClientFactory factory) {
        return factory.create("payment", PaymentClient.class);
    }

    @Bean
    public ResilientClientFactory resilientClientFactory(
        @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
        OAuth2AuthorizedClientManager manager,
        RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry,
        BulkheadRegistry bulkheadRegistry,
        @Value("${ticketrush.clients.connect-timeout:2s}") Duration connectTimeout,
        @Value("${ticketrush.clients.read-timeout:5s}") Duration readTimeout
    ) {
        return new ResilientClientFactory(
            loadBalancedRestClientBuilder,
            manager,
            retryRegistry,
            circuitBreakerRegistry,
            bulkheadRegistry,
            connectTimeout,
            readTimeout
        );
    }

    /**
     * Builds a client for a service by its Consul name: timeouts, then Resilience4j instances of the same name
     * (configured under {@code resilience4j.*} in application.yml).
     */
    public record ResilientClientFactory(
        RestClient.Builder builder,
        OAuth2AuthorizedClientManager manager,
        RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry,
        BulkheadRegistry bulkheadRegistry,
        Duration connectTimeout,
        Duration readTimeout
    ) {
        public <T> T create(String service, Class<T> type) {
            var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
            interceptor.setClientRegistrationIdResolver(request -> SERVICE_REGISTRATION);
            // HttpURLConnection: its read timeout applies per socket read. JdkClientHttpRequestFactory's read timeout
            // is a deadline from the start of the request that also closes a body still being read, which on a busy
            // host turned plain 200/500 answers into "Stream closed" I/O errors (see ResilientClientTest).
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            RestClient restClient = builder
                .clone()
                .baseUrl("http://" + service)
                .requestFactory(requestFactory)
                .requestInterceptor(interceptor)
                .build();
            T client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
            return ResilientClient.wrap(
                type,
                client,
                retryRegistry.retry(service),
                circuitBreakerRegistry.circuitBreaker(service),
                bulkheadRegistry.bulkhead(service)
            );
        }
    }
}
