/**
 * Public state-changing intentions. Commands carry immutable input only,
 * contain no business logic, and must not expose application or adapter types.
 */
@org.springframework.modulith.NamedInterface("pricing-api-command")
package com.clara.insurancequotes.pricing.api.command;
