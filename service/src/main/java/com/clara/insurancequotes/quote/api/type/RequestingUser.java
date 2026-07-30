package com.clara.insurancequotes.quote.api.type;

import java.util.UUID;

/** Stable caller context shared by read-oriented Quote API contracts. */
public record RequestingUser(UUID id, boolean admin) {}
