package com.clara.insurancequotes.auth.application.port.out;

import com.clara.insurancequotes.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);
}
