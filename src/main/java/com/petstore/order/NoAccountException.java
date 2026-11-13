package com.petstore.order;

/**
 * Thrown when a user who has not entered their account details tries to order. The order needs the
 * payment card of the account.
 */
public class NoAccountException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NoAccountException(String userId) {
    super("User " + userId + " has not entered account details");
  }
}
