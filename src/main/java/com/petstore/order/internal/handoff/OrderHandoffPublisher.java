package com.petstore.order.internal.handoff;

import com.petstore.order.Order;

/**
 * Delivers an order to order processing.
 *
 * <p>Legacy equivalent: {@code AsyncSenderEJB.sendAMessage}. An implementation either delivers the
 * order or throws; it must not swallow a failure, because the caller records the outcome.
 */
public interface OrderHandoffPublisher {

  void publish(Order order);
}
