package com.clara.insurancequotes.auth.api.command;

/** Typed application input for completing a passkey registration ceremony. */
public record RegisterPasskeyCommand(String challengeId, String credential) {}
