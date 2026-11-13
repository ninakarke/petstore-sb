package com.petstore.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Whole user journeys through the running application, the way a shopper experiences them: one
 * browser session from the first page to the order confirmation.
 *
 * <p>The slice tests check each part against its legacy source. These tests check that the parts
 * fit together, and they are run unchanged against every database the application supports, by a
 * small subclass each. A journey that passes on one database and fails on another is a migration
 * defect that no slice test would show.
 *
 * <p>Legacy source for the journeys: {@code docs/using.html} of the legacy distribution, which
 * walks through creating a user and placing an order, and the screen flow of {@code
 * WEB-INF/mappings.xml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ShoppingJourneyTests {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private MockMvcTester mvc;
  @Autowired private OrderService orders;

  @Test
  @DisplayName("a new shopper browses, fills a cart, registers at checkout, orders and signs out")
  void newShopperFromFirstPageToOrder() {
    MockHttpSession browser = new MockHttpSession();
    // A user name may be at most 25 characters long (legacy rule), so keep it short.
    String username = "newshopper" + COUNTER.incrementAndGet();

    // Browses the catalog without signing in.
    assertThat(page(browser, "/"))
        .contains("Welcome to the BluePrints Petstore")
        .contains(">Dogs</a>");
    assertThat(page(browser, "/categories/DOGS"))
        .contains("Products for this Category")
        .contains(">Bulldog</a>");
    assertThat(page(browser, "/products/K9-BD-01"))
        .contains("Male Adult Bulldog")
        .contains("$18.50");
    assertThat(page(browser, "/items/EST-6")).contains("List Price:").contains("Your Price:");
    assertThat(page(browser, "/search?keywords=Angel"))
        .contains("Search Results:")
        .contains("Large Angelfish");

    // Fills the cart.
    post(browser, "/cart/items", Map.of("itemId", "EST-6"));
    post(browser, "/cart/items", Map.of("itemId", "EST-1"));
    post(browser, "/cart", Map.of("itemQuantity_EST-1", "3"));
    assertThat(page(browser, "/cart"))
        .contains("Shopping Cart")
        .contains("$68.00")
        .contains(">Check Out</a>");

    // Checkout needs an account: the shopper is sent to sign in, and creates a user instead.
    assertThat(get(browser, "/checkout").getResponse().getRedirectedUrl()).endsWith("/signin");
    assertThat(
            post(
                browser,
                "/register",
                Map.of("j_username", username, "j_password", "secret", "j_password_2", "secret")))
        .hasRedirectedUrl("/account/new");

    // Enters account details and is taken back to the checkout they were on their way to.
    MvcTestResult accountCreated = post(browser, "/account/new", accountDetails());
    assertThat(accountCreated.getResponse().getRedirectedUrl()).contains("/checkout");

    // The order form is prefilled from the new account; the cart survived all of this.
    assertThat(page(browser, "/checkout"))
        .contains("Billing Information")
        .contains("value=\"Duke\"")
        .contains("value=\"Sunville\"");
    assertThat(page(browser, "/cart"))
        .contains("itemQuantity_EST-6")
        .contains("itemQuantity_EST-1");

    // Places the order.
    MvcTestResult placed = post(browser, "/orders", orderDetails());
    String confirmation = placed.getResponse().getRedirectedUrl();
    assertThat(confirmation).matches("/orders/1001\\d+/confirmation");
    String orderId = confirmation.split("/")[2];
    assertThat(page(browser, confirmation))
        .contains("Your Order is Complete")
        .contains("<b>" + orderId + "</b>")
        .contains("<b>duke@example.com</b>");

    // The order is stored, carries the account's card, and has reached order processing.
    Order order = orders.find(username, orderId).orElseThrow();
    assertThat(order.lines()).hasSize(2);
    assertThat(order.totalPrice()).isEqualByComparingTo("68.00");
    assertThat(order.creditCard().cardNumber()).isEqualTo("0100-001-0001");
    assertThat(order.handoff().status()).isEqualTo(HandoffStatus.SENT);

    // Afterwards: empty cart, an account page, personalisation, and nothing ordered twice.
    assertThat(page(browser, "/cart")).contains("Your Shopping Cart is Empty.");
    assertThat(post(browser, "/orders", orderDetails())).hasStatus(HttpStatus.CONFLICT);
    assertThat(page(browser, "/account"))
        .contains("Your Account Information")
        .contains("*********0001")
        .contains("My List")
        .contains(">Bulldog</a>");
    assertThat(orders.ordersOf(username)).hasSize(1);

    // Signs out; the account is closed to the browser again.
    assertThat(post(browser, "/signout", Map.of())).hasRedirectedUrl("/signedout");
    MockHttpSession afterSignOut = new MockHttpSession();
    assertThat(get(afterSignOut, "/account").getResponse().getRedirectedUrl()).endsWith("/signin");

    // Comes back later, signs in with the password chosen, and finds the order through the API.
    assertThat(
            post(afterSignOut, "/signin", Map.of("j_username", username, "j_password", "secret")))
        .hasStatus(HttpStatus.FOUND);
    assertThat(page(afterSignOut, "/account")).contains("<dd>Duke</dd>");
    String token = apiToken(username, "secret");
    assertThat(mvc.get().uri("/api/v1/orders").header("Authorization", "Bearer " + token))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].orderId")
        .isEqualTo(orderId);
  }

  @Test
  @DisplayName("a returning customer signs in, orders from the cart, and the order is delivered")
  void returningCustomerOrders() {
    MockHttpSession browser = new MockHttpSession();
    int before = orders.ordersOf("shopper").size();

    post(browser, "/cart/items", Map.of("itemId", "EST-18"));
    assertThat(post(browser, "/signin", Map.of("j_username", "shopper", "j_password", "j2ee")))
        .hasRedirectedUrl("/");

    // The cart was filled before signing in and is still there.
    assertThat(page(browser, "/cart")).contains("itemQuantity_EST-18").contains("$193.50");
    assertThat(page(browser, "/checkout")).contains("Shipping Information");

    String confirmation = post(browser, "/orders", orderDetails()).getResponse().getRedirectedUrl();
    assertThat(page(browser, confirmation)).contains("Your Order is Complete");

    List<Order> placed = orders.ordersOf("shopper");
    assertThat(placed).hasSize(before + 1);
    assertThat(placed.get(0).totalPrice()).isEqualByComparingTo("193.50");
    assertThat(placed.get(0).handoff().isSent()).isTrue();
  }

  @Test
  @DisplayName("a link to the legacy application still leads somewhere useful")
  void legacyLinkLeadsToTheNewPage() {
    MockHttpSession browser = new MockHttpSession();

    MvcTestResult legacy =
        get(browser, "/petstore/category.screen?category_id=FISH&start=2&count=2");

    assertThat(legacy)
        .hasStatus(HttpStatus.MOVED_PERMANENTLY)
        .hasHeader("Location", "/categories/FISH?start=2");
    assertThat(page(browser, "/categories/FISH?start=2"))
        .contains(">Koi</a>")
        .contains(">Tiger Shark</a>");
  }

  @Test
  @DisplayName("the API on its own: token, catalog, account, an order placed twice by mistake")
  void apiClientOrders() {
    String token = apiToken("j2ee", "j2ee");
    String key = "journey-" + getClass().getSimpleName() + "-" + COUNTER.incrementAndGet();

    assertThat(mvc.get().uri("/api/v1/catalog/search?keywords=parrot"))
        .bodyJson()
        .extractingPath("$.items[0].itemId")
        .isEqualTo("EST-18");
    assertThat(mvc.get().uri("/api/v1/account").header("Authorization", "Bearer " + token))
        .bodyJson()
        .extractingPath("$.userId")
        .isEqualTo("j2ee");

    MvcTestResult first = apiOrder(token, key);
    MvcTestResult retry = apiOrder(token, key);

    assertThat(first).hasStatus(HttpStatus.CREATED);
    assertThat(retry).hasStatus(HttpStatus.OK);
    Object orderId = json(first).get("orderId");
    assertThat(json(retry).get("orderId")).isEqualTo(orderId);
    assertThat(((Map<?, ?>) json(first).get("handoff")).get("status")).isEqualTo("SENT");
  }

  // ---- helpers: what a browser does ------------------------------------------------------------

  private MvcTestResult get(MockHttpSession browser, String url) {
    return mvc.get().uri(java.net.URI.create(url)).session(browser).exchange();
  }

  private String page(MockHttpSession browser, String url) {
    MvcTestResult result = get(browser, url);
    assertThat(result).as("GET %s", url).hasStatusOk();
    try {
      return result.getResponse().getContentAsString();
    } catch (java.io.UnsupportedEncodingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private MvcTestResult post(MockHttpSession browser, String url, Map<String, String> form) {
    MockMvcRequestBuilder request = mvc.post().uri(url).session(browser).with(csrf());
    form.forEach(request::param);
    return request.exchange();
  }

  private String apiToken(String username, String password) {
    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
            .exchange();
    assertThat(result).hasStatusOk();
    return (String) json(result).get("accessToken");
  }

  private MvcTestResult apiOrder(String token, String idempotencyKey) {
    return mvc.post()
        .uri("/api/v1/orders")
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", idempotencyKey)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":2}]}")
        .exchange();
  }

  private static Map<?, ?> json(MvcTestResult result) {
    return (Map<?, ?>) assertThat(result).bodyJson().convertTo(Map.class).actual();
  }

  private static Map<String, String> accountDetails() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("givenName", "Duke");
    form.put("familyName", "BluesPrints");
    form.put("address1", "1234 Moon Way");
    form.put("address2", "");
    form.put("city", "Sunville");
    form.put("state", "California");
    form.put("postalCode", "10001");
    form.put("country", "USA");
    form.put("telephone", "408-555-5555");
    form.put("email", "duke@example.com");
    form.put("cardNumber", "0100-001-0001");
    form.put("cardType", "Duke Express");
    form.put("expiryMonth", "10");
    form.put("expiryYear", "2031");
    form.put("favoriteCategory", "DOGS");
    form.put("myList", "on");
    return form;
  }

  private static Map<String, String> orderDetails() {
    Map<String, String> form = new LinkedHashMap<>();
    for (String block : new String[] {"bill", "ship"}) {
      form.put(block + "GivenName", "Duke");
      form.put(block + "FamilyName", "BluesPrints");
      form.put(block + "Address1", "1234 Moon Way");
      form.put(block + "Address2", "");
      form.put(block + "City", "Sunville");
      form.put(block + "State", "California");
      form.put(block + "PostalCode", "10001");
      form.put(block + "Country", "USA");
      form.put(block + "Telephone", "408-555-5555");
      form.put(block + "Email", "duke@example.com");
    }
    return form;
  }
}
