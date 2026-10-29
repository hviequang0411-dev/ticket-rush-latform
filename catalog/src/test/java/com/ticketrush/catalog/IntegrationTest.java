package com.ticketrush.catalog;

import com.ticketrush.catalog.config.AsyncSyncConfiguration;
import com.ticketrush.catalog.config.DatabaseTestcontainer;
import com.ticketrush.catalog.config.RedisTestContainer;
import com.ticketrush.catalog.config.TestSecurityConfiguration;
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
        CatalogApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.ticketrush.catalog.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
    }
)
public @interface IntegrationTest {}
