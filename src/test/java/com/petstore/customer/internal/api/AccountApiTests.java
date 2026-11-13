package com.petstore.customer.internal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.petstore.customer.Customer;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.CustomerService;
import java.util.Map;
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
 * The account JSON API. It has no legacy counterpart and must follow the same rules as the account
 * pages, because both are thin adapters over the same service.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTests {

  private static final String ACCOUNT = "/api/v1/account";
  private static final AtomicInteger COUNTER = new AtomicInteger();

  private static final String VALID_BODY =
      """
      {
        "contact": {
          "givenName": "Duke",
          "familyName": "BluesPrints",
          "address": {
            "streetName1": "1234 Moon Way",
            "city": "Sunville",
            "state": "California",
            "zipCode": "10001",
            "country": "USA"
          },
          "email": "duke@example.com",
          "telephone": "408-555-5555"
        },
        "card": {
          "cardNumber": "0100-001-0001",
          "cardType": "Duke Express",
          "expiryMonth": "10",
          "expiryYear": "2031"
        },
        "profile": { "favoriteCategory": "DOGS", "myList": true, "banners": false }
      }
      """;

  @Autowired private MockMvcTester mvc;
  @Autowired private CustomerService customers;

  @Test
  void accountNeedsABearerToken() {
    assertThat(mvc.get().uri(ACCOUNT)).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(mvc.put().uri(ACCOUNT).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("same data as the account page")
  void callerGetsTheirOwnAccount() {
    assertThat(mvc.get().uri(ACCOUNT).with(tokenOf("j2ee")))
        .hasStatusOk()
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .bodyJson()
        .convertTo(Map.class)
        .satisfies(account -> {
          assertThat(account.get("userId")).isEqualTo("j2ee");
          assertThat(account.get("status")).isEqualTo("active");
          Map<?, ?> contact = (Map<?, ?>) account.get("contact");
          assertThat(contact.get("givenName")).isEqualTo("XYZ");
          assertThat(((Map<?, ?>) contact.get("address")).get("city")).isEqualTo("Palo Alto");
          Map<?, ?> profile = (Map<?, ?>) account.get("profile");
          assertThat(profile.get("favoriteCategory")).isEqualTo("REPTILES");
          assertThat(profile.get("myList")).isEqualTo(true);
          assertThat(profile.get("banners")).isEqualTo(false);
        });
  }

  @Test
  @DisplayName("same rule as the pages: the card number is only ever returned masked")
  void cardNumberIsMasked() {
    assertThat(mvc.get().uri(ACCOUNT).with(tokenOf("j2ee")))
        .bodyText()
        .contains("\"maskedNumber\":\"*****6789\"")
        .doesNotContain("123456789")
        .doesNotContain("cardNumber");
  }

  @Test
  void userWithoutAccountDetailsGetsNotFound() {
    assertThat(mvc.get().uri(ACCOUNT).with(tokenOf(newUser())))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void putCreatesTheAccountOfAUserWhoHasNone() {
    String username = newUser();

    assertThat(put(username, VALID_BODY))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.card.maskedNumber")
        .isEqualTo("*********0001");

    Customer created = customers.find(username).orElseThrow();
    assertThat(created.account().status()).isEqualTo("active");
    assertThat(created.account().contactInfo().givenName()).isEqualTo("Duke");
    assertThat(created.account().creditCard().expiryDate()).isEqualTo("10/2031");
    assertThat(created.profile().favoriteCategory()).isEqualTo("DOGS");
  }

  @Test
  void putReplacesTheDetailsOfAnExistingAccount() {
    String username = existingCustomer();

    assertThat(put(username, VALID_BODY)).hasStatusOk();

    Customer updated = customers.find(username).orElseThrow();
    assertThat(updated.account().contactInfo().address().city()).isEqualTo("Sunville");
    assertThat(updated.account().creditCard().cardNumber()).isEqualTo("0100-001-0001");
  }

  @Test
  @DisplayName("same rule as the pages: an omitted card number keeps the card on file")
  void omittedCardNumberKeepsTheStoredOne() {
    String username = existingCustomer();
    String stored = customers.find(username).orElseThrow().account().creditCard().cardNumber();

    assertThat(put(username, VALID_BODY.replace("\"cardNumber\": \"0100-001-0001\",", "")))
        .hasStatusOk();

    assertThat(customers.find(username).orElseThrow().account().creditCard().cardNumber())
        .isEqualTo(stored);
  }

  @Test
  @DisplayName("same rule as the pages: required fields are enforced and named")
  void missingFieldsAreNamed() {
    String username = newUser();
    String incomplete =
        VALID_BODY
            .replace("\"givenName\": \"Duke\",", "\"givenName\": \" \",")
            .replace("\"city\": \"Sunville\",", "");

    assertThat(put(username, incomplete))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.fields")
        .asArray()
        .containsExactly("CITY", "GIVEN_NAME");
    assertThat(customers.exists(username)).isFalse();
  }

  @Test
  void bodyWithoutItsMainPartsIsABadRequest() {
    assertThat(put(newUser(), "{}")).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(put(newUser(), "{\"contact\": {}, \"card\": {}, \"profile\": {}}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("a token from the token endpoint opens the caller's account, end to end")
  void realTokenOpensTheAccount() {
    MvcTestResult tokenResult =
        mvc.post()
            .uri("/api/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"shopper\",\"password\":\"j2ee\"}")
            .exchange();
    Map<?, ?> token = (Map<?, ?>) assertThat(tokenResult).bodyJson().convertTo(Map.class).actual();

    assertThat(mvc.get().uri(ACCOUNT).header("Authorization", "Bearer " + token.get("accessToken")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.userId")
        .isEqualTo("shopper");
  }

  @Test
  void apiDescriptionListsTheAccountEndpoint() {
    assertThat(mvc.get().uri("/v3/api-docs")).bodyText().contains("/api/v1/account");
  }

  private MvcTestResult put(String username, String body) {
    return mvc.put()
        .uri(ACCOUNT)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .with(tokenOf(username))
        .exchange();
  }

  /** A valid bearer token belonging to the given user. */
  private static RequestPostProcessor tokenOf(String username) {
    return jwt().jwt(token -> token.subject(username));
  }

  private static String newUser() {
    return "apiuser" + COUNTER.incrementAndGet();
  }

  private String existingCustomer() {
    String username = newUser();
    customers.create(username, CustomerFixtures.details());
    return username;
  }
}
