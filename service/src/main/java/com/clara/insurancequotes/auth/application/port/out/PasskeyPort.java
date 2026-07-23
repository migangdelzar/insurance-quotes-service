package com.clara.insurancequotes.auth.application.port.out;

import com.clara.insurancequotes.auth.domain.model.User;
import java.util.Optional;

public interface PasskeyPort {

    record StartedCeremony(String challengeId, String publicKeyOptionsJson) {}

    StartedCeremony startRegistration(User user);

    String finishRegistration(String challengeId, String credentialJson);

    StartedCeremony startAssertion(Optional<String> username);

    String finishAssertion(String challengeId, String credentialJson);
}
