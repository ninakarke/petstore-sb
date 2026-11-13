package com.petstore.order.internal.handoff;

import com.petstore.order.Order;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands an order off by writing the purchase-order document to the log.
 *
 * <p>For running without a message broker. The document is the same one the broker would carry.
 */
public class LoggingOrderHandoffPublisher implements OrderHandoffPublisher {

  private static final Logger log = LoggerFactory.getLogger(LoggingOrderHandoffPublisher.class);

  private final Clock clock;

  public LoggingOrderHandoffPublisher(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void publish(Order order) {
    log.info(
        "Purchase order {} handed off (log only):\n{}",
        order.orderId(),
        PurchaseOrderXml.write(order, clock.getZone()));
  }
}
