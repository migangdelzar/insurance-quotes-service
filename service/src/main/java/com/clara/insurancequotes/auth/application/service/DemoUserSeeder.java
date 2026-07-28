package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.configuration.DemoUserProperties;
import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(DemoUserProperties.class)
public class DemoUserSeeder {

    @Bean
    public ApplicationRunner seedDemoUsers(
            UserRepository users, PasswordEncoder passwordEncoder, Clock clock, DemoUserProperties properties) {
        return args -> {
            properties.users().forEach(user -> seedUser(users, passwordEncoder, clock, user));
        };
    }

    private void seedUser(
            UserRepository users, PasswordEncoder passwordEncoder, Clock clock, DemoUserProperties.User user) {
        if (users.findByUsername(user.username()).isEmpty()) {
            users.save(User.create(
                    user.username(),
                    passwordEncoder.encode(user.password()),
                    UserRole.valueOf(user.role()),
                    clock.instant()));
        }
    }
}
