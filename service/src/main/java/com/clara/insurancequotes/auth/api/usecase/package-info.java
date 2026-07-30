/**
 * Public authentication capabilities. Controllers and other module callers
 * depend on these focused inbound ports, never on application services or
 * transport request records.
 */
@org.springframework.modulith.NamedInterface("auth-api-usecase")
package com.clara.insurancequotes.auth.api.usecase;
