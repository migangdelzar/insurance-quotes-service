package com.clara.insurancequotes.auth.adapter.in.web.controller;

import com.clara.insurancequotes.auth.api.exception.InvalidRefreshTokenException;
import com.clara.insurancequotes.auth.api.result.LoginResponse;
import com.clara.insurancequotes.auth.api.result.TokenPairResponse;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.application.service.LoginService;
import com.clara.insurancequotes.auth.application.service.RefreshTokenService;
import com.clara.insurancequotes.auth.application.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final UserRepository users;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var rotation = refreshTokenService.rotate(request.refreshToken());
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        var access = tokenService.issueApiToken(user.username());
        return new TokenPairResponse(access.accessToken(), rotation.rawToken(), access.expiresInSeconds());
    }

    @PostMapping("/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
