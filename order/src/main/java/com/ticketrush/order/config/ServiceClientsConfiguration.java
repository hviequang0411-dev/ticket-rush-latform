package com.ticketrush.order.config;

import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.client.PaymentClient;
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
    public InventoryClient inventoryClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder, OAuth2AuthorizedClientManager manager) {
        return client(loadBalancedRestClientBuilder, manager, "http://inventory", InventoryClient.class);
    }

    @Bean
    public CatalogClient catalogClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder, OAuth2AuthorizedClientManager manager) {
        return client(loadBalancedRestClientBuilder, manager, "http://catalog", CatalogClient.class);
    }

    @Bean
    public PaymentClient paymentClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder, OAuth2AuthorizedClientManager manager) {
        return client(loadBalancedRestClientBuilder, manager, "http://payment", PaymentClient.class);
    }

    private static <T> T client(RestClient.Builder builder, OAuth2AuthorizedClientManager manager, String baseUrl, Class<T> type) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> SERVICE_REGISTRATION);
        RestClient restClient = builder.clone().baseUrl(baseUrl).requestInterceptor(interceptor).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
    }
}
