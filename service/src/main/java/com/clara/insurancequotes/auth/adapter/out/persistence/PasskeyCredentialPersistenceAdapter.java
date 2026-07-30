package com.clara.insurancequotes.auth.adapter.out.persistence;

import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Implements passkey credential persistence with Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class PasskeyCredentialPersistenceAdapter implements CredentialRepository {

    private final SpringDataPasskeyCredentialRepository delegate;

    @Override
    public boolean existsForUser(UUID userId) {
        return delegate.existsForUser(userId);
    }

    @Override
    public PasskeyCredential save(PasskeyCredential credential) {
        return delegate.save(credential);
    }

    @Override
    public List<PasskeyCredential> findAllByUserId(UUID userId) {
        return delegate.findAllByUserId(userId);
    }

    @Override
    public Optional<PasskeyCredential> findByCredentialId(String credentialId) {
        return delegate.findByCredentialId(credentialId);
    }
}
