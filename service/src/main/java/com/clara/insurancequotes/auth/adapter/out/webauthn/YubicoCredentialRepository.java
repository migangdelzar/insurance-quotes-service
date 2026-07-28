package com.clara.insurancequotes.auth.adapter.out.webauthn;

import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class YubicoCredentialRepository implements com.yubico.webauthn.CredentialRepository {

    private final CredentialRepository credentials;
    private final UserRepository users;

    public YubicoCredentialRepository(CredentialRepository credentials, UserRepository users) {
        this.credentials = credentials;
        this.users = users;
    }

    public static ByteArray userHandleOf(UUID userId) {
        var buffer = ByteBuffer.allocate(16);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());
        return new ByteArray(buffer.array());
    }

    public static UUID userIdOf(ByteArray userHandle) {
        if (userHandle == null || userHandle.size() != 16) {
            throw new InvalidPasskeyException("invalid user handle");
        }
        var buffer = ByteBuffer.wrap(userHandle.getBytes());
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return users.findByUsername(username)
                .map(user -> credentials.findAllByUserId(user.id()).stream()
                        .map(credential -> PublicKeyCredentialDescriptor.builder()
                                .id(decodeCredentialId(credential.credentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return users.findByUsername(username).map(user -> userHandleOf(user.id()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return users.findById(userIdOf(userHandle)).map(user -> user.username());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentials
                .findByCredentialId(credentialId.getBase64Url())
                .filter(credential -> userHandleOf(credential.userId()).equals(userHandle))
                .map(credential -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(userHandleOf(credential.userId()))
                        .publicKeyCose(new ByteArray(credential.publicKeyCose()))
                        .signatureCount(credential.signatureCount())
                        .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentials.findByCredentialId(credentialId.getBase64Url()).stream()
                .map(credential -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(userHandleOf(credential.userId()))
                        .publicKeyCose(new ByteArray(credential.publicKeyCose()))
                        .signatureCount(credential.signatureCount())
                        .build())
                .collect(Collectors.toSet());
    }

    private static ByteArray decodeCredentialId(String credentialId) {
        try {
            return ByteArray.fromBase64Url(credentialId);
        } catch (Exception exception) {
            throw new InvalidPasskeyException("stored credential id is invalid");
        }
    }
}
