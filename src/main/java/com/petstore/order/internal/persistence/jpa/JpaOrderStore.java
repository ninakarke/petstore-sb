package com.petstore.order.internal.persistence.jpa;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.internal.OrderStore;
import com.petstore.shared.RelationalStore;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relational adapter of the order port.
 *
 * <p>An order is one row with its line rows, written in one transaction. Its hand-off state is
 * columns of that same row, so "the order exists" and "the order still has to be delivered" can
 * never disagree.
 */
@Repository
@RelationalStore
@Transactional(readOnly = true)
class JpaOrderStore implements OrderStore {

  private final EntityManager entityManager;

  JpaOrderStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Writes at once, so a broken uniqueness rule (order id, or idempotency key per user) is reported
   * here, and reports it as the store-neutral exception the port promises.
   */
  @Override
  @Transactional
  public void save(Order order) {
    try {
      entityManager.persist(new OrderEntity(order));
      entityManager.flush();
    } catch (EntityExistsException | ConstraintViolationException duplicate) {
      throw new DataIntegrityViolationException(
          "Order " + order.orderId() + " could not be stored: it breaks a uniqueness rule",
          duplicate);
    }
  }

  @Override
  public Optional<Order> find(String orderId) {
    return Optional.ofNullable(entityManager.find(OrderEntity.class, orderId))
        .map(OrderEntity::toOrder);
  }

  @Override
  public Optional<Order> findByIdempotencyKey(String userId, String idempotencyKey) {
    return entityManager
        .createQuery(
            "select o from OrderEntity o where o.userId = :userId and o.idempotencyKey = :key",
            OrderEntity.class)
        .setParameter("userId", userId)
        .setParameter("key", idempotencyKey)
        .getResultStream()
        .findFirst()
        .map(OrderEntity::toOrder);
  }

  @Override
  public List<Order> findByUser(String userId) {
    return entityManager
        .createQuery(
            "select o from OrderEntity o where o.userId = :userId"
                + " order by o.placedAt desc, o.orderId desc",
            OrderEntity.class)
        .setParameter("userId", userId)
        .getResultStream()
        .map(OrderEntity::toOrder)
        .toList();
  }

  @Override
  public List<Order> findPendingHandoff(int limit) {
    return entityManager
        .createQuery(
            "select o from OrderEntity o where o.handoffStatus = :status"
                + " order by o.placedAt, o.orderId",
            OrderEntity.class)
        .setParameter("status", HandoffStatus.PENDING)
        .setMaxResults(limit)
        .getResultStream()
        .map(OrderEntity::toOrder)
        .toList();
  }

  @Override
  public List<Order> ordersFrom(String startOrderId, int limit) {
    TypedQuery<OrderEntity> query =
        startOrderId == null
            ? entityManager.createQuery(
                "select o from OrderEntity o order by o.orderId", OrderEntity.class)
            : entityManager
                .createQuery(
                    "select o from OrderEntity o where o.orderId >= :start order by o.orderId",
                    OrderEntity.class)
                .setParameter("start", startOrderId);
    return query.setMaxResults(limit).getResultList().stream().map(OrderEntity::toOrder).toList();
  }

  @Override
  @Transactional
  public void updateHandoff(String orderId, Handoff handoff) {
    OrderEntity order = entityManager.find(OrderEntity.class, orderId);
    if (order == null) {
      throw new IllegalArgumentException("No such order: " + orderId);
    }
    order.setHandoff(handoff);
  }
}
