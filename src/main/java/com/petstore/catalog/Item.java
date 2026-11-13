package com.petstore.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * A purchasable item in one language, with the prices that apply in that language's market.
 *
 * <p>Legacy equivalent: {@code com.sun.j2ee.blueprints.catalog.model.Item}. Two differences: prices
 * are {@link BigDecimal} instead of {@code double}, and the five attribute fields are one list.
 *
 * @param categoryId id of the category the item's product belongs to
 * @param productId id of the item's product
 * @param productName name of the item's product, in the requested language
 * @param itemId id of the item
 * @param imageLocation file name of the item's picture
 * @param description description of the item
 * @param attributes up to five distinguishing attributes, such as "Large"; never {@code null}
 * @param listPrice the list price
 * @param unitCost the price charged to the customer, shown as "Your Price"
 */
public record Item(
    String categoryId,
    String productId,
    String productName,
    String itemId,
    String imageLocation,
    String description,
    List<String> attributes,
    BigDecimal listPrice,
    BigDecimal unitCost) {

  public Item {
    attributes = attributes == null ? List.of() : List.copyOf(attributes);
  }

  /** The first attribute, or an empty string. Legacy: {@code Item.getAttribute()}. */
  public String attribute() {
    return attributes.isEmpty() ? "" : attributes.get(0);
  }

  /**
   * The name shown for the item: its first attribute followed by the product name, for example
   * "Large Angelfish". The legacy pages printed the two values next to each other.
   */
  public String displayName() {
    return (attribute() + " " + productName).trim();
  }
}
