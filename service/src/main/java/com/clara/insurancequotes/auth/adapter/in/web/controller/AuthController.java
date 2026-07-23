package com.clara.insurancequotes.auth.adapter.in.web.controller;

import com.clara.insurancequotes.auth.api.result.TokenResponse;
import com.clara.insurancequotes.auth.application.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    public record TokenRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return tokenService.issueFor(request.username(), request.password());
    }
}
