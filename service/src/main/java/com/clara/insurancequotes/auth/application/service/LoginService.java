package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.result.LoginResponse;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository users;
    private final CredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;

    public LoginService(
            UserRepository users,
            CredentialRepository credentials,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (credentials.existsForUser(user.id())) {
            return LoginResponse.mfaRequired(tokenService.issueMfaToken(username));
        }
        return LoginResponse.tokensIssued(issuePair(user));
    }

    @Transactional
    public TokenPairResponse issuePair(User user) {
        var access = tokenService.issueApiToken(user);
        var refresh = refreshTokens.issue(user);
        return new TokenPairResponse(access.accessToken(), refresh, access.expiresInSeconds());
    }
}
