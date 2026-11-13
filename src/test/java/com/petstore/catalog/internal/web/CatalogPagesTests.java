package com.petstore.catalog.internal.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The catalog pages, rendered by the running application from the legacy seed data.
 *
 * <p>Legacy sources: {@code docroot/category.jsp}, {@code product.jsp}, {@code item.jsp}, {@code
 * search.jsp}, {@code sidebar.jsp}, {@code main.jsp} and {@code banner.jsp}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogPagesTests {

  @Autowired private MockMvcTester mvc;

  // ---- category page -------------------------------------------------------------------------

  @Test
  @DisplayName("legacy category.screen: products of the category, ordered by name, two per page")
  void categoryPageListsTheFirstTwoProducts() {
    assertThat(mvc.get().uri("/categories/FISH"))
        .hasStatusOk()
        .hasViewName("catalog/category")
        .bodyText()
        .contains("<title>Items</title>")
        .contains("Products for this Category")
        .contains("href=\"/products/FI-SW-01\">Angelfish</a>")
        .contains("Salt Water fish from Australia")
        .contains("href=\"/products/FI-FW-02\">Goldfish</a>")
        .doesNotContain("Koi")
        .doesNotContain("Tiger Shark");
  }

  @Test
  @DisplayName("legacy: the first page offers Next but not Previous")
  void firstCategoryPageLinksToTheNextPageOnly() {
    assertThat(mvc.get().uri("/categories/FISH"))
        .bodyText()
        .contains("href=\"/categories/FISH?start=2\" rel=\"next\">Next</a>")
        .doesNotContain("Previous");
  }

  @Test
  @DisplayName("legacy: the last page offers Previous but not Next")
  void lastCategoryPageLinksBackOnly() {
    assertThat(mvc.get().uri("/categories/FISH?start=2"))
        .hasStatusOk()
        .bodyText()
        .contains(">Koi</a>")
        .contains(">Tiger Shark</a>")
        .doesNotContain("Angelfish")
        .contains("href=\"/categories/FISH?start=0\" rel=\"prev\">Previous</a>")
        .doesNotContain(">Next</a>");
  }

  @Test
  @DisplayName("changed: an unknown category is a 404 (legacy rendered an empty list)")
  void unknownCategoryIsNotFound() {
    assertThat(mvc.get().uri("/categories/UNICORNS")).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void startThatIsNotANumberIsABadRequest() {
    assertThat(mvc.get().uri("/categories/FISH?start=abc")).hasStatus(HttpStatus.BAD_REQUEST);
  }

  // ---- product page --------------------------------------------------------------------------

  @Test
  @DisplayName("legacy product.screen: items shown as attribute + product name with the list price")
  void productPageListsItemsWithListPrice() {
    assertThat(mvc.get().uri("/products/FI-SW-01"))
        .hasStatusOk()
        .hasViewName("catalog/product")
        .bodyText()
        .contains("<title>Product</title>")
        .contains("Items for this Product")
        .contains("href=\"/items/EST-1\">Large Angelfish</a>")
        .contains("href=\"/items/EST-2\">Small Angelfish</a>")
        .contains("Fresh Water fish from Japan")
        .contains("$16.50");
  }

  @Test
  void unknownProductIsNotFound() {
    assertThat(mvc.get().uri("/products/XX-00")).hasStatus(HttpStatus.NOT_FOUND);
  }

  // ---- item page -----------------------------------------------------------------------------

  @Test
  @DisplayName("legacy item.screen: list price and the customer's price")
  void itemPageShowsBothPrices() {
    assertThat(mvc.get().uri("/items/EST-1"))
        .hasStatusOk()
        .hasViewName("catalog/item")
        .bodyText()
        .contains("<title>Item</title>")
        .contains("<h1>Large Angelfish</h1>")
        .contains("List Price:")
        .contains("$16.50")
        .contains("Your Price:")
        .contains("$10.00");
  }

  @Test
  void unknownItemIsNotFound() {
    assertThat(mvc.get().uri("/items/EST-404")).hasStatus(HttpStatus.NOT_FOUND);
  }

  // ---- search page ---------------------------------------------------------------------------

  @Test
  @DisplayName("legacy search.screen: matches shown with the customer's price, not the list price")
  void searchPageListsMatchesWithUnitCost() {
    assertThat(mvc.get().uri("/search?keywords=angel"))
        .hasStatusOk()
        .hasViewName("catalog/search")
        .bodyText()
        .contains("<title>Search</title>")
        .contains("Search Results:")
        .contains("Items matching any of:")
        .contains("<b>angel</b>")
        .contains("href=\"/items/EST-1\">Large Angelfish</a>")
        .contains("$10.00")
        .doesNotContain("$16.50");
  }

  @Test
  @DisplayName("fixed: a keyword with a capital letter finds the same items")
  void searchIgnoresCase() {
    assertThat(mvc.get().uri("/search?keywords=Angel"))
        .bodyText()
        .contains("Search Results:")
        .contains(">Large Angelfish</a>");
  }

  @Test
  @DisplayName("legacy: search results are paged two at a time and keep the keywords")
  void searchResultsArePaged() {
    assertThat(mvc.get().uri("/search?keywords=fish"))
        .bodyText()
        .contains("href=\"/search?keywords=fish&amp;start=2\" rel=\"next\">Next</a>");
  }

  @Test
  @DisplayName("legacy: an empty search shows the no-results message")
  void emptySearchShowsNoResults() {
    assertThat(mvc.get().uri("/search?keywords="))
        .hasStatusOk()
        .bodyText()
        .contains("No results were found for your search.")
        .doesNotContain("Search Results:");
    assertThat(mvc.get().uri("/search"))
        .bodyText()
        .contains("No results were found for your search.");
  }

  @Test
  void searchWithoutMatchesShowsNoResults() {
    assertThat(mvc.get().uri("/search?keywords=unicorn"))
        .bodyText()
        .contains("No results were found for your search.");
  }

  @Test
  void searchTextIsEscapedWhenShownBack() {
    // "a" matches many items, so the results page, which echoes the keywords, is rendered.
    assertThat(mvc.get().uri("/search").param("keywords", "a <script>alert(1)</script>"))
        .hasStatusOk()
        .bodyText()
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        .doesNotContain("<script>alert(1)</script>");
  }

  // ---- layout --------------------------------------------------------------------------------

  @Test
  @DisplayName("legacy sidebar.jsp: every page lists the categories, ordered by name")
  void sidebarListsTheCategoriesOnEveryPage() {
    for (String url :
        new String[] {"/", "/categories/DOGS", "/items/EST-1", "/search?keywords=x"}) {
      String body = body(url);
      int sidebarStart = body.indexOf("class=\"sidebar\"");
      String sidebar = body.substring(sidebarStart, body.indexOf("</nav>", sidebarStart));

      assertThat(sidebar)
          .as("sidebar of %s", url)
          .contains("Pets")
          .containsSubsequence(
              "href=\"/categories/BIRDS\">Birds</a>",
              "href=\"/categories/CATS\">Cats</a>",
              "href=\"/categories/DOGS\">Dogs</a>",
              "href=\"/categories/FISH\">Fish</a>",
              "href=\"/categories/REPTILES\">Reptiles</a>");
    }
  }

  @Test
  @DisplayName("legacy main.jsp: the home page links to every category")
  void homePageLinksToEveryCategory() {
    assertThat(mvc.get().uri("/"))
        .bodyText()
        .contains("class=\"category-tiles\"")
        .contains("href=\"/categories/REPTILES\">Reptiles</a>");
  }

  @Test
  @DisplayName("legacy banner.jsp: every page has the search box")
  void bannerHasTheSearchBox() {
    assertThat(mvc.get().uri("/"))
        .bodyText()
        .contains("action=\"/search\"")
        .contains("name=\"keywords\"")
        .contains(">Search</button>");
  }

  private String body(String url) {
    try {
      return mvc.get().uri(url).exchange().getResponse().getContentAsString();
    } catch (java.io.UnsupportedEncodingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
