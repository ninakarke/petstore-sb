package com.petstore.order.internal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An order-number generator that counts in memory, for fast tests. Held to the same {@link
 * OrderIdGeneratorContract} as the real adapters.
 */
public class SequenceOrderIdGenerator implements OrderIdGenerator {

  private final AtomicInteger counter = new AtomicInteger();

  @Override
  public String nextOrderId() {
    return PREFIX + counter.incrementAndGet();
  }

  public void reset() {
    counter.set(0);
  }
}
