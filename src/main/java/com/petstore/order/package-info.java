/**
 * Order: checkout, order numbering, order storage and the hand-off to order processing.
 *
 * <p>Replaces the legacy {@code uidgen} and {@code asyncsender} components and the purchase-order
 * document classes. The legacy storefront sent each order as XML over JMS and kept no record of it;
 * this module stores the order and delivers the same document reliably.
 */
@ApplicationModule(
    displayName = "Order",
    allowedDependencies = {"cart", "catalog", "customer", "shared"})
package com.petstore.order;

import org.springframework.modulith.ApplicationModule;
