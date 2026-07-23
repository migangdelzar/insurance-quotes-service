package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserSeeder {

    @Bean
    public ApplicationRunner seedDemoUser(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${auth.demo.username}") String username,
            @Value("${auth.demo.password}") String password) {
        return args -> {
            if (users.findByUsername(username).isEmpty()) {
                users.save(User.create(username, passwordEncoder.encode(password), clock.instant()));
            }
        };
    }
}
