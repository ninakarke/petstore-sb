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

  /** The counter value of the last number handed out; zero if none has been. */
  long lastNumber();

  /**
   * Makes sure the next number handed out is greater than the given counter value. Never moves the
   * counter backwards. Used when orders are copied in from another store, so that numbers already
   * in use are not handed out again.
   */
  void advanceTo(long number);
}
