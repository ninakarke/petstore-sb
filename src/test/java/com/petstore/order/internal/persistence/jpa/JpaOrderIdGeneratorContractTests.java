package com.petstore.order.internal.persistence.jpa;

import com.petstore.order.internal.OrderIdGenerator;
import com.petstore.order.internal.OrderIdGeneratorContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the order-number contract against the relational adapter. Subclasses choose the database.
 */
@DataJpaTest
@Import(JpaOrderIdGenerator.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class JpaOrderIdGeneratorContractTests extends OrderIdGeneratorContract {

  @Autowired private OrderIdGenerator generator;
  @Autowired private JdbcTemplate jdbc;

  @Override
  protected OrderIdGenerator generator() {
    return generator;
  }

  @Override
  protected void resetTheCounter() {
    jdbc.update("update counter set counter = 0 where name = ?", OrderIdGenerator.PREFIX);
  }
}
