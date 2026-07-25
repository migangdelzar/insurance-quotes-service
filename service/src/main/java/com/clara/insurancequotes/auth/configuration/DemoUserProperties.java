package com.clara.insurancequotes.auth.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.demo")
public record DemoUserProperties(List<User> users) {

    public DemoUserProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String username, String password) {}
}
