package com.clara.insurancequotes.auth.adapter.out.persistence;

import com.clara.insurancequotes.auth.domain.model.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {

    java.util.Optional<User> findByUsername(String username);
}
