package com.clara.insurancequotes.auth.adapter.in.web.controller;

import com.clara.insurancequotes.auth.adapter.in.web.request.AssertionOptionsRequest;
import com.clara.insurancequotes.auth.adapter.in.web.request.LoginRequest;
import com.clara.insurancequotes.auth.adapter.in.web.request.RefreshRequest;
import com.clara.insurancequotes.auth.adapter.in.web.request.WebAuthnAssertRequest;
import com.clara.insurancequotes.auth.adapter.in.web.request.WebAuthnRegisterRequest;
import com.clara.insurancequotes.auth.api.result.LoginResult;
import com.clara.insurancequotes.auth.api.result.TokenPair;
import com.clara.insurancequotes.auth.api.result.WebAuthnChallenge;
import com.clara.insurancequotes.auth.api.usecase.AssertPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.LoginUseCase;
import com.clara.insurancequotes.auth.api.usecase.LogoutUseCase;
import com.clara.insurancequotes.auth.api.usecase.RefreshTokenUseCase;
import com.clara.insurancequotes.auth.api.usecase.RegisterPasskeyUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyAssertionUseCase;
import com.clara.insurancequotes.auth.api.usecase.StartPasskeyRegistrationUseCase;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth", version = "1.0")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AssertPasskeyUseCase assertPasskeyUseCase;
    private final RegisterPasskeyUseCase registerPasskeyUseCase;
    private final StartPasskeyAssertionUseCase startPasskeyAssertionUseCase;
    private final StartPasskeyRegistrationUseCase startPasskeyRegistrationUseCase;

    @PostMapping("/login")
    public LoginResult login(@Valid @RequestBody LoginRequest request) {
        return loginUseCase.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokenUseCase.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        logoutUseCase.logout(request.refreshToken());
    }

    @PostMapping("/webauthn/assertion-options")
    public WebAuthnChallenge assertionOptions(@RequestBody(required = false) AssertionOptionsRequest request) {
        var username = Optional.ofNullable(request).map(AssertionOptionsRequest::username);
        return startPasskeyAssertionUseCase.startAssertion(username.orElse(null));
    }

    @PostMapping("/webauthn/assert")
    public TokenPair assertPasskey(@Valid @RequestBody WebAuthnAssertRequest request) {
        return assertPasskeyUseCase.assertPasskey(request.challengeId(), request.credentialJson(), request.mfaToken());
    }

    @PostMapping("/webauthn/register-options")
    public WebAuthnChallenge registerOptions(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
                    org.springframework.security.oauth2.jwt.Jwt jwt) {
        return startPasskeyRegistrationUseCase.startRegistration(jwt.getSubject());
    }

    @PostMapping("/webauthn/register")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody WebAuthnRegisterRequest request) {
        registerPasskeyUseCase.register(request.challengeId(), request.credentialJson());
    }
}
