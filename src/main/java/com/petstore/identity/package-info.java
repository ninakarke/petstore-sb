/**
 * Identity: users, sign-in, sign-out, registration and the list of protected resources.
 *
 * <p>Replaces the legacy {@code signon} component: the {@code User} entity bean, the sign-on
 * session bean, the custom servlet filter and its {@code signon-config.xml}.
 */
@ApplicationModule(
    displayName = "Identity",
    allowedDependencies = {"shared"})
package com.petstore.identity;

import org.springframework.modulith.ApplicationModule;
