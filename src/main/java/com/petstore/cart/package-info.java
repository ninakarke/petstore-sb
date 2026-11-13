/**
 * Cart: the shopper's cart for the current browser session.
 *
 * <p>Replaces the legacy {@code cart} component, a stateful session bean referenced from the HTTP
 * session.
 */
@ApplicationModule(
    displayName = "Cart",
    allowedDependencies = {"catalog", "shared"})
package com.petstore.cart;

import org.springframework.modulith.ApplicationModule;
