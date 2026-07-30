package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
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
    public TokenPairResponse refresh(String refreshToken) {
        var rotation = refreshTokens.rotate(refreshToken);
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        var access = tokenService.issueApiToken(user);
        return new TokenPairResponse(access.accessToken(), rotation.rawToken(), access.expiresInSeconds());
    }
}
