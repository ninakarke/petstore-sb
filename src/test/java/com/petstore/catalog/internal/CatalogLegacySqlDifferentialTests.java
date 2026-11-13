package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
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
 * return the same data, for every category, product and item in every language.
 *
 * <p>Where the new behaviour differs on purpose, the test first shows the legacy behaviour on the
 * legacy SQL and then the new behaviour, so the difference is demonstrated and not just claimed.
 *
 * <p>Subclasses choose the database.
 */
@SpringBootTest
abstract class CatalogLegacySqlDifferentialTests {

  private static final List<String> LOCALES = List.of("en_US", "ja_JP", "zh_CN");

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
  @DisplayName("GET_CATEGORIES: same categories in the same order, in every language")
  void categoryListsMatch() {
    for (String locale : LOCALES) {
      List<Category> expected = legacy.categories(locale);

      assertThat(expected).as("legacy categories in %s", locale).hasSize(5);
      assertThat(store.findCategories(locale))
          .as("categories in %s", locale)
          .containsExactlyElementsOf(expected);
    }
  }

  @Test
  @DisplayName("GET_CATEGORY: same category for every id, in every language")
  void singleCategoriesMatch() {
    for (String locale : LOCALES) {
      for (Category category : legacy.categories(locale)) {
        assertThat(store.findCategory(category.id(), locale).stream().toList())
            .as("category %s in %s", category.id(), locale)
            .containsExactlyElementsOf(legacy.category(category.id(), locale));
      }
    }
  }

  @Test
  @DisplayName("GET_PRODUCTS: same products in the same order, for every category and language")
  void productListsMatch() {
    for (String locale : LOCALES) {
      int total = 0;
      for (Category category : legacy.categories(locale)) {
        List<Product> expected = legacy.products(category.id(), locale);
        total += expected.size();

        assertThat(allPages(start -> store.findProducts(category.id(), locale, start, PAGE_SIZE)))
            .as("products of %s in %s", category.id(), locale)
            .containsExactlyElementsOf(expected);
      }
      assertThat(total).as("legacy products in %s", locale).isEqualTo(16);
    }
  }

  @Test
  @DisplayName("GET_PRODUCT: same product for every id, in every language")
  void singleProductsMatch() {
    for (String locale : LOCALES) {
      for (Product product : allLegacyProducts(locale)) {
        assertThat(store.findProduct(product.id(), locale).stream().toList())
            .as("product %s in %s", product.id(), locale)
            .containsExactlyElementsOf(legacy.product(product.id(), locale));
      }
    }
  }

  @Test
  @DisplayName("GET_ITEMS: same items for every product and language; the new order is by item id")
  void itemListsMatch() {
    for (String locale : LOCALES) {
      int total = 0;
      for (Product product : allLegacyProducts(locale)) {
        List<Item> expected = legacy.items(product.id(), locale);
        total += expected.size();
        List<Item> actual =
            allPages(start -> store.findItems(product.id(), locale, start, PAGE_SIZE));

        // The legacy statement has no ORDER BY, so only the content can be compared.
        assertThat(actual)
            .as("items of %s in %s", product.id(), locale)
            .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
            .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(actual).isSortedAccordingTo(Comparator.comparing(Item::itemId));
      }
      // One item of the seed data has no Japanese details.
      assertThat(total)
          .as("legacy items in %s", locale)
          .isEqualTo("ja_JP".equals(locale) ? 27 : 28);
    }
  }

  @Test
  @DisplayName("GET_ITEM: same item for every id, in every language")
  void singleItemsMatch() {
    for (String locale : LOCALES) {
      for (Product product : allLegacyProducts(locale)) {
        for (Item item : legacy.items(product.id(), locale)) {
          assertThat(store.findItem(item.itemId(), locale).stream().toList())
              .as("item %s in %s", item.itemId(), locale)
              .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
              .containsExactlyElementsOf(legacy.item(item.itemId(), locale));
        }
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
      assertSearchMatchesLegacy("en_US", keywords);
    }
    assertThat(legacy.search("en_US", "fish")).as("legacy matches for 'fish'").isNotEmpty();

    // For the other languages, search for the first character of a product name that exists.
    for (String locale : List.of("ja_JP", "zh_CN")) {
      String keyword = allLegacyProducts(locale).get(0).name().substring(0, 1);

      assertThat(legacy.search(locale, keyword)).as("legacy matches in %s", locale).isNotEmpty();
      assertSearchMatchesLegacy(locale, List.of(keyword));
    }
  }

  @Test
  @DisplayName(
      "SEARCH_ITEMS defect: the legacy SQL finds nothing for 'Dog'; the adapter finds dogs")
  void capitalLetterInKeywordIsADefectInLegacyAndFixedHere() {
    List<Item> legacyLowerCase = legacy.search("en_US", "dog");
    assertThat(legacyLowerCase).isNotEmpty();

    // The defect, reproduced on the legacy SQL: the column is lower-cased, the keyword is not.
    assertThat(legacy.search("en_US", "Dog")).isEmpty();

    // The fix: the same keyword with a capital letter gives what the lower-case keyword gives.
    assertThat(allPages(start -> store.searchItems(Set.of("Dog"), "en_US", start, PAGE_SIZE)))
        .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
        .containsExactlyInAnyOrderElementsOf(legacyLowerCase);
  }

  private void assertSearchMatchesLegacy(String locale, List<String> keywords) {
    List<Item> expected = legacy.search(locale, keywords.toArray(String[]::new));
    List<Item> actual =
        allPages(start -> store.searchItems(Set.copyOf(keywords), locale, start, PAGE_SIZE));

    assertThat(actual)
        .as("search for %s in %s", keywords, locale)
        .usingRecursiveFieldByFieldElementComparator(IGNORING_DECIMAL_SCALE)
        .containsExactlyInAnyOrderElementsOf(expected);
    assertThat(actual).isSortedAccordingTo(Comparator.comparing(Item::itemId));
  }

  private List<Product> allLegacyProducts(String locale) {
    List<Product> products = new ArrayList<>();
    for (Category category : legacy.categories(locale)) {
      products.addAll(legacy.products(category.id(), locale));
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
