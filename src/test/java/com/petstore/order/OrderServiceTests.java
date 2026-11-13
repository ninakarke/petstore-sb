package com.petstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.cart.CartService;
import com.petstore.cart.internal.SessionCart;
import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CustomerDetails;
import com.petstore.customer.CustomerField;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.CustomerService;
import com.petstore.customer.internal.InMemoryCustomerStore;
import com.petstore.order.internal.InMemoryOrderStore;
import com.petstore.order.internal.SequenceOrderIdGenerator;
import com.petstore.order.internal.handoff.OrderHandoffRelay;
import com.petstore.order.internal.handoff.RecordingPublisher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rules for placing an order.
 *
 * <p>Legacy sources: {@code apps/petstore/.../ejb/actions/OrderEJBAction.java} and {@code
 * .../web/actions/OrderHTMLAction.java}.
 */
class OrderServiceTests {

  private static final ContactInfo BILL_TO =
      new ContactInfo(
          "Bill",
          "Payer",
          new Address("1 Billing Road", null, "Billtown", "California", "90001", "USA"),
          "bill@example.com",
          "111-111-1111");
  private static final ContactInfo SHIP_TO =
      new ContactInfo(
          "Shirley",
          "Receiver",
          new Address("2 Shipping Lane", "Dock 4", "Shipville", "Texas", "70001", "USA"),
          "shirley@example.com",
          "222-222-2222");

  private final InMemoryOrderStore store = new InMemoryOrderStore();
  private final RecordingPublisher publisher = new RecordingPublisher();
  private final CatalogService catalog = mock(CatalogService.class);
  private final CartService cart = new CartService(new SessionCart(), catalog);
  private final CustomerService customers = new CustomerService(new InMemoryCustomerStore());
  private final OrderProperties properties =
      new OrderProperties(
          999, new OrderProperties.Handoff(Duration.ofSeconds(30), Duration.ofSeconds(15), 50));
  private final Clock clock = Clock.fixed(OrderFixtures.NOON, ZoneOffset.UTC);
  private final OrderService orders =
      new OrderService(
          store,
          new SequenceOrderIdGenerator(),
          cart,
          catalog,
          customers,
          new OrderHandoffRelay(store, publisher, properties, clock),
          properties,
          clock);

  @BeforeEach
  void setUp() {
    when(catalog.item(anyString())).thenReturn(Optional.empty());
    when(catalog.item("EST-1")).thenReturn(Optional.of(item("FISH", "FI-SW-01", "EST-1", "16.50")));
    when(catalog.item("EST-6")).thenReturn(Optional.of(item("DOGS", "K9-BD-01", "EST-6", "18.50")));
    when(catalog.item("EST-21"))
        .thenReturn(Optional.of(item("FISH", "FI-FW-02", "EST-21", "5.29")));
    customers.create("j2ee", CustomerFixtures.details());
  }

  // ---- from the cart ---------------------------------------------------------------------------

  @Test
  @DisplayName("legacy: the order takes its lines from the cart, numbered from zero, at list price")
  void orderFromCartCarriesTheCartLines() {
    cart.add("EST-1");
    cart.add("EST-6");
    cart.updateQuantities(Map.of("EST-1", 3));

    Order order = orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(order.lines())
        .containsExactly(
            new OrderLine("FISH", "FI-SW-01", "EST-1", 0, 3, new BigDecimal("16.50")),
            new OrderLine("DOGS", "K9-BD-01", "EST-6", 1, 1, new BigDecimal("18.50")));
    assertThat(order.totalPrice()).isEqualByComparingTo("68.00");
  }

  @Test
  @DisplayName("legacy: order number, user, billing e-mail, billing and shipping details")
  void orderCarriesWhoAndWhere() {
    cart.add("EST-1");

    Order order = orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(order.orderId()).isEqualTo("10011");
    assertThat(order.userId()).isEqualTo("j2ee");
    assertThat(order.email()).isEqualTo("bill@example.com");
    assertThat(order.billTo()).isEqualTo(BILL_TO);
    assertThat(order.shipTo()).isEqualTo(SHIP_TO);
    assertThat(order.placedAt()).isEqualTo(OrderFixtures.NOON);
    assertThat(order.idempotencyKey()).isNull();
  }

  @Test
  @DisplayName("legacy: the cart is emptied when the order is placed")
  void cartIsEmptiedAfterOrdering() {
    cart.add("EST-1");

    orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(cart.view().isEmpty()).isTrue();
  }

  @Test
  @DisplayName("legacy: an empty cart cannot be ordered, which also stops a repeated submission")
  void emptyCartIsRefused() {
    assertThatThrownBy(() -> orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO))
        .isInstanceOf(EmptyCartException.class);

