package com.petstore.customer.internal.persistence.jpa;

import com.petstore.customer.internal.CustomerStore;
import com.petstore.customer.internal.CustomerStoreContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the customer store contract against the relational adapter. Subclasses choose the database.
 * Every adapter call runs in its own transaction, as in the running application.
 */
@DataJpaTest
@Import(JpaCustomerStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class JpaCustomerStoreContractTests extends CustomerStoreContract {

  @Autowired private CustomerStore store;
  @Autowired private JdbcTemplate jdbc;

  @Override
  protected CustomerStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    JdbcTestUtils.deleteFromTables(
        jdbc, "profile", "credit_card", "address", "contact_info", "account", "customer");
  }
}
