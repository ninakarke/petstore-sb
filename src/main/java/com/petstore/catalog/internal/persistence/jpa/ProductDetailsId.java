package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Primary key of {@code product_details}: one row per product and locale. */
@Embeddable
record ProductDetailsId(
    @Column(name = "productid") String productId, @Column(name = "locale") String locale) {}
