package com.petstore.order.internal.handoff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.petstore.order.Order;
import com.petstore.order.OrderService;
import com.petstore.order.RequestedLine;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * The stand-in for the out-of-scope order-processing application. With it switched on, an order can
 * be followed from the checkout to the far side of the queue.
 *
 * <p>Legacy counterpart of what it stands in for: {@code apps/opc/.../ejb/PurchaseOrderMDB.java}.
 */
@SpringBootTest(properties = "petstore.order.handoff.stub-consumer=true")
@ExtendWith(OutputCaptureExtension.class)
class OrderProcessingStubTests {

  @Autowired private OrderService orders;

  @Test
  @DisplayName("the stand-in takes the purchase order off the queue and reports it")
  void stubReceivesThePurchaseOrder(CapturedOutput output) {
    Order order =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 2)), null, null, null).order();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(output.getOut())
                .contains(
                    "Order processing (stub) received purchase order "
                        + order.orderId()
                        + " from user j2ee, total 33.00"));
  }
}
