package com.petstore.order.internal.web;

import com.petstore.customer.AddressChoices;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerService;
import com.petstore.order.EmptyCartException;
import com.petstore.order.InvalidOrderException;
import com.petstore.order.NoAccountException;
import com.petstore.order.Order;
import com.petstore.order.OrderProblems;
import com.petstore.order.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checkout: the order form, placing the order, and the confirmation.
 *
 * <p>Legacy equivalents: the {@code enter_order_information} screen, {@code order.do} handled by
 * {@code OrderHTMLAction}, and the screens {@code order_complete} and {@code
 * cart_empty_order_error}.
 *
 * <p>All of it requires a signed-in customer. In the legacy application only the form was
 * protected; {@code order.do} itself could be called by anyone.
 */
@Controller
class CheckoutController {

  private static final String FORM_VIEW = "order/checkout";

  private final OrderService orders;
  private final CustomerService customers;

  CheckoutController(OrderService orders, CustomerService customers) {
    this.orders = orders;
    this.customers = customers;
  }

  /**
   * Legacy: {@code enter_order_information.screen}. Both blocks start with the account's details.
   */
  @GetMapping("/checkout")
  String form(Principal user, Model model) {
    Optional<Customer> customer = customers.find(user.getName());
    if (customer.isEmpty()) {
      return "redirect:/account/new";
    }
    ContactForm prefilled = ContactForm.of(customer.get().account().contactInfo());
    return form(prefilled, prefilled, null, model);
  }

  /** Legacy: {@code order.do}. */
  @PostMapping("/orders")
  String place(
      @RequestParam Map<String, String> parameters,
      Principal user,
      HttpServletResponse response,
      Model model) {
    ContactForm billTo = ContactForm.submitted(parameters, "bill");
    ContactForm shipTo = ContactForm.submitted(parameters, "ship");
    try {
      Order order =
          orders.placeOrderFromCart(user.getName(), billTo.toContactInfo(), shipTo.toContactInfo());
      return "redirect:/orders/" + order.orderId() + "/confirmation";
    } catch (EmptyCartException empty) {
      response.setStatus(HttpStatus.CONFLICT.value());
      return "order/cart-empty";
    } catch (NoAccountException noAccount) {
      return "redirect:/account/new";
    } catch (InvalidOrderException invalid) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      return form(billTo, shipTo, invalid.problems(), model);
    }
  }

  /** Legacy: {@code order_complete.screen}. Only the customer who placed the order can see it. */
  @GetMapping("/orders/{orderId}/confirmation")
  String confirmation(@PathVariable String orderId, Principal user, Model model) {
    Order order =
        orders
            .find(user.getName(), orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("order", order);
    return "order/confirmation";
  }

  private String form(ContactForm billTo, ContactForm shipTo, OrderProblems problems, Model model) {
    model.addAttribute("billTo", billTo);
    model.addAttribute("shipTo", shipTo);
    model.addAttribute("states", AddressChoices.states(billTo.state(), shipTo.state()));
    model.addAttribute("countries", AddressChoices.countries(billTo.country(), shipTo.country()));
    model.addAttribute("problems", problems);
    return FORM_VIEW;
  }
}
