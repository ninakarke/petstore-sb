package com.petstore.customer.internal;

/** Proves that the in-memory customer store used by the fast tests honours the store contract. */
class InMemoryCustomerStoreTests extends CustomerStoreContract {

  private final InMemoryCustomerStore store = new InMemoryCustomerStore();

  @Override
  protected CustomerStore store() {
    return store;
  }
}
