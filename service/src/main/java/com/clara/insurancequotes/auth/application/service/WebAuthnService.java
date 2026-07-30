package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.exception.PasskeyNotRegisteredException;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
import com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse;
import com.clara.insurancequotes.auth.api.usecase.AssertPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.RegisterPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyAssertionUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyRegistrationUseCase;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.PasskeyPort;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebAuthnService
        implements StartPasskeyAssertionUseCase,
                AssertPasskeyUseCase,
                StartPasskeyRegistrationUseCase,
                RegisterPasskeyUseCase {

    private static final String MFA_SCOPE = "mfa-pending";

    private final PasskeyPort passkeyPort;
    private final CredentialRepository credentials;
    private final UserRepository users;
    private final LoginService loginService;
    private final TokenService tokenService;

    public WebAuthnService(
            PasskeyPort passkeyPort,
            CredentialRepository credentials,
            UserRepository users,
            LoginService loginService,
            TokenService tokenService) {
        this.passkeyPort = passkeyPort;
        this.credentials = credentials;
        this.users = users;
        this.loginService = loginService;
        this.tokenService = tokenService;
    }

    @Override
    public WebAuthnChallengeResponse startAssertion(String username) {
        return startAssertion(Optional.ofNullable(username).filter(value -> !value.isBlank()));
    }

    public WebAuthnChallengeResponse startAssertion(Optional<String> username) {
        if (username.isPresent()) {
            var user = users.findByUsername(username.get()).orElseThrow(InvalidCredentialsException::new);
            if (!credentials.existsForUser(user.id())) {
                throw new PasskeyNotRegisteredException();
            }
        }
        var ceremony = passkeyPort.startAssertion(username);
        return new WebAuthnChallengeResponse(ceremony.challengeId(), ceremony.publicKeyOptionsJson());
    }

    @Transactional
    @Override
    public TokenPairResponse assertPasskey(String challengeId, String credentialJson, String mfaToken) {
        return finishAssertion(challengeId, credentialJson, mfaToken);
    }

    @Transactional
    public TokenPairResponse finishAssertion(String challengeId, String credentialJson, String mfaToken) {
        requireMfaScopeIfPresent(mfaToken);
        var username = passkeyPort.finishAssertion(challengeId, credentialJson);
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        return loginService.issuePair(user);
    }

    @Override
    public WebAuthnChallengeResponse startRegistration(String username) {
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        var ceremony = passkeyPort.startRegistration(user);
        return new WebAuthnChallengeResponse(ceremony.challengeId(), ceremony.publicKeyOptionsJson());
    }

    @Transactional
    @Override
    public void register(String challengeId, String credentialJson) {
        finishRegistration(challengeId, credentialJson);
    }

    @Transactional
    public void finishRegistration(String challengeId, String credentialJson) {
        passkeyPort.finishRegistration(challengeId, credentialJson);
    }

    private void requireMfaScopeIfPresent(String mfaToken) {
        if (mfaToken != null && !MFA_SCOPE.equals(tokenService.scopeOf(mfaToken))) {
            throw new InvalidCredentialsException();
        }
    }
}
