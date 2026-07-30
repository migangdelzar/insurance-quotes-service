package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.result.LoginResult;

/** Authenticates a user with username and password. */
public interface LoginUseCase {

    LoginResult login(String username, String password);
}
