package com.petstore.order;

import java.math.BigDecimal;

/**
 * One line of an order.
 *
 * <p>Legacy equivalent: the {@code LineItem} value class of the purchase-order document. As there,
 * lines are numbered from zero and the unit price is the item's list price at the time of the
 * order. Money is {@link BigDecimal}; the legacy class used {@code float}.
 */
public record OrderLine(
    String categoryId,
    String productId,
    String itemId,
    int lineNumber,
    int quantity,
    BigDecimal unitPrice) {

  public BigDecimal total() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
