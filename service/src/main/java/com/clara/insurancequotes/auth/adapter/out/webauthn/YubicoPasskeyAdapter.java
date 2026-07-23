package com.clara.insurancequotes.auth.adapter.out.webauthn;

import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.PasskeyPort;
import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import com.clara.insurancequotes.auth.domain.model.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class YubicoPasskeyAdapter implements PasskeyPort {

    private final RelyingParty relyingParty;
    private final Cache<String, Object> ceremonies;
    private final CredentialRepository credentials;
    private final Clock clock;

    public YubicoPasskeyAdapter(
            RelyingParty relyingParty,
            Cache<String, Object> webAuthnCeremonyCache,
            CredentialRepository credentials,
            Clock clock) {
        this.relyingParty = relyingParty;
        this.ceremonies = webAuthnCeremonyCache;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    public StartedCeremony startRegistration(User user) {
        var identity = UserIdentity.builder()
                .name(user.username())
                .displayName(user.username())
                .id(YubicoCredentialRepository.userHandleOf(user.id()))
                .build();
        var options = relyingParty.startRegistration(StartRegistrationOptions.builder()
                .user(identity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.PREFERRED)
                        .build())
                .build());
        return store(options, serializeCreation(options));
    }

    @Override
    public String finishRegistration(String challengeId, String credentialJson) {
        var options = takeCeremony(challengeId, PublicKeyCredentialCreationOptions.class);
        try {
            var credential = PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
            var result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(credential)
                    .build());
            var userId = YubicoCredentialRepository.userIdOf(options.getUser().getId());
            credentials.save(new PasskeyCredential(
                    userId,
                    result.getKeyId().getId().getBase64Url(),
                    result.getPublicKeyCose().getBytes(),
                    result.getSignatureCount(),
                    clock.instant()));
            return options.getUser().getName();
        } catch (Exception exception) {
            throw new InvalidPasskeyException(exception.getMessage());
        }
    }

    @Override
    public StartedCeremony startAssertion(Optional<String> username) {
        var builder = StartAssertionOptions.builder();
        username.ifPresent(builder::username);
        var request = relyingParty.startAssertion(builder.build());
        return store(request, serializeAssertion(request));
    }

    @Override
    public String finishAssertion(String challengeId, String credentialJson) {
        var request = takeCeremony(challengeId, AssertionRequest.class);
        try {
            var credential = PublicKeyCredential.parseAssertionResponseJson(credentialJson);
            var result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(credential)
                    .build());
            if (!result.isSuccess()) {
                throw new InvalidPasskeyException("assertion rejected");
            }
            credentials
                    .findByCredentialId(result.getCredential().getCredentialId().getBase64Url())
                    .ifPresent(stored -> {
                        stored.updateSignatureCount(result.getSignatureCount());
                        credentials.save(stored);
                    });
            return result.getUsername();
        } catch (InvalidPasskeyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidPasskeyException(exception.getMessage());
        }
    }

    private StartedCeremony store(Object ceremony, String optionsJson) {
        var challengeId = UUID.randomUUID().toString();
        ceremonies.put(challengeId, ceremony);
        return new StartedCeremony(challengeId, optionsJson);
    }

    private <T> T takeCeremony(String challengeId, Class<T> type) {
        var ceremony = ceremonies.getIfPresent(challengeId);
        ceremonies.invalidate(challengeId);
        if (!type.isInstance(ceremony)) {
            throw new InvalidPasskeyException("unknown or expired challenge");
        }
        return type.cast(ceremony);
    }

    private static String serializeCreation(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toCredentialsCreateJson();
        } catch (Exception exception) {
            throw new InvalidPasskeyException(exception.getMessage());
        }
    }

    private static String serializeAssertion(AssertionRequest request) {
        try {
            return request.toCredentialsGetJson();
        } catch (Exception exception) {
            throw new InvalidPasskeyException(exception.getMessage());
        }
    }
}
