package com.clara.insurancequotes.auth.api.result;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResult(String status, String mfaToken, TokenPair tokens) {

    public static LoginResult mfaRequired(String mfaToken) {
        return new LoginResult("MFA_REQUIRED", mfaToken, null);
    }

    public static LoginResult tokensIssued(TokenPair tokens) {
        return new LoginResult("TOKENS_ISSUED", null, tokens);
    }
}
