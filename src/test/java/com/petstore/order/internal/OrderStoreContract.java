package com.petstore.order.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderFixtures;
import com.petstore.order.OrderLine;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The contract every {@link OrderStore} adapter must fulfil. Written once against the port and run
 * for each data store by a small subclass.
 *
 * <p>The legacy storefront did not store orders, so there is no legacy counterpart for most of
 * this. What it protects is new: an order and its need to be handed off are stored together.
 */
public abstract class OrderStoreContract {

  private static final Instant NOON = OrderFixtures.NOON;

  /** The adapter under test. */
  protected abstract OrderStore store();

  /** Hook for stores that do not roll back after a test. */
  protected void emptyTheStore() {}

  @BeforeEach
  void startEmpty() {
    emptyTheStore();
  }

  @Test
  void savedOrderIsReadBackComplete() {
    Order order = OrderFixtures.order("10011", "j2ee", NOON);

    store().save(order);

    assertThat(store().find("10011")).contains(order);
  }

  @Test
  void unknownOrderIsAbsent() {
    assertThat(store().find("99999")).isEmpty();
  }

  @Test
  @DisplayName("legacy: lines are numbered from zero and keep that order")
  void linesKeepTheirOrder() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));

    assertThat(store().find("10011").orElseThrow().lines())
        .extracting(OrderLine::lineNumber, OrderLine::itemId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(0, "EST-1"),
            org.assertj.core.groups.Tuple.tuple(1, "EST-6"));
  }

  @Test
  void orderIdCanBeUsedOnlyOnce() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));

    assertThatThrownBy(() -> store().save(OrderFixtures.order("10011", "shopper", NOON)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void ordersOfAUserAreListedNewestFirst() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));
    store().save(OrderFixtures.order("10012", "shopper", NOON.plusSeconds(60)));
    store().save(OrderFixtures.order("10013", "j2ee", NOON.plusSeconds(120)));

    assertThat(store().findByUser("j2ee"))
        .extracting(Order::orderId)
        .containsExactly("10013", "10011");
    assertThat(store().findByUser("shopper")).extracting(Order::orderId).containsExactly("10012");
    assertThat(store().findByUser("nobody")).isEmpty();
  }

  // ---- hand-off state ------------------------------------------------------------------------

  @Test
  @DisplayName("an order is stored together with its pending hand-off")
  void newOrderIsPendingHandoff() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));

    assertThat(store().find("10011").orElseThrow().handoff()).isEqualTo(Handoff.pending());
    assertThat(store().findPendingHandoff(10)).extracting(Order::orderId).containsExactly("10011");
  }

  @Test
  void pendingHandoffsAreListedOldestFirstUpToTheLimit() {
    store().save(OrderFixtures.order("10013", "j2ee", NOON.plusSeconds(120)));
    store().save(OrderFixtures.order("10011", "j2ee", NOON));
    store().save(OrderFixtures.order("10012", "j2ee", NOON.plusSeconds(60)));

    assertThat(store().findPendingHandoff(2))
        .extracting(Order::orderId)
        .containsExactly("10011", "10012");
  }

  @Test
  void failedAttemptIsRecordedAndTheOrderStaysPending() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));
    Handoff afterFailure = new Handoff(HandoffStatus.PENDING, 1, NOON.plusSeconds(5));

    store().updateHandoff("10011", afterFailure);

    assertThat(store().find("10011").orElseThrow().handoff()).isEqualTo(afterFailure);
    assertThat(store().findPendingHandoff(10)).hasSize(1);
  }

  @Test
  void deliveredOrderIsNoLongerPending() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));
    store().save(OrderFixtures.order("10012", "j2ee", NOON.plusSeconds(60)));
    Handoff sent = new Handoff(HandoffStatus.SENT, 2, NOON.plusSeconds(90));

    store().updateHandoff("10011", sent);

    assertThat(store().find("10011").orElseThrow().handoff()).isEqualTo(sent);
    assertThat(store().findPendingHandoff(10)).extracting(Order::orderId).containsExactly("10012");
  }

  @Test
  void handoffOfAnUnknownOrderCannotBeUpdated() {
    assertThatThrownBy(() -> store().updateHandoff("99999", Handoff.pending()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---- idempotency key -----------------------------------------------------------------------

  @Test
  void orderIsFoundByItsIdempotencyKey() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON, "key-1"));

    assertThat(store().findByIdempotencyKey("j2ee", "key-1")).map(Order::orderId).contains("10011");
    assertThat(store().findByIdempotencyKey("j2ee", "key-2")).isEmpty();
    assertThat(store().findByIdempotencyKey("shopper", "key-1")).isEmpty();
  }

  @Test
  @DisplayName("a user cannot have two orders under the same idempotency key")
  void idempotencyKeyIsUniquePerUser() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON, "key-1"));

    assertThatThrownBy(() -> store().save(OrderFixtures.order("10012", "j2ee", NOON, "key-1")))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(store().find("10012")).isEmpty();
  }

  @Test
  void differentUsersMayUseTheSameIdempotencyKey() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON, "key-1"));
    store().save(OrderFixtures.order("10012", "shopper", NOON, "key-1"));

    assertThat(store().findByIdempotencyKey("shopper", "key-1"))
        .map(Order::orderId)
        .contains("10012");
  }

  @Test
  void ordersWithoutAnIdempotencyKeyDoNotCollide() {
    store().save(OrderFixtures.order("10011", "j2ee", NOON));
    store().save(OrderFixtures.order("10012", "j2ee", NOON.plusSeconds(1)));

    assertThat(store().findByUser("j2ee")).hasSize(2);
  }
}
