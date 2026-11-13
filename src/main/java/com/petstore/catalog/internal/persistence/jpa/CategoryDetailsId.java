package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Primary key of {@code category_details}: one row per category and locale. */
@Embeddable
record CategoryDetailsId(
    @Column(name = "catid") String categoryId, @Column(name = "locale") String locale) {}
