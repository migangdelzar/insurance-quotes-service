package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.LoginResponse;

/** Authenticates a user with username and password. */
public interface LoginUseCase {

    LoginResponse login(String username, String password);
}
