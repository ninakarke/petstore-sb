package com.petstore.cart.internal;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * The cart's state: which items, and how many of each. One instance per browser session.
 *
 * <p>Replaces the conversational state of the legacy stateful session bean {@code
 * ShoppingCartLocalEJB}, which lived in the EJB container and was referenced from the HTTP session.
 * As there, the cart disappears with the session.
 *
 * <p>It holds item ids and quantities only, never prices or names, so it stays small, serialises
 * cleanly, and always shows current catalog data. Lines keep the order in which they were added;
 * the legacy bean used a {@code HashMap} and showed them in no particular order.
 */
@Component
@SessionScope
public class SessionCart implements Serializable {

  private static final long serialVersionUID = 1L;

  private final LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();

  /** A copy of the cart's content, in the order the items were added. */
  public synchronized Map<String, Integer> quantities() {
    return new LinkedHashMap<>(quantities);
  }

  public synchronized boolean contains(String itemId) {
    return quantities.containsKey(itemId);
  }

  public synchronized void put(String itemId, int quantity) {
    quantities.put(itemId, quantity);
  }

  public synchronized void remove(String itemId) {
    quantities.remove(itemId);
  }

  public synchronized void clear() {
    quantities.clear();
  }
}
