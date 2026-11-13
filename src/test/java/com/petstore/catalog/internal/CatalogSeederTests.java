package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.shared.SeedProperties;
import java.util.ArrayList;
import java.util.List;
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
  @DisplayName("legacy seed data: five categories")
  void loadsAllCategories() {
    seed();

    assertThat(store.findCategories())
        .extracting(Category::name)
        .containsExactly("Birds", "Cats", "Dogs", "Fish", "Reptiles");
  }

  @Test
  @DisplayName("legacy seed data: 16 products")
  void loadsAllProducts() {
    seed();

    assertThat(products()).hasSize(16);
    assertThat(store.findProduct("FI-SW-01"))
        .contains(new Product("FI-SW-01", "Angelfish", "Salt Water fish from Australia"));
  }

  @Test
  @DisplayName("legacy seed data: 28 items")
  void loadsAllItems() {
    seed();

    assertThat(items()).hasSize(28);
    assertThat(store.findItem("EST-15")).isPresent();
  }

  @Test
  @DisplayName("legacy: Attribute[0..4] become the item's attributes")
  void mapsItemDetailsAsTheLegacyPopulatorDid() {
    seed();

    Item item = store.findItem("EST-1").orElseThrow();
    assertThat(item.displayName()).isEqualTo("Large Angelfish");
    assertThat(item.attributes()).containsExactly("Large", "Cuddly");
    assertThat(item.imageLocation()).isEqualTo("fish3.gif");
    assertThat(item.description()).isEqualTo("Fresh Water fish from Japan");
    assertThat(item.listPrice()).isEqualByComparingTo("16.50");
    assertThat(item.unitCost()).isEqualByComparingTo("10.00");
    assertThat(item.categoryId()).isEqualTo("FISH");
  }

  @Test
  @DisplayName("the seed file has text in three languages; only the English text is loaded")
  void loadsTheEnglishTextOnly() {
    seed();

    // The Japanese and Chinese prices of EST-1 are 1951 and 142; the English one is 16.50.
    assertThat(store.findItem("EST-1").orElseThrow().listPrice()).isEqualByComparingTo("16.50");
    assertThat(store.findCategory("FISH")).map(Category::name).contains("Fish");
  }

  @Test
  @DisplayName("fixed: a store that already holds data is left alone (legacy dropped the tables)")
  void doesNothingWhenTheStoreAlreadyHoldsData() {
    store.saveCategory("ONLY", new CategoryText("Only", null, null));

    seed();

    assertThat(store.findCategories()).extracting(Category::id).containsExactly("ONLY");
  }

  @Test
  void doesNothingWhenSeedingIsSwitchedOff() {
    new CatalogSeeder(store, new SeedProperties(false, SEED)).run(null);

    assertThat(store.isEmpty()).isTrue();
  }

  private void seed() {
    new CatalogSeeder(store, new SeedProperties(true, SEED)).run(null);
  }

  private List<Product> products() {
    List<Product> all = new ArrayList<>();
    for (Category category : store.findCategories()) {
      all.addAll(store.findProducts(category.id(), 0, 1000).items());
    }
    return all;
  }

  private List<Item> items() {
    List<Item> all = new ArrayList<>();
    for (Product product : products()) {
      all.addAll(store.findItems(product.id(), 0, 1000).items());
    }
    return all;
  }
}
