/**
 * Quote module. It owns quote lifecycle and customer-owned quote state; other
 * modules use only its explicitly named API packages and must not depend on
 * application, domain, or adapter implementations.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Quote")
package com.clara.insurancequotes.quote;
