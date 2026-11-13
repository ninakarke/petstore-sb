/**
 * Customer: the customer account with its contact details, address, payment card and preferences.
 *
 * <p>Replaces the legacy {@code customer}, {@code contactinfo}, {@code address} and {@code
 * creditcard} components, six entity beans linked by one-to-one relationships that together form a
 * single aggregate.
 */
@ApplicationModule(
    displayName = "Customer",
    allowedDependencies = {"shared"})
package com.petstore.customer;

import org.springframework.modulith.ApplicationModule;
