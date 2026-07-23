package com.clara.insurancequotes.quote.api.model;

public record CreateQuoteCommand(String name, String email, int age, String zipCode) {}
