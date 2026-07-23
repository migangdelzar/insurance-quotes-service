package com.clara.insurancequotes.auth.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException;
import com.clara.insurancequotes.auth.application.port.out.PasskeyPort;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WebAuthnServiceTest {

    private final PasskeyPort passkeyPort = mock(PasskeyPort.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LoginService loginService = mock(LoginService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final WebAuthnService service = new WebAuthnService(passkeyPort, users, loginService, tokenService);

    private final User demo = User.create("demo", "$2a$10$hash", Instant.now());

    @Test
    void assertionWithValidCeremonyIssuesTokenPair() {
        when(passkeyPort.finishAssertion("ch-1", "{cred}")).thenReturn("demo");
        when(users.findByUsername("demo")).thenReturn(Optional.of(demo));

        service.finishAssertion("ch-1", "{cred}", null);

        verify(loginService).issuePair(demo);
    }

    @Test
    void assertionMfaTokenMustCarryMfaPendingScope() {
        when(tokenService.scopeOf("bad-token")).thenReturn("api");

        assertThatThrownBy(() -> service.finishAssertion("ch-1", "{cred}", "bad-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void assertionUnknownUserFromCeremonyIsRejected() {
        when(passkeyPort.finishAssertion(anyString(), anyString())).thenReturn("ghost");
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finishAssertion("ch-1", "{cred}", null))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
