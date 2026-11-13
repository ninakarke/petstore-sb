package com.petstore.order.internal;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;

/**
 * An order store that keeps everything in memory, for fast tests of the logic above the port. Held
 * to the same {@link OrderStoreContract} as the real adapters.
 */
public class InMemoryOrderStore implements OrderStore {

  private final Map<String, Order> orders = new LinkedHashMap<>();

  @Override
  public synchronized void save(Order order) {
    if (orders.containsKey(order.orderId())) {
      throw new DuplicateKeyException("Order id taken: " + order.orderId());
    }
    if (order.idempotencyKey() != null
        && findByIdempotencyKey(order.userId(), order.idempotencyKey()).isPresent()) {
      throw new DuplicateKeyException("Idempotency key used: " + order.idempotencyKey());
    }
    orders.put(order.orderId(), order);
  }

  @Override
  public synchronized Optional<Order> find(String orderId) {
    return Optional.ofNullable(orders.get(orderId));
  }

  @Override
  public synchronized Optional<Order> findByIdempotencyKey(String userId, String idempotencyKey) {
    return orders.values().stream()
        .filter(order -> order.userId().equals(userId))
        .filter(order -> idempotencyKey.equals(order.idempotencyKey()))
        .findFirst();
  }

  @Override
  public synchronized List<Order> findByUser(String userId) {
    return orders.values().stream()
        .filter(order -> order.userId().equals(userId))
        .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::orderId).reversed())
        .toList();
  }

  @Override
  public synchronized List<Order> findPendingHandoff(int limit) {
    return orders.values().stream()
        .filter(order -> order.handoff().status() == HandoffStatus.PENDING)
        .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::orderId))
        .limit(limit)
        .toList();
  }

  @Override
  public synchronized void updateHandoff(String orderId, Handoff handoff) {
    Order order = orders.get(orderId);
    if (order == null) {
      throw new IllegalArgumentException("No such order: " + orderId);
    }
    orders.put(orderId, order.withHandoff(handoff));
  }

  public synchronized int size() {
    return orders.size();
  }

  public synchronized void clear() {
    orders.clear();
  }

  @Override
  public synchronized List<Order> ordersFrom(String startOrderId, int limit) {
    return orders.values().stream()
        .filter(order -> startOrderId == null || order.orderId().compareTo(startOrderId) >= 0)
        .sorted(Comparator.comparing(Order::orderId))
        .limit(limit)
        .toList();
  }
}
