package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Primary key of {@code item_details}: one row per item and locale. */
@Embeddable
record ItemDetailsId(
    @Column(name = "itemid") String itemId, @Column(name = "locale") String locale) {}
