package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.shared.SeedProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Loading the catalog from the legacy seed file.
 *
 * <p>Legacy sources: {@code apps/petstore/.../tools/populate/PopulateServlet.java} and the
 * populator classes, and {@code docroot/populate/Populate-UTF8.xml}.
 */
class CatalogSeederTests {

  private static final ClassPathResource SEED = new ClassPathResource("seed/Populate-UTF8.xml");

  private final InMemoryCatalogStore store = new InMemoryCatalogStore();

  @Test
  @DisplayName("legacy seed data: five categories in each of the three languages")
  void loadsAllCategoriesInEveryLanguage() {
    seed();

    assertThat(store.findCategories("en_US"))
        .extracting(Category::name)
        .containsExactly("Birds", "Cats", "Dogs", "Fish", "Reptiles");
    assertThat(store.findCategories("ja_JP")).hasSize(5);
    assertThat(store.findCategories("zh_CN")).hasSize(5);
  }

  @Test
  @DisplayName("legacy seed data: 16 products in each of the three languages")
  void loadsAllProductsInEveryLanguage() {
    seed();

    assertThat(products("en_US")).hasSize(16);
    assertThat(products("ja_JP")).hasSize(16);
    assertThat(products("zh_CN")).hasSize(16);
    assertThat(store.findProduct("FI-SW-01", "en_US"))
        .contains(new Product("FI-SW-01", "Angelfish", "Salt Water fish from Australia"));
  }

  @Test
  @DisplayName("legacy seed data: 28 items; one of them has no Japanese details")
  void loadsAllItemsWithTheirGaps() {
    seed();

    assertThat(items("en_US")).hasSize(28);
    assertThat(items("zh_CN")).hasSize(28);
    assertThat(items("ja_JP")).hasSize(27);
    assertThat(store.findItem("EST-15", "en_US")).isPresent();
    assertThat(store.findItem("EST-15", "ja_JP")).isEmpty();
  }

  @Test
  @DisplayName("legacy: Attribute[0..4] become the item's attributes; prices are per language")
  void mapsItemDetailsAsTheLegacyPopulatorDid() {
    seed();

    Item english = store.findItem("EST-1", "en_US").orElseThrow();
    assertThat(english.displayName()).isEqualTo("Large Angelfish");
    assertThat(english.attributes()).containsExactly("Large", "Cuddly");
    assertThat(english.imageLocation()).isEqualTo("fish3.gif");
    assertThat(english.description()).isEqualTo("Fresh Water fish from Japan");
    assertThat(english.listPrice()).isEqualByComparingTo("16.50");
    assertThat(english.unitCost()).isEqualByComparingTo("10.00");
    assertThat(english.categoryId()).isEqualTo("FISH");

    Item japanese = store.findItem("EST-1", "ja_JP").orElseThrow();
    assertThat(japanese.listPrice()).isEqualByComparingTo("1951");
    assertThat(japanese.unitCost()).isEqualByComparingTo("1551");
  }

  @Test
  @DisplayName("fixed: a store that already holds data is left alone (legacy dropped the tables)")
  void doesNothingWhenTheStoreAlreadyHoldsData() {
    store.saveCategory("ONLY", Map.of("en_US", new CategoryText("Only", null, null)));

    seed();

    assertThat(store.findCategories("en_US")).extracting(Category::id).containsExactly("ONLY");
  }

  @Test
  void doesNothingWhenSeedingIsSwitchedOff() {
    new CatalogSeeder(store, new SeedProperties(false, SEED)).run(null);

    assertThat(store.isEmpty()).isTrue();
  }

  private void seed() {
    new CatalogSeeder(store, new SeedProperties(true, SEED)).run(null);
  }

  private List<Product> products(String locale) {
    List<Product> all = new ArrayList<>();
    for (Category category : store.findCategories(locale)) {
      all.addAll(store.findProducts(category.id(), locale, 0, 1000).items());
    }
    return all;
  }

  private List<Item> items(String locale) {
    List<Item> all = new ArrayList<>();
    for (Product product : products(locale)) {
      all.addAll(store.findItems(product.id(), locale, 0, 1000).items());
    }
    return all;
  }
}
