package com.petstore.order;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Order settings.
 *
 * @param maxQuantity the largest quantity of one item a client of the JSON API may order
 * @param handoff settings of the delivery of orders to order processing
 */
@ConfigurationProperties("petstore.order")
public record OrderProperties(@DefaultValue("999") int maxQuantity, @DefaultValue Handoff handoff) {

  /**
   * @param retryInterval how often orders that could not be delivered are tried again
   * @param grace how long a new order is left to its first, immediate delivery attempt before the
   *     retry run may pick it up
   * @param batchSize how many pending orders one retry run handles
   * @param transport how orders are delivered: {@code jms} puts the purchase-order document on a
   *     queue; {@code log} only writes it to the log
   * @param queue the queue orders are put on. The default is the name the legacy application used,
   *     {@code jms/opc/OrderQueue}
   * @param stubConsumer whether the application also runs a stand-in for the order-processing
   *     application that takes the messages off the queue and logs them
   */
  public record Handoff(
      @DefaultValue("30s") Duration retryInterval,
      @DefaultValue("15s") Duration grace,
      @DefaultValue("50") int batchSize,
      @DefaultValue("jms") String transport,
      @DefaultValue("jms/opc/OrderQueue") String queue,
      @DefaultValue("true") boolean stubConsumer) {}
}
