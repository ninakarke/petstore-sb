package com.petstore.cart;

import java.math.BigDecimal;

/**
 * One line of the cart: an item, how many of it, and what one costs.
 *
 * <p>Legacy equivalent: {@code com.sun.j2ee.blueprints.cart.model.CartItem}. As there, the price of
 * a line is the item's <em>list price</em>. Unlike there, money is {@link BigDecimal}, not {@code
 * double}.
 *
 * @param name the product name
 * @param attribute the item's first attribute, such as "Large"
 * @param unitPrice the list price of one item
 */
public record CartLine(
    String itemId,
    String productId,
    String categoryId,
    String name,
    String attribute,
    int quantity,
    BigDecimal unitPrice) {

  /** Price of the whole line. Legacy: {@code CartItem.getTotalCost()}. */
  public BigDecimal total() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  /** The name shown for the line: attribute followed by product name. */
  public String displayName() {
    return (attribute + " " + name).trim();
  }
}
