package com.petstore.catalog.internal.persistence.jpa;

import com.petstore.catalog.Item;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Result row of the item queries, in the column order of the legacy {@code GET_ITEM}, {@code
 * GET_ITEMS} and {@code SEARCH_ITEMS} statements.
 */
record ItemRow(
    String categoryId,
    String productId,
    String productName,
    String itemId,
    String image,
    String description,
    String attr1,
    String attr2,
    String attr3,
    String attr4,
    String attr5,
    BigDecimal listPrice,
    BigDecimal unitCost) {

  Item toItem() {
    return new Item(
        categoryId,
        productId,
        productName,
        itemId,
        image,
        description,
        Stream.of(attr1, attr2, attr3, attr4, attr5).filter(Objects::nonNull).toList(),
        listPrice,
        unitCost);
  }
}
