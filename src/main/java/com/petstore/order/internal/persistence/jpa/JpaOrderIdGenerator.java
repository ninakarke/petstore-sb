package com.petstore.order.internal.persistence.jpa;

import com.petstore.order.internal.OrderIdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relational adapter of the order-number generator.
 *
 * <p>Replaces the legacy {@code CounterEJB}, whose {@code getNextValue} read the counter, added one
 * and wrote it back with nothing to stop two requests doing so at the same moment, and which
 * created its row on first use. Here the row exists from the start (the migration inserts it) and
 * is locked while it is incremented, so two requests can never be given the same number.
 */
@Repository
class JpaOrderIdGenerator implements OrderIdGenerator {

  private final EntityManager entityManager;

  JpaOrderIdGenerator(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Runs in a transaction of its own, so the row lock is held only for the increment. */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String nextOrderId() {
    CounterEntity counter =
        entityManager.find(CounterEntity.class, PREFIX, LockModeType.PESSIMISTIC_WRITE);
    if (counter == null) {
      throw new IllegalStateException("The order counter row '" + PREFIX + "' is missing");
    }
    counter.value += 1;
    // Legacy: getName() + String.valueOf(count)
    return PREFIX + counter.value;
  }
}
