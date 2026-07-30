package com.clara.insurancequotes.auth.api.usecase;

/** Revokes a refresh token. */
public interface LogoutUseCase {

    void logout(String refreshToken);
}
