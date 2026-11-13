package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import com.petstore.catalog.internal.InMemoryCatalogStore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  private static final String EN = "en_US";
  private static final String JA = "ja_JP";

  private final InMemoryCatalogStore store = new InMemoryCatalogStore();
  private final CatalogService catalog = new CatalogService(store, new CatalogProperties(2, 5));

  @BeforeEach
  void loadFixture() {
    store.saveCategory(
        "FISH",
        Map.of(
            EN, new CategoryText("Fish", null, null), JA, new CategoryText("Sakana", null, null)));
    store.saveProduct(
        "FI-SW-01",
        "FISH",
        Map.of(
            EN, new ProductText("Angelfish", null, "Salt water fish"),
            JA, new ProductText("Enzeru", null, "Kaisui")));
    store.saveProduct("FI-FW-01", "FISH", Map.of(EN, new ProductText("Koi", null, "Fresh water")));
    store.saveItem("EST-1", "FI-SW-01", Map.of(EN, item("16.50"), JA, item("1951")));
    for (int number = 2; number <= 8; number++) {
      store.saveItem("EST-" + number, "FI-SW-01", Map.of(EN, item("10.00")));
    }
    store.saveItem("EST-9", "FI-FW-01", Map.of(EN, item("18.50")));
  }

  @Test
  @DisplayName("legacy: the visitor's locale selects the data")
  void localeSelectsTheData() {
    assertThat(catalog.categories(Locale.US)).extracting(Category::name).containsExactly("Fish");
    assertThat(catalog.categories(Locale.JAPAN))
        .extracting(Category::name)
        .containsExactly("Sakana");
    assertThat(catalog.category("FISH", Locale.JAPAN)).map(Category::name).contains("Sakana");
    assertThat(catalog.product("FI-SW-01", Locale.JAPAN)).map(Product::name).contains("Enzeru");
  }

  @Test
  void localeWithoutACountryIsMatchedByLanguageAndUnknownOnesUseTheDefault() {
    assertThat(catalog.code(Locale.JAPANESE)).isEqualTo(JA);
    assertThat(catalog.code(Locale.GERMANY)).isEqualTo(EN);
    assertThat(catalog.code(null)).isEqualTo(EN);
  }

  @Test
  @DisplayName("legacy: a negative start gives the empty page")
  void negativeStartGivesTheEmptyPage() {
    assertThat(catalog.products("FISH", -1, 2, Locale.US)).isEqualTo(Page.empty());
    assertThat(catalog.items("FI-SW-01", -1, 2, Locale.US)).isEqualTo(Page.empty());
    assertThat(catalog.search("fish", -1, 2, Locale.US)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("legacy: a count below one still returns one result")
  void countBelowOneReturnsOneResult() {
    // The legacy loop was "do { add } while (next() && --count > 0)": it always read one row.
    assertThat(catalog.items("FI-SW-01", 0, 0, Locale.US).items()).hasSize(1);
    assertThat(catalog.products("FISH", 0, -3, Locale.US).items()).hasSize(1);
  }

  @Test
  @DisplayName("new: a requested page size above the maximum is cut down (legacy had no limit)")
  void countAboveTheMaximumIsCutDown() {
    Page<Item> page = catalog.items("FI-SW-01", 0, 1000, Locale.US);

    assertThat(page.items()).hasSize(5);
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("legacy: search text is split on whitespace and any keyword may match")
  void searchTextIsSplitIntoKeywords() {
    // Tabs, newlines and repeated spaces all separate keywords; "unicorn" matches nothing.
    assertThat(catalog.search("  koi \t unicorn\n", 0, 5, Locale.US).items())
        .extracting(Item::itemId)
        .containsExactly("EST-9");

    // Two matching keywords give the union: eight Angelfish items and one Koi item.
    Page<Item> first = catalog.search("angel koi", 0, 5, Locale.US);
    Page<Item> second = catalog.search("angel koi", first.nextStart(), 5, Locale.US);
    assertThat(first.items()).hasSize(5);
    assertThat(first.hasNext()).isTrue();
    assertThat(second.items()).hasSize(4);
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  @DisplayName("legacy: repeating a keyword changes nothing")
  void repeatedKeywordsAreUsedOnce() {
    assertThat(catalog.search("koi koi koi", 0, 5, Locale.US))
        .isEqualTo(catalog.search("koi", 0, 5, Locale.US));
  }

  @Test
  @DisplayName("legacy: a search text without keywords finds nothing")
  void blankSearchFindsNothing() {
    assertThat(catalog.search("", 0, 2, Locale.US)).isEqualTo(Page.empty());
    assertThat(catalog.search("   ", 0, 2, Locale.US)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("fixed: a missing search text finds nothing (legacy threw a NullPointerException)")
  void missingSearchTextFindsNothing() {
    assertThat(catalog.search(null, 0, 2, Locale.US)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName(
      "fixed: an item without details in the visitor's language is shown in the default one")
  void itemFallsBackToTheDefaultLanguage() {
    // EST-2 has English details only. The legacy lookup returned null and the cart then failed.
    Item item = catalog.item("EST-2", Locale.JAPAN).orElseThrow();

    assertThat(item.productName()).isEqualTo("Angelfish");
    assertThat(item.listPrice()).isEqualByComparingTo("10.00");
  }

  @Test
  void itemWithDetailsInTheVisitorsLanguageIsNotReplacedByTheDefault() {
    assertThat(catalog.item("EST-1", Locale.JAPAN).orElseThrow().listPrice())
        .isEqualByComparingTo("1951");
  }

  @Test
  void unknownItemIsAbsentInEveryLanguage() {
    assertThat(catalog.item("EST-404", Locale.JAPAN)).isEmpty();
  }

  @Test
  @DisplayName("legacy: lists show only what exists in the visitor's language")
  void listsDoNotFallBack() {
    assertThat(catalog.items("FI-SW-01", 0, 5, Locale.JAPAN).items())
        .extracting(Item::itemId)
        .containsExactly("EST-1");
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
