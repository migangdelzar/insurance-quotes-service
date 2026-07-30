package com.clara.insurancequotes.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.auth.application.port.out.CredentialRepository;
import com.clara.insurancequotes.auth.application.port.out.RefreshTokenRepository;
import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PersistenceAdapterStructureTest {

    @Test
    void persistencePortsAreImplementedByRoleNamedOutboundAdapters() throws Exception {
        assertAdapter(
                "com.clara.insurancequotes.quote.adapter.out.persistence.QuotePersistenceAdapter",
                QuoteRepository.class);
        assertAdapter(
                "com.clara.insurancequotes.auth.adapter.out.persistence.UserPersistenceAdapter", UserRepository.class);
        assertAdapter(
                "com.clara.insurancequotes.auth.adapter.out.persistence.RefreshTokenPersistenceAdapter",
                RefreshTokenRepository.class);
        assertAdapter(
                "com.clara.insurancequotes.auth.adapter.out.persistence.PasskeyCredentialPersistenceAdapter",
                CredentialRepository.class);
    }

    private static void assertAdapter(String className, Class<?> port) throws ClassNotFoundException {
        var adapter = Class.forName(className);

        assertThat(Stream.of(adapter.getInterfaces())).contains(port);
    }
}
