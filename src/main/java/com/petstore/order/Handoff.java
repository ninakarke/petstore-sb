package com.petstore.order;

import java.time.Instant;

/**
 * The state of an order's delivery to order processing.
 *
 * <p>It is part of the order and is written with it in one atomic step. That is what guarantees
 * that an order which exists will be delivered: there is no moment at which the order is stored but
 * its need for delivery is not. The legacy storefront sent the message without storing anything,
 * and lost the order if sending failed.
 *
 * @param attempts how many deliveries have been tried
 * @param lastAttemptAt when the last one was tried; {@code null} before the first
 */
public record Handoff(HandoffStatus status, int attempts, Instant lastAttemptAt) {

  public static Handoff pending() {
    return new Handoff(HandoffStatus.PENDING, 0, null);
  }

  public boolean isSent() {
    return status == HandoffStatus.SENT;
  }
}
