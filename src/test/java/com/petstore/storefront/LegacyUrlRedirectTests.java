package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Links to the legacy application keep working.
 *
 * <p>Legacy sources: the screen names of {@code WEB-INF/screendefinitions_en_US.xml}, the links
 * written by the legacy JSPs, and the context root {@code petstore} of {@code application.xml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LegacyUrlRedirectTests {

  @Autowired private MockMvcTester mvc;

  @ParameterizedTest(name = "{0}  ->  {1}")
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          /main.screen                                          | /
          /index.jsp                                            | /
          /petstore                                             | /
          /petstore/                                            | /
          /petstore/index.jsp                                   | /
          /petstore/main.screen                                 | /
          /category.screen?category_id=DOGS                     | /categories/DOGS
          /petstore/category.screen?category_id=DOGS            | /categories/DOGS
          /category.screen?category_id=FISH&start=2&count=2     | /categories/FISH?start=2
          /category.screen?category_id=FISH&start=0&count=2     | /categories/FISH
          /product.screen?product_id=FI-SW-01                   | /products/FI-SW-01
          /product.screen?product_id=FI-SW-01&start=2&count=2   | /products/FI-SW-01?start=2
          /item.screen?item_id=EST-1                            | /items/EST-1
          /search.screen?keywords=dog                           | /search?keywords=dog
          /search.screen?keywords=great%20dog&start=2&count=2   | /search?keywords=great%20dog&start=2
          /search.screen                                        | /search?keywords=
          /cart.screen                                          | /cart
          /cart.do                                              | /cart
          /petstore/cart.do                                     | /cart
          /cart.do?action=purchase&itemId=EST-1                 | /items/EST-1
          /cart.do?action=remove&itemId=EST-1                   | /cart
          /cart.do?action=update&itemQuantity_EST-1=9           | /cart
          """)
  @DisplayName("legacy address is permanently redirected to its replacement")
  void legacyAddressIsRedirected(String legacy, String replacement) {
    assertThat(mvc.get().uri(java.net.URI.create(legacy)))
        .hasStatus(HttpStatus.MOVED_PERMANENTLY)
        .hasHeader("Location", replacement);
  }

  @Test
  @DisplayName("fixed: fetching a legacy cart address does not change the cart")
  void legacyCartAddressDoesNotChangeTheCart() {
    org.springframework.mock.web.MockHttpSession session =
        new org.springframework.mock.web.MockHttpSession();

    mvc.get().uri("/cart.do?action=purchase&itemId=EST-1").session(session).exchange();

    assertThat(mvc.get().uri("/cart").session(session))
        .bodyText()
        .contains("Your Shopping Cart is Empty.");
  }

  @Test
  void redirectTargetIsEncoded() {
    assertThat(mvc.get().uri("/item.screen").param("item_id", "a b/c"))
        .hasStatus(HttpStatus.MOVED_PERMANENTLY)
        .hasHeader("Location", "/items/a%20b%2Fc");
  }

  @Test
  void startThatIsNotANumberIsDropped() {
    assertThat(mvc.get().uri("/category.screen?category_id=DOGS&start=abc"))
        .hasHeader("Location", "/categories/DOGS");
  }

  @Test
  void legacyAddressWithoutItsSubjectIsNotFound() {
    assertThat(mvc.get().uri("/category.screen")).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/item.screen?item_id=")).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void unknownLegacyScreenIsNotFound() {
    assertThat(mvc.get().uri("/no_such_thing.screen")).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("a redirected legacy address leads to a page that exists")
  void redirectTargetExists() {
    assertThat(mvc.get().uri("/categories/DOGS")).hasStatusOk();
    assertThat(mvc.get().uri("/items/EST-1")).hasStatusOk();
  }
}
