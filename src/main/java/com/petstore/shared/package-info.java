/**
 * Shared kernel: the few small value types every module needs.
 *
 * <p>Kept deliberately tiny. Anything that belongs to one business capability lives in that
 * capability's module, not here.
 */
@ApplicationModule(displayName = "Shared kernel")
package com.petstore.shared;

import org.springframework.modulith.ApplicationModule;
