package com.petstore.identity.internal;

/** Proves that the in-memory user store used by the fast tests honours the store contract. */
class InMemoryUserStoreTests extends UserStoreContract {

  private final InMemoryUserStore store = new InMemoryUserStore();

  @Override
  protected UserStore store() {
    return store;
  }
}
