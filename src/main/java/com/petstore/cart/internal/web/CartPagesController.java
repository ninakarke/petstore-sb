package com.petstore.cart.internal.web;

import com.petstore.cart.CartService;
import com.petstore.cart.UnknownItemException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * The cart page and the actions on the cart.
 *
 * <p>Legacy equivalents: the {@code cart} screen and {@code cart.do} with {@code
 * action=purchase|update|remove}, handled by {@code CartHTMLAction}.
 *
 * <p>In the legacy application every one of those actions was a GET request, so a link could change
 * a visitor's cart. Here they are POST requests that must carry a CSRF token, each followed by a
 * redirect to the cart page.
 */
@Controller
class CartPagesController {

  /** Prefix of the quantity fields of the cart form, as in the legacy {@code cart.jsp}. */
  static final String QUANTITY_PREFIX = "itemQuantity_";

  private final CartService cart;

  CartPagesController(CartService cart) {
    this.cart = cart;
  }

  /** Legacy: {@code cart.screen}, and {@code cart.do} without an action. */
  @GetMapping("/cart")
  String show(Model model) {
    model.addAttribute("cart", cart.view());
    return "cart/cart";
  }

  /** Legacy: {@code cart.do?action=purchase&itemId=...}. */
  @PostMapping("/cart/items")
  String add(@RequestParam String itemId) {
    try {
      cart.add(itemId);
    } catch (UnknownItemException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
    }
    return "redirect:/cart";
  }

  /** Legacy: {@code cart.do?action=update&itemQuantity_<id>=...}. */
  @PostMapping("/cart")
  String update(@RequestParam Map<String, String> parameters) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    parameters.forEach((name, value) -> {
      if (name.startsWith(QUANTITY_PREFIX)) {
        quantities.put(name.substring(QUANTITY_PREFIX.length()), quantity(value));
      }
    });
    cart.updateQuantities(quantities);
    return "redirect:/cart";
  }

  /** Legacy: {@code cart.do?action=remove&itemId=...}. */
  @PostMapping("/cart/items/{itemId}/remove")
  String remove(@PathVariable String itemId) {
    cart.remove(itemId);
    return "redirect:/cart";
  }

  /**
   * Anything that is not a whole number counts as zero, which removes the line. Same rule as the
   * legacy {@code CartHTMLAction}.
   */
  static int quantity(String text) {
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException exception) {
      return 0;
    }
  }
}
