/**
 * Business state and invariants: aggregates, value objects, and domain enums.
 *
 * <p>This package must not depend on transport DTOs, persistence entities or
 * repositories, caches, messaging, external clients, or framework APIs.</p>
 *
 * <p>The current demo intentionally keeps the aggregate JPA-mapped so Spring
 * Data can persist it without a second domain-to-entity mapping layer. The
 * dependency is a documented persistence boundary exception; business rules
 * remain in the aggregate and do not depend on repositories or HTTP.</p>
 */
package com.clara.insurancequotes.quote.domain.model;
