package com.petstore.order;

import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An order: one aggregate made of who ordered, where it goes, how it is paid, its lines and the
 * state of its hand-off to order processing.
 *
 * <p>Legacy equivalent: the {@code PurchaseOrder} document the storefront built and sent away.
 *
 * @param orderId the legacy-style order number, {@code 1001} followed by a counter
 * @param email where the confirmation is sent; the billing e-mail address
 * @param idempotencyKey a key chosen by a client of the JSON API so that a repeated request does
 *     not order twice; {@code null} for orders placed on the website
 */
public record Order(
    String orderId,
    String userId,
    String email,
    Instant placedAt,
    ContactInfo shipTo,
    ContactInfo billTo,
    CreditCard creditCard,
    List<OrderLine> lines,
    BigDecimal totalPrice,
    Handoff handoff,
    String idempotencyKey) {

  public Order {
    lines = List.copyOf(lines);
  }

  /** The same order with a different hand-off state. */
  public Order withHandoff(Handoff newHandoff) {
    return new Order(
        orderId,
        userId,
        email,
        placedAt,
        shipTo,
        billTo,
        creditCard,
        lines,
        totalPrice,
        newHandoff,
        idempotencyKey);
  }
}
