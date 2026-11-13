package com.petstore.order.internal.handoff;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderProperties;
import com.petstore.order.internal.OrderStore;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gets stored orders to order processing, and keeps trying until each one has arrived.
 *
 * <p>This is the second half of the transactional-outbox pattern. The first half is that an order
 * is stored with its hand-off marked as pending. This class takes pending orders, publishes them
 * and marks them as sent. If publishing fails, the order stays pending and is tried again later.
 *
 * <p>The legacy storefront had neither half: it called the JMS sender once, inside the request, and
 * if the sender could not be obtained it printed a stack trace, emptied the cart and told the
 * customer the order was placed.
 *
 * <p>Delivery is at-least-once: after a crash between publishing and marking, an order is published
 * again. The order id in the message lets the receiver recognise a repeat.
 */
@Component
public class OrderHandoffRelay {

  private static final Logger log = LoggerFactory.getLogger(OrderHandoffRelay.class);

  private final OrderStore orders;
  private final OrderHandoffPublisher publisher;
  private final OrderProperties properties;
  private final Clock clock;

  public OrderHandoffRelay(
      OrderStore orders, OrderHandoffPublisher publisher, OrderProperties properties, Clock clock) {
    this.orders = orders;
    this.publisher = publisher;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Tries once to deliver an order and records the outcome on it.
   *
   * @return the hand-off state after the attempt
   */
  public Handoff deliver(Order order) {
    int attempts = order.handoff().attempts() + 1;
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    Handoff outcome;
    try {
      publisher.publish(order);
      outcome = new Handoff(HandoffStatus.SENT, attempts, now);
    } catch (RuntimeException failure) {
      outcome = new Handoff(HandoffStatus.PENDING, attempts, now);
      log.warn(
          "Order {} could not be handed off (attempt {}); it stays pending and will be retried:"
              + " {}",
          order.orderId(),
          attempts,
          failure.toString());
    }
    orders.updateHandoff(order.orderId(), outcome);
    return outcome;
  }

  /**
   * Tries again for orders that are still pending. A new order that has not had its first attempt
   * yet is left alone for a short while, so that it is not published twice, once by the request
   * that placed it and once from here.
   */
  @Scheduled(
      fixedDelayString = "${petstore.order.handoff.retry-interval:30s}",
      initialDelayString = "${petstore.order.handoff.retry-interval:30s}")
  public void retryPending() {
    Instant graceEnds = clock.instant().minus(properties.handoff().grace());
    for (Order order : orders.findPendingHandoff(properties.handoff().batchSize())) {
      boolean firstAttemptStillDue =
          order.handoff().attempts() == 0 && order.placedAt().isAfter(graceEnds);
      if (!firstAttemptStillDue) {
        deliver(order);
      }
    }
  }
}
