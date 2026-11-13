package com.petstore.catalog.internal.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A category, one document in {@code categories}.
 *
 * <p>Holds what the legacy tables {@code category} and {@code category_details} held for one
 * language.
 */
@Document("categories")
record CategoryDocument(@Id String id, String name, String image, String description) {}
