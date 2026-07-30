package com.clara.insurancequotes.auth.api.usecase;

import com.clara.insurancequotes.auth.api.command.RegisterPasskeyCommand;

/** Completes a passkey registration ceremony. */
public interface RegisterPasskeyUseCase {

    void register(RegisterPasskeyCommand command);
}
