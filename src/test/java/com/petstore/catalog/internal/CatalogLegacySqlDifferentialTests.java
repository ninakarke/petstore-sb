package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import com.petstore.shared.StoreLocale;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Differential tests: the new catalog adapter against the legacy SQL.
 *
 * <p>The legacy application cannot be run any more, but its SQL can. These tests start the real
 * application (which loads the legacy seed data into its database), execute every statement of the
 * legacy {@code CatalogDAOSQL.xml} unchanged against that database, and require the new adapter to
 * return the same data, for every category, product and item.
 *
 * <p>The legacy statements take a locale; they are run with the English one, the only one this
 * application loads.
 *
 * <p>Where the new behaviour differs on purpose, the test first shows the legacy behaviour on the
 * legacy SQL and then the new behaviour, so the difference is demonstrated and not just claimed.
 *
 * <p>Subclasses choose the database.
 */
@SpringBootTest
abstract class CatalogLegacySqlDifferentialTests {

  private static final String LOCALE = StoreLocale.CODE;

  /** The page size of the legacy pages; results are read page by page to exercise paging. */
  private static final int PAGE_SIZE = 2;

  private static final RecursiveComparisonConfiguration IGNORING_DECIMAL_SCALE =
      RecursiveComparisonConfiguration.builder()
          .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
          .build();

  @Autowired private CatalogStore store;
  @Autowired private JdbcTemplate jdbc;

  private LegacyCatalogSql legacy;

  @BeforeEach
  void setUp() {
    legacy = new LegacyCatalogSql(jdbc);
  }

  @Test
  @DisplayName("GET_CATEGORIES: same categories in the same order")
  void categoryListsMatch() {
    List<Category> expected = legacy.categories(LOCALE);

    assertThat(expected).as("legacy categories").hasSize(5);
    assertThat(store.findCategories()).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("GET_CATEGORY: same category for every id")
  void singleCategoriesMatch() {
    for (Category category : legacy.categories(LOCALE)) {
      assertThat(store.findCategory(category.id()).stream().toList())
          .as("category %s", category.id())
          .containsExactlyElementsOf(legacy.category(category.id(), LOCALE));
    }
  }

  @Test
  @DisplayName("GET_PRODUCTS: same products in the same order, for every category")
  void productListsMatch() {
    int total = 0;
    for (Category category : legacy.categories(LOCALE)) {
      List<Product> expected = legacy.products(category.id(), LOCALE);
      total += expected.size();

      assertThat(allPages(start -> store.findProducts(category.id(), start, PAGE_SIZE)))
          .as("products of %s", category.id())
          .containsExactlyElementsOf(expected);
    }
    assertThat(total).as("legacy products").isEqualTo(16);
  }

  @Test
  @DisplayName("GET_PRODUCT: same product for every id")
  void singleProductsMatch() {
    for (Product product : allLegacyProducts()) {
      assertThat(store.findProduct(product.id()).stream().toList())
          .as("product %s", product.id())
          .containsExactlyElementsOf(legacy.product(product.id(), LOCALE));
    }
  }

  @Test
  @DisplayName("GET_ITEMS: same items for every product; the new order is by item id")
  void itemListsMatch() {
    int total = 0;
    for (Product product : allLegacyProducts()) {
      List<Item> expected = legacy.items(product.id(), LOCALE);
      total += expected.size();
      List<Item> actual = allPages(start -> store.findItems(product.id(), start, PAGE_SIZE));

      // The legacy statement has no ORDER BY, so only the content can be compared.
      assertThat(actual)
          .as("items of %s", product.id())
          .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
          .containsExactlyInAnyOrderElementsOf(expected);
      assertThat(actual).isSortedAccordingTo(Comparator.comparing(Item::itemId));
    }
    assertThat(total).as("legacy items").isEqualTo(28);
  }

  @Test
  @DisplayName("GET_ITEM: same item for every id")
  void singleItemsMatch() {
    for (Product product : allLegacyProducts()) {
      for (Item item : legacy.items(product.id(), LOCALE)) {
        assertThat(store.findItem(item.itemId()).stream().toList())
            .as("item %s", item.itemId())
            .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
            .containsExactlyElementsOf(legacy.item(item.itemId(), LOCALE));
      }
    }
  }

  @Test
  @DisplayName("SEARCH_ITEMS: same matches for lower-case keywords")
  void searchMatchesForLowerCaseKeywords() {
    List<List<String>> queries =
        List.of(
            List.of("fish"),
            List.of("dog"),
            List.of("angel"),
            List.of("water"),
            List.of("snake"),
            List.of("cat"),
            List.of("dog", "cat"),
            List.of("friendly", "great"),
            List.of("from"),
            List.of("xyzzy"));

    for (List<String> keywords : queries) {
      List<Item> expected = legacy.search(LOCALE, keywords.toArray(String[]::new));
      List<Item> actual =
          allPages(start -> store.searchItems(Set.copyOf(keywords), start, PAGE_SIZE));

      assertThat(actual)
          .as("search for %s", keywords)
          .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
          .containsExactlyInAnyOrderElementsOf(expected);
      assertThat(actual).isSortedAccordingTo(Comparator.comparing(Item::itemId));
    }
    assertThat(legacy.search(LOCALE, "fish")).as("legacy matches for 'fish'").isNotEmpty();
  }

  @Test
  @DisplayName(
      "SEARCH_ITEMS defect: the legacy SQL finds nothing for 'Dog'; the adapter finds dogs")
  void capitalLetterInKeywordIsADefectInLegacyAndFixedHere() {
    List<Item> legacyLowerCase = legacy.search(LOCALE, "dog");
    assertThat(legacyLowerCase).isNotEmpty();

    // The defect, reproduced on the legacy SQL: the column is lower-cased, the keyword is not.
    assertThat(legacy.search(LOCALE, "Dog")).isEmpty();

    // The fix: the same keyword with a capital letter gives what the lower-case keyword gives.
    assertThat(allPages(start -> store.searchItems(Set.of("Dog"), start, PAGE_SIZE)))
        .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
        .containsExactlyInAnyOrderElementsOf(legacyLowerCase);
  }

  @Test
  @DisplayName("only the English rows of the legacy per-locale tables are loaded")
  void onlyEnglishRowsAreLoaded() {
    assertThat(jdbc.queryForList("select distinct locale from item_details", String.class))
        .containsExactly(LOCALE);
    assertThat(legacy.categories("ja_JP")).isEmpty();
  }

  private List<Product> allLegacyProducts() {
    List<Product> products = new ArrayList<>();
    for (Category category : legacy.categories(LOCALE)) {
      products.addAll(legacy.products(category.id(), LOCALE));
    }
    return products;
  }

  /** Reads a paged result to the end, the way a visitor clicking "Next" would. */
  private static <T> List<T> allPages(IntFunction<Page<T>> pageAt) {
    List<T> all = new ArrayList<>();
    Page<T> page = pageAt.apply(0);
    all.addAll(page.items());
    while (page.hasNext()) {
      page = pageAt.apply(page.nextStart());
      all.addAll(page.items());
    }
    return all;
  }
}
