package com.clara.insurancequotes.testsupport;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

/** Persists a real user through the domain factory so FK-backed integration tests have a valid owner. */
public final class TestUsers {

    private TestUsers() {}

    public static UUID create(UserRepository users) {
        var user = User.create("user-" + UUID.randomUUID(), "hash", UserRole.USER, Instant.now());
        return users.save(user).id();
    }
}
