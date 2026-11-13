package com.petstore.order;

/** Thrown when a requested order cannot be accepted. Carries everything that is wrong with it. */
public class InvalidOrderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient OrderProblems problems;

  public InvalidOrderException(OrderProblems problems) {
    super("Invalid order: " + problems);
    this.problems = problems;
  }

  public OrderProblems problems() {
    return problems;
  }
}
