/**
 * Public inbound-port interfaces such as {@link CalculatePremiumUseCase}.
 * Callers depend on these capabilities rather
 * than application services, repositories, or adapters; implementations remain
 * internal to the module.
 */
@org.springframework.modulith.NamedInterface("pricing-api-usecase")
package com.clara.insurancequotes.pricing.api.usecase;
