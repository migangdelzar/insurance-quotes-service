package com.clara.insurancequotes.auth.application.port.out;

import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository {

    boolean existsForUser(UUID userId);

    PasskeyCredential save(PasskeyCredential credential);

    List<PasskeyCredential> findAllByUserId(UUID userId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);
}
