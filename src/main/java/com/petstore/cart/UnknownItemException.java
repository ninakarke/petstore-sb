package com.petstore.cart;

/** Thrown when something that is not in the catalog is added to the cart. */
public class UnknownItemException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UnknownItemException(String itemId) {
    super("No such item: " + itemId);
  }
}
