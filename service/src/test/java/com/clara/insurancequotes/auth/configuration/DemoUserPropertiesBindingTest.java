package com.clara.insurancequotes.auth.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DemoUserPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "auth.demo.users[0].username=demo",
                    "auth.demo.users[0].password=demo-password",
                    "auth.demo.users[1].username=demo-admin",
                    "auth.demo.users[1].password=demo-admin-password",
                    "auth.demo.users[1].role=ADMIN");

    @Test
    void bindsDemoUsersAndRolesFromIndexedProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var properties = context.getBean(DemoUserProperties.class);

            assertThat(properties.users())
                    .extracting(DemoUserProperties.User::username)
                    .containsExactly("demo", "demo-admin");
            assertThat(properties.users().get(1).role()).isEqualTo("ADMIN");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DemoUserProperties.class)
    static class TestConfiguration {}
}
