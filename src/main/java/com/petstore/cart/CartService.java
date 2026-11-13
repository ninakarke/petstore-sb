package com.petstore.cart;

import com.petstore.cart.internal.SessionCart;
import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The visitor's shopping cart.
 *
 * <p>Replaces the business methods of the legacy {@code ShoppingCartLocalEJB} and the {@code
 * CartEJBAction} that drove it. The rules are the legacy ones unless noted on the method.
 */
@Service
public class CartService {

  private final SessionCart cart;
  private final CatalogService catalog;

  public CartService(SessionCart cart, CatalogService catalog) {
    this.cart = cart;
    this.catalog = catalog;
  }

  /**
   * Puts an item in the cart with quantity one. If the item is already in the cart its quantity
   * becomes one again; it is not increased. That is the legacy behaviour ({@code cart.put(itemID,
   * new Integer(1))}) and it is kept.
   *
   * <p>Only items that exist in the catalog can be added. The legacy cart accepted any id and the
   * cart page then failed.
   *
   * @throws UnknownItemException if the catalog has no such item
   */
  public void add(String itemId, Locale locale) {
    catalog.item(itemId, locale).orElseThrow(() -> new UnknownItemException(itemId));
    cart.put(itemId, 1);
  }

  /** Removes an item from the cart. Removing one that is not there does nothing. */
  public void remove(String itemId) {
    cart.remove(itemId);
  }

  /**
   * Sets the quantities of items in the cart. A quantity of zero or less removes the line, as in
   * the legacy cart.
   *
   * <p>Ids that are not in the cart are ignored. The legacy update added them, so the quantity form
   * could be used to put arbitrary ids into the cart.
   */
  public void updateQuantities(Map<String, Integer> quantities) {
    quantities.forEach((itemId, quantity) -> {
      if (!cart.contains(itemId)) {
        return;
      }
      if (quantity > 0) {
        cart.put(itemId, quantity);
      } else {
        cart.remove(itemId);
      }
    });
  }

  /** Empties the cart. */
  public void empty() {
    cart.clear();
  }

  /**
   * The cart as it is shown: every line with the item's current name and list price in the
   * visitor's language.
   *
   * <p>An item without details in that language is shown with the details of the default language;
   * the legacy cart failed with a {@code NullPointerException} in that case. An item that has left
   * the catalog altogether is left out.
   */
  public CartView view(Locale locale) {
    List<CartLine> lines = new ArrayList<>();
    cart.quantities()
        .forEach((itemId, quantity) ->
            catalog.item(itemId, locale).map(item -> line(item, quantity)).ifPresent(lines::add));
    return new CartView(lines);
  }

  private static CartLine line(Item item, int quantity) {
    return new CartLine(
        item.itemId(),
        item.productId(),
        item.categoryId(),
        item.productName(),
        item.attribute(),
        quantity,
        item.listPrice());
  }
}
