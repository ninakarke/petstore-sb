package com.petstore.order;

/** Whether an order has been delivered to order processing. */
public enum HandoffStatus {
  /** Stored, not yet delivered. Delivery will be attempted, and retried, until it succeeds. */
  PENDING,
  /** Delivered to the order-processing queue. */
  SENT
}
