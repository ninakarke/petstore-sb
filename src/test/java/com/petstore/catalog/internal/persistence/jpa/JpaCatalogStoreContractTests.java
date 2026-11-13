package com.petstore.catalog.internal.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.internal.CatalogStore;
import com.petstore.catalog.internal.CatalogStoreContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the catalog store contract against the relational adapter.
 *
 * <p>Subclasses choose the database. The schema always comes from the Flyway migrations and is
 * validated against the entities when the test context starts.
 *
 * <p>The tests deliberately run without a surrounding test transaction, so every call to the
 * adapter opens and commits its own transaction exactly as it does in the running application. The
 * tables are emptied before each test instead of being rolled back after it.
 */
@DataJpaTest
@Import(JpaCatalogStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class JpaCatalogStoreContractTests extends CatalogStoreContract {

  @Autowired private CatalogStore store;
  @Autowired private JdbcTemplate jdbc;

  @Override
  protected CatalogStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    JdbcTestUtils.deleteFromTables(
        jdbc, "item_details", "item", "product_details", "product", "category_details", "category");
  }

  @Test
  void storeWithoutCategoriesIsEmpty() {
    emptyTheStore();

    assertThat(store.isEmpty()).isTrue();
  }
}
