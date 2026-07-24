package com.clara.insurancequotes.auth.adapter.out.webauthn;

import com.clara.insurancequotes.auth.api.exception.InvalidPasskeyException;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.PasskeyPort;
import com.clara.insurancequotes.auth.application.port.out.StoredCeremony;
import com.clara.insurancequotes.auth.application.port.out.WebAuthnCeremonyStore;
import com.clara.insurancequotes.auth.domain.model.PasskeyCredential;
import com.clara.insurancequotes.auth.domain.model.User;
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
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class YubicoPasskeyAdapter implements PasskeyPort {

    private static final Duration CEREMONY_TTL = Duration.ofMinutes(5);

    private final RelyingParty relyingParty;
    private final WebAuthnCeremonyStore ceremonies;
    private final CredentialRepository credentials;
    private final Clock clock;

    public YubicoPasskeyAdapter(
            RelyingParty relyingParty,
            WebAuthnCeremonyStore ceremonies,
            CredentialRepository credentials,
            Clock clock) {
        this.relyingParty = relyingParty;
        this.ceremonies = ceremonies;
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
        return store(StoredCeremony.CeremonyType.REGISTRATION, serialize(options), serializeCreation(options));
    }

    @Override
    public String finishRegistration(String challengeId, String credentialJson) {
        var options = takeRegistration(challengeId);
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
        return store(StoredCeremony.CeremonyType.ASSERTION, serialize(request), serializeAssertion(request));
    }

    @Override
    public String finishAssertion(String challengeId, String credentialJson) {
        var request = takeAssertion(challengeId);
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

    private StartedCeremony store(StoredCeremony.CeremonyType type, String payload, String publicKeyOptionsJson) {
        var challengeId = UUID.randomUUID().toString();
        ceremonies.save(challengeId, new StoredCeremony(type, payload), CEREMONY_TTL);
        return new StartedCeremony(challengeId, publicKeyOptionsJson);
    }

    private PublicKeyCredentialCreationOptions takeRegistration(String challengeId) {
        var ceremony = take(challengeId, StoredCeremony.CeremonyType.REGISTRATION);
        try {
            return PublicKeyCredentialCreationOptions.fromJson(ceremony.payload());
        } catch (Exception exception) {
            throw new InvalidPasskeyException("malformed registration ceremony");
        }
    }

    private AssertionRequest takeAssertion(String challengeId) {
        var ceremony = take(challengeId, StoredCeremony.CeremonyType.ASSERTION);
        try {
            return AssertionRequest.fromJson(ceremony.payload());
        } catch (Exception exception) {
            throw new InvalidPasskeyException("malformed assertion ceremony");
        }
    }

    private StoredCeremony take(String challengeId, StoredCeremony.CeremonyType expectedType) {
        var ceremony = ceremonies.take(challengeId).orElse(null);
        if (ceremony == null || ceremony.type() != expectedType) {
            throw new InvalidPasskeyException("unknown or expired challenge");
        }
        return ceremony;
    }

    private static String serialize(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toJson();
        } catch (Exception exception) {
            throw new InvalidPasskeyException("could not serialize registration ceremony");
        }
    }

    private static String serialize(AssertionRequest request) {
        try {
            return request.toJson();
        } catch (Exception exception) {
            throw new InvalidPasskeyException("could not serialize assertion ceremony");
        }
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
