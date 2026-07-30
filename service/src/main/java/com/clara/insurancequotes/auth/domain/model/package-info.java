/**
 * Authentication aggregates, value objects, enums, and their invariants.
 *
 * <p>Models own business state and must not depend on transport DTOs,
 * persistence entities or repositories, cache clients, or framework APIs.</p>
 *
 * <p>Authentication entities are JPA-mapped in this demo to keep credential
 * persistence explicit and transactional. This is a documented persistence
 * boundary exception, not a dependency on controllers or transport concerns.</p>
 */
package com.clara.insurancequotes.auth.domain.model;
