package com.petstore.order.internal;

import com.petstore.order.Handoff;
import com.petstore.order.Order;
import java.util.List;
import java.util.Optional;

/**
 * The persistence port of the order module.
 *
 * <p>An order is written as one aggregate, lines and hand-off state included, in a single atomic
 * step. No adapter may need a transaction that spans more than that one write: the document adapter
 * has none to offer. Every adapter must pass the same contract test suite.
 */
public interface OrderStore {

  /**
   * Stores a new order.
   *
   * @throws org.springframework.dao.DataIntegrityViolationException if the order id is taken, or
   *     the user already has an order with the same idempotency key
   */
  void save(Order order);

  Optional<Order> find(String orderId);

  /** The order a user placed under an idempotency key, if any. */
  Optional<Order> findByIdempotencyKey(String userId, String idempotencyKey);

  /** A user's orders, newest first. */
  List<Order> findByUser(String userId);

  /** Orders that still have to be delivered to order processing, oldest first. */
  List<Order> findPendingHandoff(int limit);

  /** Replaces the hand-off state of an order. */
  void updateHandoff(String orderId, Handoff handoff);

  /**
   * Orders in the store's key order: at most {@code limit} of them, starting with the order of the
   * given id, or with the first one if the id is {@code null}. If there is no such order, the list
   * starts with the next one in key order. Used to copy orders to another store.
   */
  List<Order> ordersFrom(String startOrderId, int limit);
}
