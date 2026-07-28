package com.clara.insurancequotes.auth.application.port.out;

public record StoredCeremony(CeremonyType type, String payload) {

    public enum CeremonyType {
        REGISTRATION,
        ASSERTION
    }
}
