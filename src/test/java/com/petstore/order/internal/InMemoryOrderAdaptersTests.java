package com.petstore.order.internal;

import org.junit.jupiter.api.Nested;

/** Proves that the in-memory order adapters used by the fast tests honour the port contracts. */
class InMemoryOrderAdaptersTests {

  @Nested
  class Store extends OrderStoreContract {

    private final InMemoryOrderStore store = new InMemoryOrderStore();

    @Override
    protected OrderStore store() {
      return store;
    }
  }

  @Nested
  class IdGenerator extends OrderIdGeneratorContract {

    private final SequenceOrderIdGenerator generator = new SequenceOrderIdGenerator();

    @Override
    protected OrderIdGenerator generator() {
      return generator;
    }

    @Override
    protected void resetTheCounter() {
      generator.reset();
    }
  }
}
