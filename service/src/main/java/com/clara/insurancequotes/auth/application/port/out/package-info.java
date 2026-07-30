/**
 * Outbound ports required by the authentication application layer.
 *
 * <p>They declare capabilities such as persistence, ceremony storage, and
 * passkey verification for outbound adapters to implement. They must not
 * depend on adapter implementations, Spring Data, Redis, WebAuthn libraries,
 * or any other infrastructure API.</p>
 */
package com.clara.insurancequotes.auth.application.port.out;
