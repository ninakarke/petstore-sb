package com.petstore.order.internal;

import com.petstore.order.Handoff;
import com.petstore.order.Order;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreCopy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Copies all orders from one store to another, through the order ports on both sides, and carries
 * the order numbering over.
 *
 * <p>An order keeps its hand-off state. One that was delivered is not delivered again from the new
 * store; one that was still waiting is picked up by the new store's retry run.
 *
 * <p>The target's counter is moved up to the source's, so the first order placed on the new store
 * gets the number after the last one copied.
 */
public final class OrderCopy {

  private OrderCopy() {}

  public static List<CopyReport> copy(
      OrderStore source,
      OrderIdGenerator sourceNumbers,
      OrderStore target,
      OrderIdGenerator targetNumbers,
      int batchSize) {
    CopyReport orders =
        StoreCopy.copy(
            "orders",
            Order::orderId,
            (startOrderId, limit) ->
                source.ordersFrom(startOrderId, limit).stream()
                    .map(OrderCopy::toCommonPrecision)
                    .toList(),
            target::ordersFrom,
            target::save,
            batchSize);

    long lastNumber = sourceNumbers.lastNumber();
    targetNumbers.advanceTo(lastNumber);
    long targetNumber = targetNumbers.lastNumber();
    CopyReport counter =
        CopyReport.ofValue("order counter", lastNumber, targetNumber, targetNumber >= lastNumber);
    return List.of(orders, counter);
  }

  /**
   * Times are kept to the millisecond on every store. A source written by an earlier version may
   * hold finer ones; they are cut here, so that the copy can be compared with what was read.
   */
  static Order toCommonPrecision(Order order) {
    Handoff handoff = order.handoff();
    Order cut =
        new Order(
            order.orderId(),
            order.userId(),
            order.email(),
            millis(order.placedAt()),
            order.shipTo(),
            order.billTo(),
            order.creditCard(),
            order.lines(),
            order.totalPrice(),
            new Handoff(handoff.status(), handoff.attempts(), millis(handoff.lastAttemptAt())),
            order.idempotencyKey());
    return cut;
  }

  private static Instant millis(Instant instant) {
    return instant == null ? null : instant.truncatedTo(ChronoUnit.MILLIS);
  }
}
