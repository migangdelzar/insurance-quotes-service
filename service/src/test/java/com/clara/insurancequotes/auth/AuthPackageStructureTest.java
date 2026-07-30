package com.clara.insurancequotes.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthPackageStructureTest {

    @Test
    void keepsHttpRequestsBehindTheWebAdapterAndExposesFocusedUseCases() throws Exception {
        assertThat(Class.forName("com.clara.insurancequotes.auth.adapter.in.web.request.LoginRequest"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.auth.adapter.in.web.request.RefreshRequest"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.auth.adapter.in.web.request.WebAuthnAssertRequest"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.auth.api.usecase.LoginUseCase"))
                .isNotNull();
        assertThatThrownBy(() -> Class.forName("com.clara.insurancequotes.auth.api.request.WebAuthnAssertRequest"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
