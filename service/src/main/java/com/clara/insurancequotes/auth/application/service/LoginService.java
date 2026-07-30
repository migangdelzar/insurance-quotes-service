package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.api.result.LoginResult;
import com.clara.insurancequotes.auth.api.result.TokenPair;
import com.clara.insurancequotes.auth.api.usecase.LoginUseCase;
import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService implements LoginUseCase {

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
    @Override
    public LoginResult login(String username, String password) {
        var user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (credentials.existsForUser(user.id())) {
            return LoginResult.mfaRequired(tokenService.issueMfaToken(username));
        }
        return LoginResult.tokensIssued(issuePair(user));
    }

    @Transactional
    public TokenPair issuePair(User user) {
        var access = tokenService.issueApiToken(user);
        var refresh = refreshTokens.issue(user);
        return new TokenPair(access.accessToken(), refresh, access.expiresInSeconds());
    }
}
