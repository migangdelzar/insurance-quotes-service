/**
 * Authentication module. It owns authentication, session, and passkey
 * workflows; other modules use only its explicitly named API packages and must
 * not depend on its application or adapter implementations.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Authentication")
package com.clara.insurancequotes.auth;
