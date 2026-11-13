package com.petstore.order.internal.handoff;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.Order;
import com.petstore.order.OrderService;
import com.petstore.order.RequestedLine;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Running without a queue: the purchase-order document is written to the log instead. */
@SpringBootTest(properties = "petstore.order.handoff.transport=log")
@ExtendWith(OutputCaptureExtension.class)
class LogTransportTests {

  @Autowired private OrderService orders;
  @Autowired private OrderHandoffPublisher publisher;

  @Test
  @DisplayName("transport=log: orders are handed off by logging the same document")
  void orderIsLoggedInsteadOfQueued(CapturedOutput output) {
    Order order =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 1)), null, null, null).order();

    assertThat(publisher).isInstanceOf(LoggingOrderHandoffPublisher.class);
    assertThat(order.handoff().isSent()).isTrue();
    assertThat(output.getOut())
        .contains("Purchase order " + order.orderId() + " handed off (log only)")
        .contains("<OrderId>" + order.orderId() + "</OrderId>");
  }
}
