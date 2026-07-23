package com.clara.insurancequotes.auth.adapter.out.persistence;

import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPasskeyCredentialRepository extends JpaRepository<PasskeyCredential, UUID>, CredentialRepository {

    @Override
    default boolean existsForUser(UUID userId) {
        return countByUserId(userId) > 0;
    }

    long countByUserId(UUID userId);
}
