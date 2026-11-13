package com.petstore.order.internal.persistence.jpa;

import com.petstore.order.internal.OrderStore;
import com.petstore.order.internal.OrderStoreContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the order store contract against the relational adapter. Subclasses choose the database.
 * Every adapter call runs in its own transaction, as in the running application.
 */
@DataJpaTest
@Import(JpaOrderStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class JpaOrderStoreContractTests extends OrderStoreContract {

  @Autowired private OrderStore store;
  @Autowired private JdbcTemplate jdbc;

  @Override
  protected OrderStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    JdbcTestUtils.deleteFromTables(jdbc, "order_line", "purchase_order");
  }
}
