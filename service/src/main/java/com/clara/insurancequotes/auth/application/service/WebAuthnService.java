package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
import com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse;
import com.clara.insurancequotes.auth.application.port.out.PasskeyPort;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebAuthnService {

    private static final String MFA_SCOPE = "mfa-pending";

    private final PasskeyPort passkeyPort;
    private final UserRepository users;
    private final LoginService loginService;
    private final TokenService tokenService;

    public WebAuthnService(
            PasskeyPort passkeyPort, UserRepository users, LoginService loginService, TokenService tokenService) {
        this.passkeyPort = passkeyPort;
        this.users = users;
        this.loginService = loginService;
        this.tokenService = tokenService;
    }

    public WebAuthnChallengeResponse startAssertion(Optional<String> username) {
        var ceremony = passkeyPort.startAssertion(username);
        return new WebAuthnChallengeResponse(ceremony.challengeId(), ceremony.publicKeyOptionsJson());
    }

    @Transactional
    public TokenPairResponse finishAssertion(String challengeId, String credentialJson, String mfaToken) {
        requireMfaScopeIfPresent(mfaToken);
        var username = passkeyPort.finishAssertion(challengeId, credentialJson);
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        return loginService.issuePair(user);
    }

    public WebAuthnChallengeResponse startRegistration(String username) {
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        var ceremony = passkeyPort.startRegistration(user);
        return new WebAuthnChallengeResponse(ceremony.challengeId(), ceremony.publicKeyOptionsJson());
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
