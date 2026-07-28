package com.clara.insurancequotes.quote.api.usecase;

import java.util.UUID;

public record RequestingUser(UUID id, boolean admin) {}
