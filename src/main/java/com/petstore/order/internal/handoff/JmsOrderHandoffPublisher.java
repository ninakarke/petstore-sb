package com.petstore.order.internal.handoff;

import com.petstore.order.Order;
import com.petstore.order.OrderProperties;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Hands an order off by putting its purchase-order document on a JMS queue, as a text message.
 *
 * <p>Replaces the legacy {@code AsyncSenderEJB}, which did the same through a queue and connection
 * factory looked up in JNDI ({@code jms/opc/OrderQueue}). The queue keeps that name. A failure to
 * send is not handled here: it propagates to the relay, which records it and tries again later.
 */
@Component
@ConditionalOnProperty(
    name = "petstore.order.handoff.transport",
    havingValue = "jms",
    matchIfMissing = true)
class JmsOrderHandoffPublisher implements OrderHandoffPublisher {

  private final JmsTemplate jms;
  private final String queue;
  private final Clock clock;

  JmsOrderHandoffPublisher(JmsTemplate jms, OrderProperties properties, Clock clock) {
    this.jms = jms;
    this.queue = properties.handoff().queue();
    this.clock = clock;
  }

  @Override
  public void publish(Order order) {
    jms.convertAndSend(queue, PurchaseOrderXml.write(order, clock.getZone()));
  }
}
