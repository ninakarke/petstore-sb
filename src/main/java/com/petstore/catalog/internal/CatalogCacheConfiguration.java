package com.petstore.catalog.internal;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on caching for catalog reads.
 *
 * <p>Replaces the legacy {@code waf:cache} tag, which stored rendered page fragments in the servlet
 * context for five minutes and never removed an entry. The cache names, size limit and expiry are
 * set in {@code application.yml}.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
class CatalogCacheConfiguration {}
