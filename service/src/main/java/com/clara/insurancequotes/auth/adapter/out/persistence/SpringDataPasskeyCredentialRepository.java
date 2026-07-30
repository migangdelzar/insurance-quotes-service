package com.clara.insurancequotes.auth.adapter.out.persistence;

import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPasskeyCredentialRepository extends JpaRepository<PasskeyCredential, UUID> {

    default boolean existsForUser(UUID userId) {
        return countByUserId(userId) > 0;
    }

    long countByUserId(UUID userId);

    List<PasskeyCredential> findAllByUserId(UUID userId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);
}
