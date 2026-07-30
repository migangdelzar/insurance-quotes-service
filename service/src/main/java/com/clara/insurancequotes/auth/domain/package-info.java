/**
 * Authentication domain boundary.
 *
 * <p>The concrete identity entities under {@code model} intentionally retain
 * JPA mappings for transactional credential persistence. This is the
 * documented persistence-boundary exception; the domain remains independent
 * from HTTP, controllers, and infrastructure adapters.</p>
 */
package com.clara.insurancequotes.auth.domain;
