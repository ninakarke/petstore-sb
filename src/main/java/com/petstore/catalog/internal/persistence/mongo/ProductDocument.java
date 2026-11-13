package com.petstore.catalog.internal.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A product, one document in {@code products}.
 *
 * <p>Holds what the legacy tables {@code product} and {@code product_details} held for one
 * language.
 */
@Document("products")
record ProductDocument(
    @Id String id, String categoryId, String name, String image, String description) {}
