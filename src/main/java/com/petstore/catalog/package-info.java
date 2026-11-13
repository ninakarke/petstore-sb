/**
 * Catalog: categories, products, items and keyword search.
 *
 * <p>Replaces the legacy {@code catalog} component (stateless session bean, SQL-in-XML DAO and the
 * JSP helper that read the database directly from the pages) and the populate tooling that created
 * and loaded the catalog tables.
 */
@ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {"shared"})
package com.petstore.catalog;

import org.springframework.modulith.ApplicationModule;
