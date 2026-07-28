package com.clara.insurancequotes.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface WebAuthnCeremonyStore {

    void save(String challengeId, StoredCeremony ceremony, Duration ttl);

    Optional<StoredCeremony> take(String challengeId);
}
