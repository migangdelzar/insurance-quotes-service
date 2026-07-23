package com.clara.insurancequotes.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credential_id", nullable = false, unique = true)
    private String credentialId;

    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private long signatureCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasskeyCredential() {}

    public PasskeyCredential(UUID userId, String credentialId, byte[] publicKeyCose, long signatureCount, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKeyCose = publicKeyCose;
        this.signatureCount = signatureCount;
        this.createdAt = now;
    }

    public UUID userId() {
        return userId;
    }

    public String credentialId() {
        return credentialId;
    }

    public byte[] publicKeyCose() {
        return publicKeyCose;
    }

    public long signatureCount() {
        return signatureCount;
    }

    public void updateSignatureCount(long count) {
        this.signatureCount = count;
    }
}
