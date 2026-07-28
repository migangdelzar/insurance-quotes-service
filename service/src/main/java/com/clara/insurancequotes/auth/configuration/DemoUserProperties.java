package com.clara.insurancequotes.auth.configuration;

import com.clara.insurancequotes.auth.domain.model.UserRole;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "auth.demo")
public record DemoUserProperties(List<User> users) {

    @ConstructorBinding
    public DemoUserProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String username, String password, UserRole role) {

        @ConstructorBinding
        public User {
            role = role == null ? UserRole.USER : role;
        }

        public User(String username, String password) {
            this(username, password, UserRole.USER);
        }
    }
}
