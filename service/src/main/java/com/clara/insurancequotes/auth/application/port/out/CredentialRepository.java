package com.clara.insurancequotes.auth.application.port.out;

import java.util.UUID;

public interface CredentialRepository {

    boolean existsForUser(UUID userId);
}
