package com.petstore.order.internal.handoff;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderProperties;
import com.petstore.order.OrderService;
import com.petstore.order.RequestedLine;
import jakarta.jms.ConnectionFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.w3c.dom.Document;

/**
 * The hand-off over a real message broker: an order placed in the running application arrives on
 * the queue as a purchase-order document the legacy order-processing application could read.
 *
 * <p>Legacy sources: {@code components/asyncsender/.../ejb/AsyncSenderEJB.java} and the queue
 * binding {@code jms/AsyncSenderQueue -> jms/opc/OrderQueue} in {@code sun-j2ee-ri.xml}.
 */
@SpringBootTest
class JmsOrderHandoffTests {

  @Autowired private OrderService orders;
  @Autowired private OrderProperties properties;
  @Autowired private ConnectionFactory connectionFactory;

  private JmsTemplate queueReader;

  @BeforeEach
  void emptyTheQueue() {
    queueReader = new JmsTemplate(connectionFactory);
    queueReader.setReceiveTimeout(200);
    while (queueReader.receive(properties.handoff().queue()) != null) {
      // discard what earlier tests left behind
    }
    queueReader.setReceiveTimeout(5000);
  }

  @Test
  @DisplayName("legacy: orders go to the queue jms/opc/OrderQueue")
  void queueKeepsItsLegacyName() {
    assertThat(properties.handoff().queue()).isEqualTo("jms/opc/OrderQueue");
    assertThat(properties.handoff().transport()).isEqualTo("jms");
  }

  @Test
  @DisplayName("an order placed in the application arrives on the queue as a valid legacy document")
  void placedOrderArrivesOnTheQueue() {
    Order order =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 2)), null, null, null).order();

    String message = (String) queueReader.receiveAndConvert(properties.handoff().queue());

    assertThat(message).as("message on the queue").isNotNull();
    Document document = LegacyDtd.validate(message);
    assertThat(text(document, "OrderId")).isEqualTo(order.orderId());
    assertThat(text(document, "UserId")).isEqualTo("j2ee");
    assertThat(new BigDecimal(text(document, "TotalPrice"))).isEqualByComparingTo("33.00");
    assertThat(text(document, "ItemId")).isEqualTo("EST-1");
    assertThat(text(document, "Quantity")).isEqualTo("2");
  }

  @Test
  void orderIsMarkedAsSentOnceItIsOnTheQueue() {
    Order order =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-6", 1)), null, null, null).order();

    assertThat(order.handoff().status()).isEqualTo(HandoffStatus.SENT);
    assertThat(order.handoff().attempts()).isEqualTo(1);
    assertThat(orders.find("j2ee", order.orderId()).orElseThrow().handoff().isSent()).isTrue();
  }

  @Test
  void everyOrderIsOneMessage() {
    Order first =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 1)), null, null, null).order();
    Order second =
        orders
            .placeOrder("shopper", List.of(new RequestedLine("EST-6", 1)), null, null, null)
            .order();

    String firstMessage = (String) queueReader.receiveAndConvert(properties.handoff().queue());
    String secondMessage = (String) queueReader.receiveAndConvert(properties.handoff().queue());
    queueReader.setReceiveTimeout(200);

    assertThat(text(LegacyDtd.validate(firstMessage), "OrderId")).isEqualTo(first.orderId());
    assertThat(text(LegacyDtd.validate(secondMessage), "OrderId")).isEqualTo(second.orderId());
    assertThat(queueReader.receive(properties.handoff().queue())).isNull();
  }

  @Test
  @DisplayName(
      "a repeated API request with the same idempotency key puts no second message on the queue")
  void replayedRequestSendsNothing() {
    List<RequestedLine> lines = List.of(new RequestedLine("EST-1", 1));
    orders.placeOrder("j2ee", lines, null, null, "jms-test-key");
    queueReader.receive(properties.handoff().queue());

    orders.placeOrder("j2ee", lines, null, null, "jms-test-key");
    queueReader.setReceiveTimeout(300);

    assertThat(queueReader.receive(properties.handoff().queue())).isNull();
  }

  private static String text(Document document, String element) {
    return document.getElementsByTagName(element).item(0).getTextContent();
  }
}
