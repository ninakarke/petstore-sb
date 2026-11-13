package com.petstore.order;

import com.petstore.cart.CartLine;
import com.petstore.cart.CartService;
import com.petstore.cart.CartView;
import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.ContactRules;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerField;
import com.petstore.customer.CustomerService;
import com.petstore.order.internal.OrderIdGenerator;
import com.petstore.order.internal.OrderStore;
import com.petstore.order.internal.handoff.OrderHandoffRelay;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Placing and reading orders.
 *
 * <p>Replaces the legacy {@code OrderEJBAction}. There, placing an order meant: get a number, build
 * a purchase-order document from the cart, send it over JMS, empty the cart. Nothing was stored,
 * and a failure to send was swallowed.
 *
 * <p>Here an order is stored first, with its hand-off to order processing marked as pending, in one
 * atomic step. Delivery is then attempted at once and retried later if it fails. There are two ways
 * in, the checkout page and the JSON API, and both end in the same {@link #store}.
 */
@Service
public class OrderService {

  private final OrderStore orders;
  private final OrderIdGenerator orderIds;
  private final CartService cart;
  private final CatalogService catalog;
  private final CustomerService customers;
  private final OrderHandoffRelay relay;
  private final OrderProperties properties;
  private final Clock clock;

  public OrderService(
      OrderStore orders,
      OrderIdGenerator orderIds,
      CartService cart,
      CatalogService catalog,
      CustomerService customers,
      OrderHandoffRelay relay,
      OrderProperties properties,
      Clock clock) {
    this.orders = orders;
    this.orderIds = orderIds;
    this.cart = cart;
    this.catalog = catalog;
    this.customers = customers;
    this.relay = relay;
    this.properties = properties;
    this.clock = clock;
  }

  // ---- placing an order from the cart (checkout page) ------------------------------------------

  /**
   * Places an order for what is in the visitor's cart and empties the cart.
   *
   * <p>Legacy rules, kept: lines are numbered from zero in cart order; a line's price is the item's
   * list price; the confirmation address is the billing e-mail address; an empty cart is refused.
   *
   * <p>Fixed: the order carries the payment card of the customer's account. The legacy code put the
   * same hard-coded card on every order.
   *
   * @throws EmptyCartException if the cart is empty
   * @throws NoAccountException if the user has not entered account details
   * @throws InvalidOrderException if billing or shipping details are incomplete
   */
  public Order placeOrderFromCart(String userId, ContactInfo billTo, ContactInfo shipTo) {
    CartView view = cart.view();
    if (view.isEmpty()) {
      throw new EmptyCartException();
    }
    List<OrderLine> lines = new ArrayList<>();
    for (CartLine line : view.lines()) {
      lines.add(
          new OrderLine(
              line.categoryId(),
              line.productId(),
              line.itemId(),
              lines.size(),
              line.quantity(),
              line.unitPrice()));
    }
    Order order = store(userId, lines, billTo, shipTo, null, noProblems()).order();
    cart.empty();
    return order;
  }

  // ---- placing an order with explicit lines (JSON API) -----------------------------------------

  /**
   * Places an order for the given items. The legacy application had no such way in.
   *
   * <p>What the cart guarantees on the website has to be checked here: every item must exist in the
   * catalog and every quantity must be between one and the configured maximum. Prices are never
   * accepted from the caller; they are the catalog's list prices. An item listed more than once
   * becomes one line with the quantities added up.
   *
   * <p>Billing and shipping details may be omitted; the contact details of the account are then
   * used, as the checkout page prefills them.
   *
   * @param idempotencyKey if given and the user already placed an order under it, that order is
   *     returned and nothing new is created
   * @throws NoAccountException if the user has not entered account details
   * @throws InvalidOrderException if lines or contact details are not acceptable
   */
  public Placement placeOrder(
      String userId,
      List<RequestedLine> requested,
      ContactInfo billTo,
      ContactInfo shipTo,
      String idempotencyKey) {
    if (idempotencyKey != null) {
      Optional<Order> earlier = orders.findByIdempotencyKey(userId, idempotencyKey);
      if (earlier.isPresent()) {
        return new Placement(earlier.get(), true);
      }
    }
    Map<String, Integer> quantities = new LinkedHashMap<>();
    List<String> invalidQuantities = new ArrayList<>();
    for (RequestedLine line : requested == null ? List.<RequestedLine>of() : requested) {
      if (line.itemId() == null || line.itemId().isBlank()) {
        invalidQuantities.add("");
      } else if (line.quantity() < 1 || line.quantity() > properties.maxQuantity()) {
        invalidQuantities.add(line.itemId());
      } else {
        quantities.merge(line.itemId(), line.quantity(), Integer::sum);
      }
    }
    List<OrderLine> lines = new ArrayList<>();
    List<String> unknownItems = new ArrayList<>();
    quantities.forEach((itemId, quantity) -> {
      Optional<Item> item = catalog.item(itemId);
      if (item.isEmpty()) {
        unknownItems.add(itemId);
      } else if (quantity > properties.maxQuantity()) {
        invalidQuantities.add(itemId);
      } else {
        lines.add(
            new OrderLine(
                item.get().categoryId(),
                item.get().productId(),
                itemId,
                lines.size(),
                quantity,
                item.get().listPrice()));
      }
    });
    boolean noLines = quantities.isEmpty() && invalidQuantities.isEmpty();
    OrderProblems lineProblems =
        new OrderProblems(Set.of(), Set.of(), unknownItems, invalidQuantities, noLines);
    return store(userId, lines, billTo, shipTo, idempotencyKey, lineProblems);
  }

  // ---- reading ---------------------------------------------------------------------------------

  /** An order, if it exists and belongs to the user. */
  public Optional<Order> find(String userId, String orderId) {
    return orders.find(orderId).filter(order -> order.userId().equals(userId));
  }

  /** The user's orders, newest first. */
  public List<Order> ordersOf(String userId) {
    return orders.findByUser(userId);
  }

  // ---- the one place where an order comes into being -------------------------------------------

  private Placement store(
      String userId,
      List<OrderLine> lines,
      ContactInfo billTo,
      ContactInfo shipTo,
      String idempotencyKey,
      OrderProblems lineProblems) {
    Customer customer = customers.find(userId).orElseThrow(() -> new NoAccountException(userId));
    ContactInfo accountContact = customer.account().contactInfo();
    ContactInfo billing = ContactRules.normalise(billTo != null ? billTo : accountContact);
    ContactInfo shipping = ContactRules.normalise(shipTo != null ? shipTo : accountContact);

    Set<CustomerField> billingProblems = ContactRules.problems(billing);
    Set<CustomerField> shippingProblems = ContactRules.problems(shipping);
    OrderProblems problems =
        new OrderProblems(
            billingProblems,
            shippingProblems,
            lineProblems.unknownItems(),
            lineProblems.invalidQuantities(),
            lineProblems.noLines());
    if (!problems.isEmpty()) {
      throw new InvalidOrderException(problems);
    }

    BigDecimal total =
        lines.stream().map(OrderLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    Order order =
        new Order(
            orderIds.nextOrderId(),
            userId,
            billing.email() != null ? billing.email() : accountContact.email(),
            clock.instant().truncatedTo(ChronoUnit.MICROS),
            shipping,
            billing,
            customer.account().creditCard(),
            lines,
            total,
            Handoff.pending(),
            idempotencyKey);
    try {
      orders.save(order);
    } catch (DataIntegrityViolationException duplicate) {
      // Two requests with the same idempotency key arrived together; the other one won.
      if (idempotencyKey != null) {
        Optional<Order> winner = orders.findByIdempotencyKey(userId, idempotencyKey);
        if (winner.isPresent()) {
          return new Placement(winner.get(), true);
        }
      }
      throw duplicate;
    }
    // The order is safe. Try to hand it off now; if that fails it stays pending and is retried.
    Handoff handoff = relay.deliver(order);
    return new Placement(order.withHandoff(handoff), false);
  }

  private static OrderProblems noProblems() {
    return new OrderProblems(Set.of(), Set.of(), List.of(), List.of(), false);
  }
}
