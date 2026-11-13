package com.petstore.order.internal;

/**
 * Hands out order numbers.
 *
 * <p>Legacy equivalent: {@code UniqueIdGeneratorEJB} with its {@code Counter} entity bean, called
 * as {@code getUniqueId("1001")}. The format is kept: the prefix {@code 1001} followed by a counter
 * that starts at one, so the first orders are {@code 10011}, {@code 10012}, ...
 *
 * <p>Every call returns a number that no other call returns, also when calls arrive at the same
 * time. Numbers are increasing but not guaranteed to be without gaps: a number is used up even if
 * the order it was meant for is not stored in the end.
 */
public interface OrderIdGenerator {

  /** Legacy: the argument of {@code getUniqueId("1001")}. */
  String PREFIX = "1001";

  String nextOrderId();
}
