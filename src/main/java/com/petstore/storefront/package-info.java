/**
 * Storefront: the web shell around the business modules.
 *
 * <p>Page layout, home page, language switching, redirects from the legacy URLs and error pages.
 * Replaces the legacy web application framework (front controller, template servlet, screen
 * definitions and custom tags). It may use every other module; no other module may use it.
 */
@ApplicationModule(displayName = "Storefront")
package com.petstore.storefront;

import org.springframework.modulith.ApplicationModule;
