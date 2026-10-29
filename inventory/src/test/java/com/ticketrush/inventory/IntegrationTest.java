package com.ticketrush.inventory;

import com.ticketrush.inventory.config.AsyncSyncConfiguration;
import com.ticketrush.inventory.config.DatabaseTestcontainer;
import com.ticketrush.inventory.config.TestSecurityConfiguration;
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
        InventoryApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.ticketrush.inventory.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
