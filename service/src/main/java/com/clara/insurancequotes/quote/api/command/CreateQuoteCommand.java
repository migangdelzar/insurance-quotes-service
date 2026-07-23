package com.clara.insurancequotes.quote.api.command;

public record CreateQuoteCommand(String name, String email, int age, String zipCode) {}
