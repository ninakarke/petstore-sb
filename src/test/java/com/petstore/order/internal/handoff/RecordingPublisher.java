package com.petstore.order.internal.handoff;

import com.petstore.order.Order;
import java.util.ArrayList;
import java.util.List;

/** A hand-off publisher for tests: remembers what it was given and can be told to fail. */
public class RecordingPublisher implements OrderHandoffPublisher {

  private final List<Order> published = new ArrayList<>();
  private boolean failing;

  @Override
  public void publish(Order order) {
    if (failing) {
      throw new IllegalStateException("order processing is not reachable");
    }
    published.add(order);
  }

  public void failFromNowOn() {
    failing = true;
  }

  public void workAgain() {
    failing = false;
  }

  public List<String> publishedOrderIds() {
    return published.stream().map(Order::orderId).toList();
  }
}
