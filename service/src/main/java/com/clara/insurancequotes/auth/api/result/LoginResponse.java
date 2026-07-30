package com.clara.insurancequotes.auth.api.result;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String status, String mfaToken, TokenPairResponse tokens) {

    public static LoginResponse mfaRequired(String mfaToken) {
        return new LoginResponse("MFA_REQUIRED", mfaToken, null);
    }

    public static LoginResponse tokensIssued(TokenPairResponse tokens) {
        return new LoginResponse("TOKENS_ISSUED", null, tokens);
    }
}
