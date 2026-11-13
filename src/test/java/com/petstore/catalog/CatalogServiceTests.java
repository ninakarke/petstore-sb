package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import com.petstore.catalog.internal.InMemoryCatalogStore;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rules the catalog applies above its data store.
 *
 * <p>Legacy sources: {@code components/catalog/.../dao/GenericCatalogDAO.java} (keyword splitting,
 * the read loop, the start check) and {@code components/catalog/.../client/CatalogHelper.java}.
 */
class CatalogServiceTests {

  private final InMemoryCatalogStore store = new InMemoryCatalogStore();
  private final CatalogService catalog = new CatalogService(store, new CatalogProperties(2, 5));

  @BeforeEach
  void loadFixture() {
    store.saveCategory("FISH", new CategoryText("Fish", null, null));
    store.saveProduct("FI-SW-01", "FISH", new ProductText("Angelfish", null, "Salt water fish"));
    store.saveProduct("FI-FW-01", "FISH", new ProductText("Koi", null, "Fresh water"));
    store.saveItem("EST-1", "FI-SW-01", item("16.50"));
    for (int number = 2; number <= 8; number++) {
      store.saveItem("EST-" + number, "FI-SW-01", item("10.00"));
    }
    store.saveItem("EST-9", "FI-FW-01", item("18.50"));
  }

  @Test
  void catalogIsReadThroughTheService() {
    assertThat(catalog.categories()).extracting(Category::name).containsExactly("Fish");
    assertThat(catalog.category("FISH")).map(Category::name).contains("Fish");
    assertThat(catalog.product("FI-SW-01")).map(Product::name).contains("Angelfish");
    assertThat(catalog.item("EST-1")).map(Item::displayName).contains("Large Angelfish");
    assertThat(catalog.item("EST-404")).isEmpty();
  }

  @Test
  @DisplayName("legacy: a negative start gives the empty page")
  void negativeStartGivesTheEmptyPage() {
    assertThat(catalog.products("FISH", -1, 2)).isEqualTo(Page.empty());
    assertThat(catalog.items("FI-SW-01", -1, 2)).isEqualTo(Page.empty());
    assertThat(catalog.search("fish", -1, 2)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("legacy: a count below one still returns one result")
  void countBelowOneReturnsOneResult() {
    // The legacy loop was "do { add } while (next() && --count > 0)": it always read one row.
    assertThat(catalog.items("FI-SW-01", 0, 0).items()).hasSize(1);
    assertThat(catalog.products("FISH", 0, -3).items()).hasSize(1);
  }

  @Test
  @DisplayName("new: a requested page size above the maximum is cut down (legacy had no limit)")
  void countAboveTheMaximumIsCutDown() {
    Page<Item> page = catalog.items("FI-SW-01", 0, 1000);

    assertThat(page.items()).hasSize(5);
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("legacy: search text is split on whitespace and any keyword may match")
  void searchTextIsSplitIntoKeywords() {
    // Tabs, newlines and repeated spaces all separate keywords; "unicorn" matches nothing.
    assertThat(catalog.search("  koi \t unicorn\n", 0, 5).items())
        .extracting(Item::itemId)
        .containsExactly("EST-9");

    // Two matching keywords give the union: eight Angelfish items and one Koi item.
    Page<Item> first = catalog.search("angel koi", 0, 5);
    Page<Item> second = catalog.search("angel koi", first.nextStart(), 5);
    assertThat(first.items()).hasSize(5);
    assertThat(first.hasNext()).isTrue();
    assertThat(second.items()).hasSize(4);
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  @DisplayName("legacy: repeating a keyword changes nothing")
  void repeatedKeywordsAreUsedOnce() {
    assertThat(catalog.search("koi koi koi", 0, 5)).isEqualTo(catalog.search("koi", 0, 5));
  }

  @Test
  @DisplayName("legacy: a search text without keywords finds nothing")
  void blankSearchFindsNothing() {
    assertThat(catalog.search("", 0, 2)).isEqualTo(Page.empty());
    assertThat(catalog.search("   ", 0, 2)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("fixed: a missing search text finds nothing (legacy threw a NullPointerException)")
  void missingSearchTextFindsNothing() {
    assertThat(catalog.search(null, 0, 2)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("legacy: catalog pages show two results")
  void pageSizeComesFromTheSettings() {
    assertThat(catalog.pageSize()).isEqualTo(2);
  }

  private static ItemDetails item(String listPrice) {
    return new ItemDetails(
        new BigDecimal(listPrice), new BigDecimal("1.00"), "fish.gif", "A fish", List.of("Large"));
  }
}
