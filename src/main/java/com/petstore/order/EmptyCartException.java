package com.petstore.order;

/**
 * Thrown when an order is placed from an empty cart.
 *
 * <p>Legacy equivalent: {@code ShoppingCartEmptyOrderException}. As there, this is also what a
 * visitor gets who submits the same order form twice: the first submission emptied the cart.
 */
public class EmptyCartException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmptyCartException() {
    super("The shopping cart is empty");
  }
}
