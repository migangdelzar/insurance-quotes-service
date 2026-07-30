/**
 * Business state and invariants: aggregates, value objects, and domain enums.
 *
 * <p>This package must stay independent of transport and persistence. It remains
 * a named interface only as a compatibility bridge for the existing submission
 * dependency; a later API-splitting task removes that exposure.</p>
 */
@org.springframework.modulith.NamedInterface("quote-domain-model")
package com.clara.insurancequotes.quote.domain.model;
