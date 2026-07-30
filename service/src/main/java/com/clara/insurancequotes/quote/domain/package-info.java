/**
 * Quote business model and policies.
 *
 * <p>The aggregate model is intentionally JPA-mapped in this demo so the
 * persistence adapter can retain transactional aggregate behavior without a
 * duplicate entity model. The mapping is the documented persistence-boundary
 * exception; domain behavior remains independent of HTTP and repositories.</p>
 */
package com.clara.insurancequotes.quote.domain;