    cart.add("EST-1");
    orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);
    assertThatThrownBy(() -> orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO))
        .isInstanceOf(EmptyCartException.class);
    assertThat(store.size()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "fixed: the order carries the customer's own card (legacy hard-coded one card for all)")
  void orderCarriesTheCustomersCard() {
    cart.add("EST-1");

    Order order = orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(order.creditCard()).isEqualTo(CustomerFixtures.creditCard());
    assertThat(order.creditCard().cardNumber()).isNotEqualTo("1234-2334");
  }

  @Test
  @DisplayName("fixed: the total is exact decimal arithmetic (legacy summed floats)")
  void totalIsExact() {
    cart.add("EST-21");
    cart.updateQuantities(Map.of("EST-21", 3));

    assertThat(orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO).totalPrice())
        .isEqualTo(new BigDecimal("15.87"));
  }

  @Test
  void userWithoutAccountDetailsCannotOrder() {
    cart.add("EST-1");

    assertThatThrownBy(() -> orders.placeOrderFromCart("stranger", BILL_TO, SHIP_TO))
        .isInstanceOf(NoAccountException.class);
    assertThat(cart.view().isEmpty()).isFalse();
    assertThat(store.size()).isZero();
  }

  @Test
  @DisplayName("fixed: required billing and shipping fields are enforced on the server")
  void incompleteAddressesAreRefused() {
    cart.add("EST-1");
    ContactInfo noCity =
        new ContactInfo(
            "Bill", "Payer", new Address("1 Road", null, " ", "CA", "9", null), null, "1");
    ContactInfo noPhone =
        new ContactInfo("S", "R", new Address("2 Lane", null, "Town", "TX", "7", null), null, "");

    assertThatThrownBy(() -> orders.placeOrderFromCart("j2ee", noCity, noPhone))
        .isInstanceOfSatisfying(
            InvalidOrderException.class,
            invalid -> {
              assertThat(invalid.problems().billTo()).containsExactly(CustomerField.CITY);
              assertThat(invalid.problems().shipTo()).containsExactly(CustomerField.TELEPHONE);
            });
    // Nothing happened: the visitor can correct the form and try again.
    assertThat(cart.view().isEmpty()).isFalse();
    assertThat(store.size()).isZero();
  }

  @Test
  @DisplayName("legacy: address values are trimmed")
  void addressValuesAreTrimmed() {
    cart.add("EST-1");
    ContactInfo padded =
        new ContactInfo(
            " Bill ",
            " Payer ",
            new Address(" 1 Road ", " ", " Town ", " CA ", " 9 ", " "),
            " ",
            " 1 ");

    Order order = orders.placeOrderFromCart("j2ee", padded, padded);

    assertThat(order.billTo())
        .isEqualTo(
            new ContactInfo(
                "Bill", "Payer", new Address("1 Road", null, "Town", "CA", "9", null), null, "1"));
    // No billing e-mail given: the confirmation goes to the address on the account.
    assertThat(order.email()).isEqualTo("aaa@bbb.ccc");
  }

  // ---- hand-off --------------------------------------------------------------------------------

  @Test
  @DisplayName("the order is stored and handed off to order processing")
  void orderIsStoredAndHandedOff() {
    cart.add("EST-1");

    Order order = orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
    assertThat(order.handoff().status()).isEqualTo(HandoffStatus.SENT);
    assertThat(store.find("10011").orElseThrow().handoff())
        .isEqualTo(new Handoff(HandoffStatus.SENT, 1, OrderFixtures.NOON));
  }

  @Test
  @DisplayName("fixed: if the hand-off fails the order is kept and stays pending (legacy lost it)")
  void failedHandoffKeepsTheOrder() {
    publisher.failFromNowOn();
    cart.add("EST-1");

    Order order = orders.placeOrderFromCart("j2ee", BILL_TO, SHIP_TO);

    assertThat(order.handoff())
        .isEqualTo(new Handoff(HandoffStatus.PENDING, 1, OrderFixtures.NOON));
    assertThat(store.find(order.orderId())).isPresent();
    assertThat(store.findPendingHandoff(10)).extracting(Order::orderId).containsExactly("10011");
    assertThat(cart.view().isEmpty()).isTrue();
  }

  // ---- with explicit lines, as the JSON API does -----------------------------------------------

  @Test
  @DisplayName("API: lines are priced from the catalog; the caller cannot name a price")
  void explicitLinesArePricedFromTheCatalog() {
    Placement placement =
        orders.placeOrder(
            "j2ee",
            List.of(new RequestedLine("EST-6", 2), new RequestedLine("EST-1", 1)),
            BILL_TO,
            SHIP_TO,
            null);

    assertThat(placement.replayed()).isFalse();
    assertThat(placement.order().lines())
        .containsExactly(
            new OrderLine("DOGS", "K9-BD-01", "EST-6", 0, 2, new BigDecimal("18.50")),
            new OrderLine("FISH", "FI-SW-01", "EST-1", 1, 1, new BigDecimal("16.50")));
    assertThat(placement.order().totalPrice()).isEqualByComparingTo("53.50");
    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
  }

  @Test
  @DisplayName("API: without addresses the contact details of the account are used")
  void omittedAddressesDefaultToTheAccount() {
    Order order =
        orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 1)), null, null, null).order();

    assertThat(order.billTo()).isEqualTo(CustomerFixtures.contactInfo());
    assertThat(order.shipTo()).isEqualTo(CustomerFixtures.contactInfo());
    assertThat(order.email()).isEqualTo("aaa@bbb.ccc");
  }

  @Test
  @DisplayName("API: an item listed twice becomes one line")
  void repeatedItemsAreMerged() {
    Order order =
        orders
            .placeOrder(
                "j2ee",
                List.of(
                    new RequestedLine("EST-1", 1),
                    new RequestedLine("EST-6", 1),
                    new RequestedLine("EST-1", 4)),
                null,
                null,
                null)
            .order();

    assertThat(order.lines())
        .extracting(OrderLine::itemId, OrderLine::quantity)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("EST-1", 5),
            org.assertj.core.groups.Tuple.tuple("EST-6", 1));
  }

  @Test
  @DisplayName(
      "API: unknown items, bad quantities and an empty order are refused, all reported at once")
  void badLinesAreRefused() {
    assertThatThrownBy(() ->
            orders.placeOrder(
                "j2ee",
                List.of(
                    new RequestedLine("EST-404", 1),
                    new RequestedLine("EST-1", 0),
                    new RequestedLine("EST-6", -2),
                    new RequestedLine("EST-21", 1000)),
                null,
                null,
                null))
        .isInstanceOfSatisfying(
            InvalidOrderException.class,
            invalid -> {
              assertThat(invalid.problems().unknownItems()).containsExactly("EST-404");
              assertThat(invalid.problems().invalidQuantities())
                  .containsExactly("EST-1", "EST-6", "EST-21");
              assertThat(invalid.problems().noLines()).isFalse();
            });
    assertThatThrownBy(() -> orders.placeOrder("j2ee", List.of(), null, null, null))
        .isInstanceOfSatisfying(
            InvalidOrderException.class,
            invalid -> assertThat(invalid.problems().noLines()).isTrue());
    assertThatThrownBy(() -> orders.placeOrder("j2ee", null, null, null, null))
        .isInstanceOf(InvalidOrderException.class);
    assertThat(store.size()).isZero();
    assertThat(publisher.publishedOrderIds()).isEmpty();
  }

  @Test
  @DisplayName("API: quantities that only together exceed the maximum are refused too")
  void mergedQuantityAboveTheMaximumIsRefused() {
    assertThatThrownBy(() ->
            orders.placeOrder(
                "j2ee",
                List.of(new RequestedLine("EST-1", 600), new RequestedLine("EST-1", 600)),
                null,
                null,
                null))
        .isInstanceOfSatisfying(
            InvalidOrderException.class,
            invalid -> assertThat(invalid.problems().invalidQuantities()).containsExactly("EST-1"));
  }

  @Test
  @DisplayName("API: placing through the API leaves the website cart alone")
  void apiOrderDoesNotTouchTheCart() {
    cart.add("EST-6");

    orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 1)), null, null, null);

    assertThat(cart.view().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("API: repeating a request with the same idempotency key does not order twice")
  void sameIdempotencyKeyReturnsTheSameOrder() {
    List<RequestedLine> lines = List.of(new RequestedLine("EST-1", 1));

    Placement first = orders.placeOrder("j2ee", lines, null, null, "retry-me");
    Placement second = orders.placeOrder("j2ee", lines, null, null, "retry-me");

    assertThat(first.replayed()).isFalse();
    assertThat(second.replayed()).isTrue();
    assertThat(second.order().orderId()).isEqualTo(first.order().orderId());
    assertThat(store.size()).isEqualTo(1);
    assertThat(publisher.publishedOrderIds()).containsExactly("10011");
  }

  @Test
  void differentIdempotencyKeysAreDifferentOrders() {
    List<RequestedLine> lines = List.of(new RequestedLine("EST-1", 1));

    orders.placeOrder("j2ee", lines, null, null, "key-a");
    orders.placeOrder("j2ee", lines, null, null, "key-b");
    orders.placeOrder("j2ee", lines, null, null, null);
    orders.placeOrder("j2ee", lines, null, null, null);

    assertThat(store.size()).isEqualTo(4);
  }

  // ---- reading ---------------------------------------------------------------------------------

  @Test
  void aUserSeesOnlyTheirOwnOrders() {
    customers.create(
        "shopper",
        new CustomerDetails(BILL_TO, CustomerFixtures.creditCard(), CustomerFixtures.profile()));
    orders.placeOrder("j2ee", List.of(new RequestedLine("EST-1", 1)), null, null, null);
    orders.placeOrder("shopper", List.of(new RequestedLine("EST-6", 1)), null, null, null);

    assertThat(orders.ordersOf("j2ee")).extracting(Order::orderId).containsExactly("10011");
    assertThat(orders.find("j2ee", "10011")).isPresent();
    assertThat(orders.find("j2ee", "10012")).isEmpty();
    assertThat(orders.find("shopper", "10012")).isPresent();
    assertThat(orders.find("j2ee", "99999")).isEmpty();
  }

  private static Item item(String categoryId, String productId, String itemId, String listPrice) {
    return new Item(
        categoryId,
        productId,
        "Product",
        itemId,
        "x.gif",
        "An item",
        List.of("Large"),
        new BigDecimal(listPrice),
        new BigDecimal("1.00"));
  }
}
