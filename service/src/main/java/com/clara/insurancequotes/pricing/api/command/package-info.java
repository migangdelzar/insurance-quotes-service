/**
 * Public pricing intentions such as {@link CalculatePremiumCommand}.
 * A pricing command requests a calculation without mutating durable state;
 * commands carry immutable input only,
 * contain no business logic, and must not expose application or adapter types.
 */
@org.springframework.modulith.NamedInterface("pricing-api-command")
package com.clara.insurancequotes.pricing.api.command;
