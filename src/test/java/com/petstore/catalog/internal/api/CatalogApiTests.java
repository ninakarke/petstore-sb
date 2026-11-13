package com.petstore.catalog.internal.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The catalog JSON API, served by the running application from the legacy seed data.
 *
 * <p>The API has no legacy counterpart. It must give the same answers as the pages, because both
 * are thin adapters over the same service.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTests {

  private static final String API = "/api/v1/catalog";

  @Autowired private MockMvcTester mvc;

  @Test
  void categoriesAreListedByName() {
    assertThat(mvc.get().uri(API + "/categories"))
        .hasStatusOk()
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .bodyJson()
        .extractingPath("$[*].name")
        .asArray()
        .containsExactly("Birds", "Cats", "Dogs", "Fish", "Reptiles");
  }

  @Test
  @DisplayName("same rule as the pages: two products per page unless a count is given")
  void productsArePagedLikeThePages() {
    assertThat(mvc.get().uri(API + "/categories/FISH/products"))
        .hasStatusOk()
        .bodyJson()
        .convertTo(java.util.Map.class)
        .satisfies(page -> {
          assertThat(page.get("items")).asList().hasSize(2);
          assertThat(page.get("start")).isEqualTo(0);
          assertThat(page.get("count")).isEqualTo(2);
          assertThat(page.get("hasNext")).isEqualTo(true);
          assertThat(page.get("hasPrevious")).isEqualTo(false);
          assertThat(page.get("nextStart")).isEqualTo(2);
        });
  }

  @Test
  void countParameterSetsThePageSize() {
    assertThat(mvc.get().uri(API + "/categories/FISH/products?count=10"))
        .bodyJson()
        .extractingPath("$.items[*].name")
        .asArray()
        .containsExactly("Angelfish", "Goldfish", "Koi", "Tiger Shark");
  }

  @Test
  void pageSizeIsCappedByTheServer() {
    assertThat(mvc.get().uri(API + "/search?keywords=a&count=100000"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.count")
        .isEqualTo(50);
  }

  @Test
  void itemsOfAProductAreListedWithPrices() {
    assertThat(mvc.get().uri(API + "/products/FI-SW-01/items"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items[*].itemId")
        .asArray()
        .containsExactly("EST-1", "EST-2");
  }

  @Test
  void itemCarriesTextAttributesAndPrices() {
    assertThat(mvc.get().uri(API + "/items/EST-1"))
        .hasStatusOk()
        .bodyJson()
        .convertTo(java.util.Map.class)
        .satisfies(item -> {
          assertThat(item.get("itemId")).isEqualTo("EST-1");
          assertThat(item.get("productId")).isEqualTo("FI-SW-01");
          assertThat(item.get("categoryId")).isEqualTo("FISH");
          assertThat(item.get("productName")).isEqualTo("Angelfish");
          assertThat(item.get("attributes")).asList().containsExactly("Large", "Cuddly");
          assertThat(((Number) item.get("listPrice")).doubleValue()).isEqualTo(16.5);
          assertThat(((Number) item.get("unitCost")).doubleValue()).isEqualTo(10.0);
        });
  }

  @Test
  void searchGivesTheSameMatchesAsTheSearchPage() {
    assertThat(mvc.get().uri(API + "/search?keywords=Angel&count=10"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items[*].itemId")
        .asArray()
        .containsExactly("EST-1", "EST-2");
  }

  @Test
  void unknownIdsAreReportedAsProblemDetails() {
    for (String path :
        new String[] {
          "/categories/NOPE",
          "/categories/NOPE/products",
          "/products/NOPE",
          "/products/NOPE/items",
          "/items/NOPE"
        }) {
      assertThat(mvc.get().uri(API + path))
          .as(path)
          .hasStatus(HttpStatus.NOT_FOUND)
          .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }
  }

  @Test
  void apiDescriptionIsGeneratedAndCoversOnlyTheApi() {
    assertThat(mvc.get().uri("/v3/api-docs"))
        .hasStatusOk()
        .bodyText()
        .contains("Pet Store API")
        .contains("/api/v1/catalog/items/{itemId}")
        .contains("/api/v1/catalog/search")
        .doesNotContain("\"/categories/{categoryId}\"");
  }

  @Test
  void interactiveApiPageIsServed() {
    assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
  }
}
