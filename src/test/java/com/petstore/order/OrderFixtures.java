package com.petstore.order;

import com.petstore.customer.CustomerFixtures;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sample orders for tests. */
public final class OrderFixtures {

  /** A fixed moment with no fraction finer than a database stores. */
  public static final Instant NOON = Instant.parse("2026-11-13T12:00:00Z");

  private OrderFixtures() {}

  public static List<OrderLine> lines() {
    return List.of(
        new OrderLine("FISH", "FI-SW-01", "EST-1", 0, 3, new BigDecimal("16.50")),
        new OrderLine("DOGS", "K9-BD-01", "EST-6", 1, 1, new BigDecimal("18.50")));
  }

  /** An order of 3 x 16.50 + 1 x 18.50 = 68.00 that still has to be handed off. */
  public static Order order(String orderId, String userId, Instant placedAt) {
    return order(orderId, userId, placedAt, null);
  }

  public static Order order(
      String orderId, String userId, Instant placedAt, String idempotencyKey) {
    return new Order(
        orderId,
        userId,
        "aaa@bbb.ccc",
        placedAt,
        CustomerFixtures.contactInfo(),
        CustomerFixtures.contactInfo(),
        CustomerFixtures.creditCard(),
        lines(),
        new BigDecimal("68.00"),
        Handoff.pending(),
        idempotencyKey);
  }
}
