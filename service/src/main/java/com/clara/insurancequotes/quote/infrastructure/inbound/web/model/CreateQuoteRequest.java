package com.clara.insurancequotes.quote.infrastructure.inbound.web.model;

import com.clara.insurancequotes.quote.api.model.CreateQuoteCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateQuoteRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull @Min(18) @Max(120) Integer age,
        @NotBlank @Pattern(regexp = "\\d{5}", message = "zip code must be 5 digits") String zipCode) {

    public CreateQuoteCommand toCommand() {
        return new CreateQuoteCommand(name, email, age, zipCode);
    }
}
