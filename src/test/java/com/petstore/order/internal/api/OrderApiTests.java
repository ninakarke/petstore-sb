package com.petstore.order.internal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.CustomerService;
import com.petstore.order.OrderService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The orders JSON API. Placing an order without the website has no legacy counterpart; the rules
 * the cart enforces on the website have to hold here too.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTests {

  private static final String ORDERS = "/api/v1/orders";
  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private MockMvcTester mvc;
  @Autowired private OrderService orders;
  @Autowired private CustomerService customers;

  @Test
  void ordersNeedABearerToken() {
    assertThat(mvc.get().uri(ORDERS)).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(
            mvc.post()
                .uri(ORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  // ---- placing ---------------------------------------------------------------------------------

  @Test
  @DisplayName("an order is created, priced from the catalog and handed off like a website order")
  void orderIsPlaced() {
    String user = customer();

    MvcTestResult result =
        post(
            user,
            "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":3},{\"itemId\":\"EST-6\",\"quantity\":1}]}",
            null);

    assertThat(result).hasStatus(HttpStatus.CREATED);
    Map<?, ?> order = body(result);
    assertThat(result.getResponse().getHeader("Location"))
        .isEqualTo("/api/v1/orders/" + order.get("orderId"));
    assertThat((String) order.get("orderId")).startsWith("1001");
    assertThat(((Number) order.get("totalPrice")).doubleValue()).isEqualTo(68.0);
    assertThat((List<?>) order.get("lines")).hasSize(2);
    Map<?, ?> firstLine = (Map<?, ?>) ((List<?>) order.get("lines")).get(0);
    assertThat(firstLine.get("lineNumber")).isEqualTo(0);
    assertThat(firstLine.get("itemId")).isEqualTo("EST-1");
    assertThat(firstLine.get("categoryId")).isEqualTo("FISH");
    assertThat(((Number) firstLine.get("unitPrice")).doubleValue()).isEqualTo(16.5);
    assertThat(((Number) firstLine.get("total")).doubleValue()).isEqualTo(49.5);
    assertThat(((Map<?, ?>) order.get("handoff")).get("status")).isEqualTo("SENT");
  }

  @Test
  @DisplayName("a price sent by the caller is ignored; the catalog price applies")
  void callerCannotSetThePrice() {
    MvcTestResult result =
        post(
            customer(),
            "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1,\"unitPrice\":0.01,\"total\":0.01}],"
                + "\"totalPrice\":0.01}",
            null);

    assertThat(result).hasStatus(HttpStatus.CREATED);
    assertThat(((Number) body(result).get("totalPrice")).doubleValue()).isEqualTo(16.5);
  }

  @Test
  @DisplayName("without addresses the contact details of the account are used")
  void addressesDefaultToTheAccount() {
    Map<?, ?> order =
        body(post(customer(), "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null));

    assertThat(((Map<?, ?>) order.get("billTo")).get("givenName")).isEqualTo("XYZ");
    assertThat(((Map<?, ?>) ((Map<?, ?>) order.get("shipTo")).get("address")).get("city"))
        .isEqualTo("Palo Alto");
    assertThat(order.get("email")).isEqualTo("aaa@bbb.ccc");
  }

  @Test
  void givenAddressesAreUsed() {
    String json =
        """
        {
          "lines": [{"itemId": "EST-6", "quantity": 2}],
          "shipTo": {
            "givenName": "Shirley", "familyName": "Receiver",
            "address": {"streetName1": "2 Shipping Lane", "city": "Shipville", "state": "Texas", "zipCode": "70001"},
            "telephone": "222-222-2222"
          }
        }
        """;

    Map<?, ?> order = body(post(customer(), json, null));

    assertThat(((Map<?, ?>) order.get("shipTo")).get("givenName")).isEqualTo("Shirley");
    assertThat(((Map<?, ?>) order.get("billTo")).get("givenName")).isEqualTo("XYZ");
  }

  @Test
  @DisplayName("same rule as the pages: the card number is only ever returned masked")
  void cardIsMasked() {
    MvcTestResult result =
        post(customer(), "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null);

    assertThat(result)
        .bodyText()
        .contains("\"maskedNumber\":\"*****6789\"")
        .doesNotContain("123456789");
  }

  @Test
  @DisplayName("unknown items, bad quantities and incomplete addresses are refused and all named")
  void badRequestsAreRefusedWithEveryProblemNamed() {
    String user = customer();
    int before = orders.ordersOf(user).size();
    String json =
        """
        {
          "lines": [
            {"itemId": "EST-404", "quantity": 1},
            {"itemId": "EST-1", "quantity": 0},
            {"itemId": "EST-6", "quantity": 5000}
          ],
          "billTo": {"givenName": "Bill", "familyName": "Payer", "address": {"streetName1": "1 Road"}}
        }
        """;

    MvcTestResult result = post(user, json, null);

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    Map<?, ?> problems = (Map<?, ?>) body(result).get("problems");
    assertThat(strings(problems.get("unknownItems"))).containsExactly("EST-404");
    assertThat(strings(problems.get("invalidQuantities"))).containsExactly("EST-1", "EST-6");
    assertThat(strings(problems.get("billTo")))
        .containsExactly("CITY", "POSTAL_CODE", "STATE", "TELEPHONE");
    assertThat((List<?>) problems.get("shipTo")).isEmpty();
    assertThat(orders.ordersOf(user)).hasSize(before);
  }

  @Test
  void orderWithoutLinesIsRefused() {
    for (String json : new String[] {"{}", "{\"lines\":[]}"}) {
      MvcTestResult result = post(customer(), json, null);

      assertThat(result).as(json).hasStatus(HttpStatus.BAD_REQUEST);
      assertThat(((Map<?, ?>) body(result).get("problems")).get("noLines")).isEqualTo(true);
    }
  }

  @Test
  @DisplayName("a user who has not entered account details cannot order")
  void userWithoutAccountCannotOrder() {
    assertThat(
            post("api-order-stranger", "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null))
        .hasStatus(HttpStatus.CONFLICT)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  // ---- idempotency -----------------------------------------------------------------------------

  @Test
  @DisplayName(
      "a request repeated with the same Idempotency-Key returns the first order and creates nothing")
  void repeatedRequestDoesNotOrderTwice() {
    String user = customer();
    String key = UUID.randomUUID().toString();
    String json = "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}";

    MvcTestResult first = post(user, json, key);
    MvcTestResult second = post(user, json, key);

    assertThat(first).hasStatus(HttpStatus.CREATED);
    assertThat(second).hasStatus(HttpStatus.OK);
    assertThat(body(second).get("orderId")).isEqualTo(body(first).get("orderId"));
    assertThat(orders.ordersOf(user)).hasSize(1);
  }

  @Test
  void withoutAKeyEveryRequestIsANewOrder() {
    String user = customer();
    String json = "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}";

    post(user, json, null);
    post(user, json, null);

    assertThat(orders.ordersOf(user)).hasSize(2);
  }

  @Test
  void unusableIdempotencyKeyIsABadRequest() {
    String json = "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}";

    assertThat(post(customer(), json, "k".repeat(101))).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(post(customer(), json, " ")).hasStatus(HttpStatus.BAD_REQUEST);
  }

  // ---- reading ---------------------------------------------------------------------------------

  @Test
  void callerListsTheirOwnOrdersNewestFirst() {
    String user = customer();
    Object first =
        body(post(user, "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null))
            .get("orderId");
    Object second =
        body(post(user, "{\"lines\":[{\"itemId\":\"EST-6\",\"quantity\":1}]}", null))
            .get("orderId");
    post(customer(), "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null);

    assertThat(mvc.get().uri(ORDERS).with(tokenOf(user)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].orderId")
        .asArray()
        .containsExactly(second, first);
  }

  @Test
  void callerReadsOneOfTheirOrders() {
    String user = customer();
    Object orderId =
        body(post(user, "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":2}]}", null))
            .get("orderId");

    assertThat(mvc.get().uri(ORDERS + "/" + orderId).with(tokenOf(user)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.lines[0].quantity")
        .isEqualTo(2);
  }

  @Test
  @DisplayName("an order of another user cannot be read")
  void anotherUsersOrderIsNotFound() {
    Object orderId =
        body(post(customer(), "{\"lines\":[{\"itemId\":\"EST-1\",\"quantity\":1}]}", null))
            .get("orderId");

    assertThat(mvc.get().uri(ORDERS + "/" + orderId).with(tokenOf(customer())))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri(ORDERS + "/00000").with(tokenOf(customer())))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void apiDescriptionListsTheOrderEndpoints() {
    assertThat(mvc.get().uri("/v3/api-docs"))
        .bodyText()
        .contains("/api/v1/orders")
        .contains("Idempotency-Key");
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private MvcTestResult post(String user, String json, String idempotencyKey) {
    var request =
        mvc.post()
            .uri(ORDERS)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(tokenOf(user));
    if (idempotencyKey != null) {
      request.header(OrderApiController.IDEMPOTENCY_HEADER, idempotencyKey);
    }
    return request.exchange();
  }

  private static Map<?, ?> body(MvcTestResult result) {
    return (Map<?, ?>) assertThat(result).bodyJson().convertTo(Map.class).actual();
  }

  @SuppressWarnings("unchecked")
  private static List<String> strings(Object jsonArray) {
    return (List<String>) jsonArray;
  }

  /** A valid bearer token belonging to the given user. */
  private static RequestPostProcessor tokenOf(String username) {
    return jwt().jwt(token -> token.subject(username));
  }

  /** A user of its own with account details, so tests do not see each other's orders. */
  private String customer() {
    String username = "orderapi" + COUNTER.incrementAndGet();
    customers.create(username, CustomerFixtures.details());
    return username;
  }
}
