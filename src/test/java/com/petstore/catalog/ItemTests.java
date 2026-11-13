package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Display rules of a catalog item.
 *
 * <p>Legacy sources: {@code components/catalog/.../model/Item.java} and the pages {@code
 * product.jsp}, {@code item.jsp} and {@code search.jsp}, which print the first attribute followed
 * by the product name.
 */
class ItemTests {

  @Test
  @DisplayName("legacy: an item is shown as its first attribute followed by the product name")
  void displayNameIsAttributeThenProductName() {
    assertThat(item(List.of("Large", "Cuddly")).displayName()).isEqualTo("Large Angelfish");
  }

  @Test
  @DisplayName("legacy: getAttribute() returns the first attribute")
  void attributeIsTheFirstOne() {
    assertThat(item(List.of("Large", "Cuddly")).attribute()).isEqualTo("Large");
  }

  @Test
  void itemWithoutAttributesIsShownByProductNameAlone() {
    assertThat(item(List.of()).displayName()).isEqualTo("Angelfish");
    assertThat(item(null).attribute()).isEmpty();
  }

  private static Item item(List<String> attributes) {
    return new Item(
        "FISH",
        "FI-SW-01",
        "Angelfish",
        "EST-1",
        "fish3.gif",
        "Fresh Water fish from Japan",
        attributes,
        new BigDecimal("16.50"),
        new BigDecimal("10.00"));
  }
}
