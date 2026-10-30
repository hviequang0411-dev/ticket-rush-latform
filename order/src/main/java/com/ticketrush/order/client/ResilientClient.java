package com.ticketrush.order.client;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.Retry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.springframework.web.client.ResourceAccessException;

/**
 * Wraps an HTTP interface client so every call goes through retry, then circuit breaker, then bulkhead.
 * <p>
 * Retry is outermost, so each attempt asks the circuit breaker again and an open circuit stops the retries.
 * Retrying is only safe because every call these clients make is idempotent (reserve and payment creation are keyed
 * by order id). A rejected call (circuit open, bulkhead full) surfaces as {@link ResourceAccessException}, the same
 * as a dependency that does not answer, so callers handle both one way.
 */
public final class ResilientClient {

    private ResilientClient() {}

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<T> type, T target, Retry retry, CircuitBreaker circuitBreaker, Bulkhead bulkhead) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, args);
            }
            CheckedSupplier<Object> call = () -> {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            };
            CheckedSupplier<Object> decorated = Retry.decorateCheckedSupplier(
                retry,
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker, Bulkhead.decorateCheckedSupplier(bulkhead, call))
            );
            try {
                return decorated.get();
            } catch (CallNotPermittedException | BulkheadFullException e) {
                throw new ResourceAccessException(circuitBreaker.getName() + " unavailable: " + e.getMessage());
            }
        });
    }
}
