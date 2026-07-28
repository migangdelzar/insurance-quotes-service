package com.clara.insurancequotes.auth.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "auth.demo")
public record DemoUserProperties(List<User> users) {

    @ConstructorBinding
    public DemoUserProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String username, String password, String role) {

        @ConstructorBinding
        public User {
            role = role == null || role.isBlank() ? "USER" : role;
        }

        public User(String username, String password) {
            this(username, password, "USER");
        }
    }
}
