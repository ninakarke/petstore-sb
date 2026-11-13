package com.petstore.order.internal.handoff;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderFixtures;
import com.petstore.order.OrderProperties;
import com.petstore.order.internal.InMemoryOrderStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Delivering stored orders to order processing, and retrying until they have arrived.
 *
 * <p>Legacy source: the sending block of {@code OrderEJBAction}, which tried once and swallowed the
 * failure ({@code sle.printStackTrace()}), after which the cart was emptied anyway.
 */
class OrderHandoffRelayTests {

  private static final Instant NOON = OrderFixtures.NOON;
  private static final Instant LATER = NOON.plus(Duration.ofMinutes(5));

  private final InMemoryOrderStore store = new InMemoryOrderStore();
  private final RecordingPublisher publisher = new RecordingPublisher();
  private final OrderProperties properties =
      new OrderProperties(
          999,
          new OrderProperties.Handoff(
              Duration.ofSeconds(30),
              Duration.ofSeconds(15),
              2,
              "log",
              "jms/opc/OrderQueue",
              false));
  private final OrderHandoffRelay relay =
      new OrderHandoffRelay(store, publisher, properties, Clock.fixed(LATER, ZoneOffset.UTC));

  @Test
  void deliveredOrderIsMarkedAsSent() {
    Order order = stored("10011", NOON);

    Handoff outcome = relay.deliver(order);

    assertThat(outcome).isEqualTo(new Handoff(HandoffStatus.SENT, 1, LATER));
    assertThat(store.find("10011").orElseThrow().handoff()).isEqualTo(outcome);
    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
  }

  @Test
  @DisplayName("fixed: a failed delivery is recorded and the order stays pending")
  void failedDeliveryIsRecordedAndTheOrderStaysPending() {
    publisher.failFromNowOn();
    Order order = stored("10011", NOON);

    Handoff outcome = relay.deliver(order);

    assertThat(outcome).isEqualTo(new Handoff(HandoffStatus.PENDING, 1, LATER));
    assertThat(store.findPendingHandoff(10)).hasSize(1);
  }

  @Test
  @DisplayName("fixed: an order that could not be delivered is delivered by a later retry run")
  void retryDeliversWhatFailedEarlier() {
    publisher.failFromNowOn();
    relay.deliver(stored("10011", NOON));
    relay.retryPending();
    assertThat(store.find("10011").orElseThrow().handoff().attempts()).isEqualTo(2);

    publisher.workAgain();
    relay.retryPending();

    assertThat(store.find("10011").orElseThrow().handoff())
        .isEqualTo(new Handoff(HandoffStatus.SENT, 3, LATER));
    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
    assertThat(store.findPendingHandoff(10)).isEmpty();
  }

  @Test
  @DisplayName(
      "an order whose first attempt never happened, for example after a crash, is picked up")
  void orderLeftBehindWithoutAnAttemptIsPickedUp() {
    stored("10011", NOON);

    relay.retryPending();

    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
  }

  @Test
  @DisplayName("a brand-new order is left to its own first attempt, so it is not delivered twice")
  void brandNewOrderIsLeftAlone() {
    stored("10011", LATER.minusSeconds(2));

    relay.retryPending();

    assertThat(publisher.publishedOrderIds()).isEmpty();
    assertThat(store.find("10011").orElseThrow().handoff()).isEqualTo(Handoff.pending());
  }

  @Test
  void oneRetryRunHandlesAtMostOneBatchOldestFirst() {
    stored("10013", NOON.plusSeconds(2));
    stored("10011", NOON);
    stored("10012", NOON.plusSeconds(1));

    relay.retryPending();

    assertThat(publisher.publishedOrderIds()).containsExactly("10011", "10012");
    relay.retryPending();
    assertThat(publisher.publishedOrderIds()).containsExactly("10011", "10012", "10013");
  }

  @Test
  void ordersAlreadySentAreNotSentAgain() {
    relay.deliver(stored("10011", NOON));

    relay.retryPending();

    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
  }

  @Test
  void loggingPublisherWritesTheDocumentWithoutFailing() {
    new LoggingOrderHandoffPublisher(Clock.fixed(LATER, ZoneOffset.UTC))
        .publish(OrderFixtures.order("10011", "j2ee", NOON));
  }

  private Order stored(String orderId, Instant placedAt) {
    Order order = OrderFixtures.order(orderId, "j2ee", placedAt);
    store.save(order);
    return order;
  }
}
