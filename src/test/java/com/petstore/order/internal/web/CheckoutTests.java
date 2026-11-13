package com.petstore.order.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.petstore.customer.CreditCard;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderLine;
import com.petstore.order.OrderService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Checkout, against the running application and the legacy demo customers.
 *
 * <p>Legacy sources: {@code docroot/enter_order_information.jsp}, {@code order_completed.jsp},
 * {@code cart_empty_order_error.jsp}, {@code cart.jsp} (the Check Out link), {@code
 * OrderHTMLAction} and {@code OrderEJBAction}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTests {

  @Autowired private MockMvcTester mvc;
  @Autowired private OrderService orders;

  /** One browser session per test: the cart lives in it. */
  private MockHttpSession session;

  @BeforeEach
  void newBrowserSession() {
    session = new MockHttpSession();
  }

  // ---- the order form --------------------------------------------------------------------------

  @Test
  @DisplayName("legacy cart.jsp: a cart with something in it offers Check Out")
  void cartOffersCheckOut() {
    addToCart("EST-1");

    assertThat(mvc.get().uri("/cart").session(session))
        .bodyText()
        .contains("href=\"/checkout\">Check Out</a>");
    assertThat(mvc.get().uri("/cart").session(new MockHttpSession()))
        .bodyText()
        .doesNotContain("Check Out");
  }

  @Test
  @DisplayName(
      "legacy enter_order_information.jsp: billing and shipping, both prefilled from the account")
  void orderFormIsPrefilledFromTheAccount() {
    String body = body(as("j2ee", mvc.get().uri("/checkout")));

    assertThat(body)
        .contains("<title>Enter Order Information</title>")
        .contains("Billing Information")
        .contains("Shipping Information")
        .contains("action=\"/orders\" method=\"post\"")
        .contains("name=\"billGivenName\"")
        .contains("name=\"shipGivenName\"")
        .contains("name=\"billPostalCode\"")
        .contains("name=\"shipTelephone\"")
        .contains(">Zip Code<")
        .contains(">Submit</button>");
    // Seed customer j2ee, in both blocks.
    assertThat(body.split("value=\"XYZ\"", -1)).hasSize(3);
    assertThat(body.split("value=\"1234 Anywhere Street\"", -1)).hasSize(3);
    assertThat(body.split("<option value=\"CA\" selected=\"selected\">CA</option>", -1)).hasSize(3);
  }

  @Test
  @DisplayName("legacy: the order form asks for no card; the order uses the card of the account")
  void orderFormHasNoCardFields() {
    assertThat(body(as("j2ee", mvc.get().uri("/checkout"))))
        .doesNotContain("cardNumber")
        .doesNotContain("123456789");
  }

  @Test
  void userWithoutAccountDetailsIsSentToEnterThem() {
    assertThat(mvc.get().uri("/checkout").with(user("checkout-stranger")))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/account/new");
    addToCart("EST-1");
    assertThat(
            validOrder(mvc.post().uri("/orders"))
                .with(user("checkout-stranger"))
                .with(csrf())
                .session(session))
        .hasRedirectedUrl("/account/new");
  }

  // ---- placing the order -----------------------------------------------------------------------

  @Test
  @DisplayName(
      "legacy order.do: the order is placed and the confirmation shows its number and e-mail")
  void orderIsPlacedAndConfirmed() {
    addToCart("EST-1");
    addToCart("EST-6");
    mvc.post()
        .uri("/cart")
        .param("itemQuantity_EST-1", "3")
        .session(session)
        .with(csrf())
        .exchange();

    MvcTestResult placed = placeOrder("j2ee");

    assertThat(placed).hasStatus(HttpStatus.FOUND);
    String confirmationUrl = placed.getResponse().getRedirectedUrl();
    assertThat(confirmationUrl).matches("/orders/1001\\d+/confirmation");
    String orderId = confirmationUrl.split("/")[2];

    assertThat(as("j2ee", mvc.get().uri(confirmationUrl)))
        .hasStatusOk()
        .hasViewName("order/confirmation")
        .bodyText()
        .contains("<title>Order Placed</title>")
        .contains("Your Order is Complete")
        .contains("Your order Id is")
        .contains("<b>" + orderId + "</b>")
        .contains("You should receive a confirmation e-mail soon at")
        .contains("<b>bill@example.com</b>");

    Order order = orders.find("j2ee", orderId).orElseThrow();
    assertThat(order.lines())
        .containsExactly(
            new OrderLine("FISH", "FI-SW-01", "EST-1", 0, 3, new BigDecimal("16.50")),
            new OrderLine("DOGS", "K9-BD-01", "EST-6", 1, 1, new BigDecimal("18.50")));
    assertThat(order.totalPrice()).isEqualByComparingTo("68.00");
    assertThat(order.billTo().address().city()).isEqualTo("Billtown");
    assertThat(order.shipTo().address().city()).isEqualTo("Shipville");
    assertThat(order.shipTo().address().streetName2()).isEqualTo("Dock 4");
  }

  @Test
  @DisplayName("fixed: the order carries the card of the customer's account, not a hard-coded one")
  void orderCarriesTheAccountsCard() {
    addToCart("EST-1");

    String orderId = placeOrder("j2ee").getResponse().getRedirectedUrl().split("/")[2];

    assertThat(orders.find("j2ee", orderId).orElseThrow().creditCard())
        .isEqualTo(new CreditCard("123456789", "Meow Card", "04/04"));
  }

  @Test
  @DisplayName("the placed order has been handed off to order processing")
  void placedOrderIsHandedOff() {
    addToCart("EST-1");

    String orderId = placeOrder("j2ee").getResponse().getRedirectedUrl().split("/")[2];

    assertThat(orders.find("j2ee", orderId).orElseThrow().handoff().status())
        .isEqualTo(HandoffStatus.SENT);
  }

  @Test
  @DisplayName("legacy: the cart is empty after ordering")
  void cartIsEmptyAfterOrdering() {
    addToCart("EST-1");

    placeOrder("j2ee");

    assertThat(mvc.get().uri("/cart").session(session))
        .bodyText()
        .contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("legacy cart_empty_order_error.jsp: an empty cart cannot be ordered")
  void emptyCartShowsTheOrderErrorPage() {
    assertThat(placeOrder("j2ee"))
        .hasStatus(HttpStatus.CONFLICT)
        .hasViewName("order/cart-empty")
        .bodyText()
        .contains("<title>Cart Empty Error</title>")
        .contains("Order Error")
        .contains("The Shopping Cart is Empty and the order could not be placed.")
        .contains("re-submitting the same order");
  }

  @Test
  @DisplayName("legacy: submitting the same order form twice places one order")
  void resubmittingPlacesNoSecondOrder() {
    addToCart("EST-1");
    int before = orders.ordersOf("j2ee").size();

    placeOrder("j2ee");
    MvcTestResult second = placeOrder("j2ee");

    assertThat(second).hasStatus(HttpStatus.CONFLICT).hasViewName("order/cart-empty");
    assertThat(orders.ordersOf("j2ee")).hasSize(before + 1);
  }

  @Test
  @DisplayName("fixed: missing address fields are reported on the form and nothing is ordered")
  void missingFieldsAreReported() {
    addToCart("EST-1");
    int before = orders.ordersOf("j2ee").size();

    MvcTestResult result =
        order(mvc.post().uri("/orders"), Map.of("billCity", " ", "shipTelephone", ""))
            .with(user("j2ee"))
            .with(csrf())
            .session(session)
            .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasViewName("order/checkout")
        .bodyText()
        .contains("Please complete the following:")
        .containsSubsequence("<span>Billing Information</span>:", "<span>City</span>")
        .containsSubsequence("<span>Shipping Information</span>:", "<span>Telephone Number</span>")
        .contains("value=\"Shipville\"");
    assertThat(orders.ordersOf("j2ee")).hasSize(before);
    assertThat(mvc.get().uri("/cart").session(session)).bodyText().contains("itemQuantity_EST-1");
  }

  // ---- protection ------------------------------------------------------------------------------

  @Test
  @DisplayName("fixed: placing an order needs a signed-in customer and a CSRF token")
  void orderingIsProtected() {
    addToCart("EST-1");

    MvcTestResult anonymous =
        validOrder(mvc.post().uri("/orders")).with(csrf()).session(session).exchange();
    assertThat(anonymous.getResponse().getRedirectedUrl()).endsWith("/signin");

    assertThat(validOrder(mvc.post().uri("/orders")).with(user("j2ee")).session(session))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void confirmationIsOnlyForTheCustomerWhoOrdered() {
    addToCart("EST-1");
    String confirmationUrl = placeOrder("j2ee").getResponse().getRedirectedUrl();

    assertThat(mvc.get().uri(confirmationUrl).with(user("shopper")))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/orders/99999999/confirmation").with(user("j2ee")))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private void addToCart(String itemId) {
    mvc.post().uri("/cart/items").param("itemId", itemId).session(session).with(csrf()).exchange();
  }

  private MvcTestResult placeOrder(String username) {
    return validOrder(mvc.post().uri("/orders"))
        .with(user(username))
        .with(csrf())
        .session(session)
        .exchange();
  }

  private MvcTestResult as(String username, MockMvcRequestBuilder request) {
    return request.with(user(username)).session(session).exchange();
  }

  private static String body(MvcTestResult result) {
    try {
      return result.getResponse().getContentAsString();
    } catch (java.io.UnsupportedEncodingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static MockMvcRequestBuilder validOrder(MockMvcRequestBuilder request) {
    return order(request, Map.of());
  }

  /** A complete order form, with some fields replaced. */
  private static MockMvcRequestBuilder order(
      MockMvcRequestBuilder request, Map<String, String> replacements) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("billGivenName", "Bill");
    fields.put("billFamilyName", "Payer");
    fields.put("billAddress1", "1 Billing Road");
    fields.put("billAddress2", "");
    fields.put("billCity", "Billtown");
    fields.put("billState", "California");
    fields.put("billPostalCode", "90001");
    fields.put("billCountry", "USA");
    fields.put("billTelephone", "111-111-1111");
    fields.put("billEmail", "bill@example.com");
    fields.put("shipGivenName", "Shirley");
    fields.put("shipFamilyName", "Receiver");
    fields.put("shipAddress1", "2 Shipping Lane");
    fields.put("shipAddress2", "Dock 4");
    fields.put("shipCity", "Shipville");
    fields.put("shipState", "Texas");
    fields.put("shipPostalCode", "70001");
    fields.put("shipCountry", "USA");
    fields.put("shipTelephone", "222-222-2222");
    fields.put("shipEmail", "shirley@example.com");
    fields.putAll(replacements);
    fields.forEach(request::param);
    return request;
  }
}
