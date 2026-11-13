package com.petstore.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Catalog settings.
 *
 * @param pageSize results per page on the category, product and search pages. The legacy pages
 *     hard-coded 2.
 * @param maxPageSize the largest page a caller may ask for. The legacy pages accepted any {@code
 *     count} from the query string.
 */
@ConfigurationProperties("petstore.catalog")
public record CatalogProperties(
    @DefaultValue("2") int pageSize, @DefaultValue("50") int maxPageSize) {}
