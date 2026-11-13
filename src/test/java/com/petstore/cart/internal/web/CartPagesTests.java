package com.petstore.cart.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * The cart page and its actions, against the running application and the legacy seed data.
 *
 * <p>Legacy sources: {@code docroot/cart.jsp}, {@code CartHTMLAction}, and the "Add to Cart" links
 * of {@code product.jsp}, {@code item.jsp} and {@code search.jsp}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CartPagesTests {

  @Autowired private MockMvcTester mvc;

  /** One browser session per test: the cart lives in it. */
  private MockHttpSession session;

  @BeforeEach
  void newBrowserSession() {
    session = new MockHttpSession();
  }

  @Test
  @DisplayName("legacy cart.jsp: an empty cart says so")
  void emptyCartSaysSo() {
    assertThat(mvc.get().uri("/cart").session(session))
        .hasStatusOk()
        .hasViewName("cart/cart")
        .bodyText()
        .contains("<title>Cart</title>")
        .contains("Your Shopping Cart is Empty.")
        .doesNotContain("Update Cart");
  }

  @Test
  @DisplayName("legacy: adding an item shows it in the cart with quantity one and its list price")
  void addedItemIsShownInTheCart() {
    assertThat(add("EST-1")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/cart");

    assertThat(cartPage())
        .contains("Shopping Cart")
        .contains("href=\"/items/EST-1\">Large Angelfish</a>")
        .contains("name=\"itemQuantity_EST-1\"")
        .contains("value=\"1\"")
        .contains("@ <span>$16.50</span>")
        .contains("Total:")
        .contains("Update Cart")
        .contains(">Remove</button>");
  }

  @Test
  @DisplayName("legacy: adding the same item again leaves the quantity at one")
  void addingTwiceLeavesQuantityAtOne() {
    add("EST-1");
    update("itemQuantity_EST-1", "4");
    add("EST-1");

    assertThat(cartPage()).contains("name=\"itemQuantity_EST-1\"").contains("value=\"1\"");
  }

  @Test
  @DisplayName("legacy: Update Cart sets the quantities and the total follows")
  void updateSetsQuantitiesAndTotal() {
    add("EST-1");
    add("EST-6");

    assertThat(update("itemQuantity_EST-1", "3")).hasRedirectedUrl("/cart");

    // 3 x 16.50 + 1 x 18.50
    assertThat(cartPage()).contains("value=\"3\"").contains("$68.00");
  }

  @Test
  @DisplayName("legacy: a quantity of zero removes the line")
  void zeroQuantityRemovesTheLine() {
    add("EST-1");
    update("itemQuantity_EST-1", "0");

    assertThat(cartPage()).contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("legacy: a quantity that is not a number removes the line")
  void nonNumericQuantityRemovesTheLine() {
    add("EST-1");
    update("itemQuantity_EST-1", "lots");

    assertThat(cartPage()).contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("legacy: Remove takes the line out")
  void removeTakesTheLineOut() {
    add("EST-1");
    add("EST-6");

    assertThat(mvc.post().uri("/cart/items/EST-1/remove").session(session).with(csrf()))
        .hasRedirectedUrl("/cart");

    assertThat(cartPage()).doesNotContain("EST-1").contains("EST-6");
  }

  @Test
  @DisplayName("legacy: each browser session has its own cart")
  void anotherSessionHasItsOwnCart() {
    add("EST-1");

    assertThat(mvc.get().uri("/cart").session(new MockHttpSession()))
        .bodyText()
        .contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("fixed: the cart cannot be changed without a CSRF token (legacy used GET links)")
  void cartCannotBeChangedWithoutCsrfToken() {
    assertThat(mvc.post().uri("/cart/items").param("itemId", "EST-1").session(session))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.post().uri("/cart").param("itemQuantity_EST-1", "9").session(session))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.post().uri("/cart/items/EST-1/remove").session(session))
        .hasStatus(HttpStatus.FORBIDDEN);

    assertThat(cartPage()).contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("fixed: something that is not in the catalog cannot be added")
  void unknownItemCannotBeAdded() {
    assertThat(add("EST-404")).hasStatus(HttpStatus.NOT_FOUND);

    assertThat(cartPage()).contains("Your Shopping Cart is Empty.");
  }

  @Test
  @DisplayName("fixed: an item without Japanese details no longer breaks the cart in Japanese")
  void cartSurvivesAnItemWithoutDetailsInTheVisitorsLanguage() {
    // EST-15 is the one seed item that has no ja_JP details.
    add("EST-15");
    add("EST-1");
    session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.JAPAN);

    assertThat(mvc.get().uri("/cart").session(session))
        .hasStatusOk()
        .bodyText()
        .contains("itemQuantity_EST-15")
        .contains("itemQuantity_EST-1");
  }

  @Test
  @DisplayName("legacy product.jsp, item.jsp, search.jsp: every item offers Add to Cart")
  void catalogPagesOfferAddToCart() {
    for (String url :
        new String[] {"/products/FI-SW-01", "/items/EST-1", "/search?keywords=angel"}) {
      assertThat(mvc.get().uri(url))
          .as(url)
          .bodyText()
          .contains("action=\"/cart/items\" method=\"post\"")
          .contains("name=\"itemId\" value=\"EST-1\"")
          .contains("name=\"_csrf\"")
          .contains(">Add to Cart</button>");
    }
  }

  @Test
  @DisplayName("legacy banner.jsp: every page links to the cart")
  void bannerLinksToTheCart() {
    assertThat(mvc.get().uri("/")).bodyText().contains("href=\"/cart\">Cart</a>");
  }

  @Test
  @DisplayName("legacy CartHTMLAction: anything but a whole number counts as zero")
  void quantityParsing() {
    assertThat(CartPagesController.quantity("7")).isEqualTo(7);
    assertThat(CartPagesController.quantity("-3")).isEqualTo(-3);
    assertThat(CartPagesController.quantity("")).isZero();
    assertThat(CartPagesController.quantity("2.5")).isZero();
    assertThat(CartPagesController.quantity(" 2")).isZero();
    assertThat(CartPagesController.quantity("99999999999")).isZero();
  }

  private org.springframework.test.web.servlet.assertj.MvcTestResult add(String itemId) {
    return mvc.post()
        .uri("/cart/items")
        .param("itemId", itemId)
        .session(session)
        .with(csrf())
        .exchange();
  }

  private org.springframework.test.web.servlet.assertj.MvcTestResult update(
      String field, String value) {
    return mvc.post().uri("/cart").param(field, value).session(session).with(csrf()).exchange();
  }

  private String cartPage() {
    try {
      return mvc.get().uri("/cart").session(session).exchange().getResponse().getContentAsString();
    } catch (java.io.UnsupportedEncodingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
