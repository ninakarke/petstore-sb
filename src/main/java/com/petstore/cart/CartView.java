package com.petstore.cart;

import java.math.BigDecimal;
import java.util.List;

/** The content of the cart as it is shown and ordered: its lines, with current names and prices. */
public record CartView(List<CartLine> lines) {

  public CartView {
    lines = List.copyOf(lines);
  }

  /**
   * Number of different items, not the sum of the quantities. Legacy: {@code
   * ShoppingCartLocalEJB.getCount()}.
   */
  public int count() {
    return lines.size();
  }

  public boolean isEmpty() {
    return lines.isEmpty();
  }

  /** Sum of the line totals. Legacy: {@code ShoppingCartLocalEJB.getSubTotal()}. */
  public BigDecimal subTotal() {
    return lines.stream().map(CartLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
