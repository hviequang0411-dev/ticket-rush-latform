package com.ticketrush.order;

import com.ticketrush.order.config.AsyncSyncConfiguration;
import com.ticketrush.order.config.DatabaseTestcontainer;
import com.ticketrush.order.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        OrderApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.ticketrush.order.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
