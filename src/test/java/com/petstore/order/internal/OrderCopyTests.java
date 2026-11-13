package com.petstore.order.internal;

import static com.petstore.order.OrderFixtures.NOON;
import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderFixtures;
import com.petstore.shared.CopyReport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Copying orders and the order numbering from one store to another, between in-memory stores. */
class OrderCopyTests {

  private final InMemoryOrderStore source = new InMemoryOrderStore();
  private final SequenceOrderIdGenerator sourceNumbers = new SequenceOrderIdGenerator();
  private final InMemoryOrderStore target = new InMemoryOrderStore();
  private final SequenceOrderIdGenerator targetNumbers = new SequenceOrderIdGenerator();

  @Test
  @DisplayName("orders are copied complete and keep their hand-off state")
  void copiesOrdersWithTheirHandoffState() {
    Handoff sent = new Handoff(HandoffStatus.SENT, 1, NOON.plusSeconds(1));
    source.save(OrderFixtures.order(sourceNumbers.nextOrderId(), "j2ee", NOON, "key-1"));
    source.updateHandoff("10011", sent);
    source.save(OrderFixtures.order(sourceNumbers.nextOrderId(), "shopper", NOON.plusSeconds(60)));

    List<CopyReport> reports = copy();

    assertThat(reports).extracting(CopyReport::what).containsExactly("orders", "order counter");
    assertThat(reports).allMatch(CopyReport::matches);
    assertThat(reports.get(0).copied()).isEqualTo(2);
    assertThat(target.find("10011")).isEqualTo(source.find("10011"));
    assertThat(target.find("10011").orElseThrow().handoff()).isEqualTo(sent);
    // Delivered orders are not delivered again; the waiting one is still waiting.
    assertThat(target.findPendingHandoff(10)).extracting(Order::orderId).containsExactly("10012");
    assertThat(target.findByIdempotencyKey("j2ee", "key-1")).isPresent();
  }

  @Test
  @DisplayName("numbering continues after the last number of the old store")
  void numberingContinues() {
    source.save(OrderFixtures.order(sourceNumbers.nextOrderId(), "j2ee", NOON));
    source.save(OrderFixtures.order(sourceNumbers.nextOrderId(), "j2ee", NOON.plusSeconds(1)));
    sourceNumbers.nextOrderId(); // a number that was used up without an order being stored

    List<CopyReport> reports = copy();

    assertThat(reports.get(1).inSource()).isEqualTo(3);
    assertThat(reports.get(1).inTarget()).isEqualTo(3);
    assertThat(targetNumbers.nextOrderId()).isEqualTo("10014");
  }

  @Test
  @DisplayName("times finer than a millisecond in an old store are cut, and the copy still matches")
  void finerTimesAreCut() {
    Order withMicroseconds =
        OrderFixtures.order("10011", "j2ee", NOON.plusNanos(123_456_000))
            .withHandoff(new Handoff(HandoffStatus.PENDING, 1, NOON.plusNanos(987_654_000)));
    source.save(withMicroseconds);

    List<CopyReport> reports = copy();

    Order copied = target.find("10011").orElseThrow();
    assertThat(copied.placedAt()).isEqualTo(NOON.plusMillis(123));
    assertThat(copied.handoff().lastAttemptAt()).isEqualTo(NOON.plusMillis(987));
    assertThat(reports.get(0).matches()).isTrue();
  }

  @Test
  void runningAgainCopiesNothingAndDoesNotMoveTheCounter() {
    source.save(OrderFixtures.order(sourceNumbers.nextOrderId(), "j2ee", NOON));
    copy();

    List<CopyReport> again = copy();

    assertThat(again.get(0).copied()).isZero();
    assertThat(again).allMatch(CopyReport::matches);
    assertThat(targetNumbers.lastNumber()).isEqualTo(1);
  }

  private List<CopyReport> copy() {
    return OrderCopy.copy(source, sourceNumbers, target, targetNumbers, 2);
  }
}
