package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Proves that the in-memory store used by the fast tests honours the store contract. */
class InMemoryCatalogStoreTests extends CatalogStoreContract {

  private final InMemoryCatalogStore store = new InMemoryCatalogStore();

  @Override
  protected CatalogStore store() {
    return store;
  }

  @Test
  void clearedStoreIsEmpty() {
    store.clear();

    assertThat(store.isEmpty()).isTrue();
  }
}
