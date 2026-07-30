package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.api.result.TokenPair;
import com.clara.insurancequotes.auth.api.usecase.RefreshTokenUseCase;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates refresh-token rotation with access-token issuance. */
@Service
@RequiredArgsConstructor
public class RefreshAccessTokenService implements RefreshTokenUseCase {

    private final RefreshTokenService refreshTokens;
    private final UserRepository users;
    private final TokenService tokenService;

    @Override
    @Transactional
    public TokenPair refresh(String refreshToken) {
        var rotation = refreshTokens.rotate(refreshToken);
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        var access = tokenService.issueApiToken(user);
        return new TokenPair(access.accessToken(), rotation.rawToken(), access.expiresInSeconds());
    }
}
