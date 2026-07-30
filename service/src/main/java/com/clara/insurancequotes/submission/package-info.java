/**
 * Submission module. It owns insurer-submission workflows; other modules use
 * only its explicitly named API packages and must not depend on application or
 * adapter implementations.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Submission")
package com.clara.insurancequotes.submission;
