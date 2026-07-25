package com.clara.insurancequotes.auth.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.configuration.DemoUserProperties;
import com.clara.insurancequotes.auth.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoUserSeederTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-25T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void seedsThreeConfiguredUsersWhenTheyDoNotExist() throws Exception {
        var properties = new DemoUserProperties(List.of(
                new DemoUserProperties.User("demo", "demo-password"),
                new DemoUserProperties.User("demo-two", "demo-password-two"),
                new DemoUserProperties.User("demo-three", "demo-password-three")));
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0));

        new DemoUserSeeder()
                .seedDemoUsers(users, passwordEncoder, clock, properties)
                .run(new DefaultApplicationArguments());

        var captured = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users, times(3)).save(captured.capture());
        assertEquals(
                List.of("demo", "demo-two", "demo-three"),
                captured.getAllValues().stream().map(User::username).toList());
    }

    @Test
    void doesNotOverwriteUsersThatAlreadyExist() throws Exception {
        var properties = new DemoUserProperties(List.of(
                new DemoUserProperties.User("demo", "demo-password"),
                new DemoUserProperties.User("demo-two", "demo-password-two"),
                new DemoUserProperties.User("demo-three", "demo-password-three")));
        when(users.findByUsername(anyString()))
                .thenReturn(Optional.of(User.create("existing", "existing-hash", clock.instant())));

        new DemoUserSeeder()
                .seedDemoUsers(users, passwordEncoder, clock, properties)
                .run(new DefaultApplicationArguments());

        verify(users, times(0)).save(any(User.class));
    }
}
