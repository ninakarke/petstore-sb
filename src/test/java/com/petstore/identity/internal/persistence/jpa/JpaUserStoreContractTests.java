package com.petstore.identity.internal.persistence.jpa;

import com.petstore.identity.internal.UserStore;
import com.petstore.identity.internal.UserStoreContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the user store contract against the relational adapter. Subclasses choose the database.
 * Every adapter call runs in its own transaction, as in the running application.
 */
@DataJpaTest
@Import(JpaUserStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class JpaUserStoreContractTests extends UserStoreContract {

  @Autowired private UserStore store;
  @Autowired private JdbcTemplate jdbc;

  @Override
  protected UserStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    JdbcTestUtils.deleteFromTables(jdbc, "signon_user");
  }
}
