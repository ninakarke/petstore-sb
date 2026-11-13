package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Item;
import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import com.petstore.shared.CopyReport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Copying a catalog from one store to another, between two in-memory stores. */
class CatalogCopyTests {

  private final InMemoryCatalogStore source = new InMemoryCatalogStore();
  private final InMemoryCatalogStore target = new InMemoryCatalogStore();

  @BeforeEach
  void fillTheSource() {
    source.saveCategory("FISH", new CategoryText("Fish", "fish_icon.gif", null));
    source.saveCategory("DOGS", new CategoryText("Dogs", "dogs_icon.gif", null));
    source.saveProduct("FI-SW-01", "FISH", new ProductText("Angelfish", "fish1.jpg", "Salt water"));
    source.saveProduct("K9-BD-01", "DOGS", new ProductText("Bulldog", "dog2.gif", "Friendly"));
    source.saveItem("EST-1", "FI-SW-01", item("16.50", "Large"));
    source.saveItem("EST-2", "FI-SW-01", item("16.50", "Small"));
    source.saveItem("EST-6", "K9-BD-01", item("18.50", "Male Adult"));
  }

  @Test
  @DisplayName("categories, products and items are copied, parents before children")
  void copiesTheWholeCatalog() {
    List<CopyReport> reports = CatalogCopy.copy(source, target, 2);

    assertThat(reports)
        .extracting(CopyReport::what)
        .containsExactly("categories", "products", "items");
    assertThat(reports).extracting(CopyReport::copied).containsExactly(2L, 2L, 3L);
    assertThat(reports).allMatch(CopyReport::matches);
    assertThat(target.findCategories()).isEqualTo(source.findCategories());
    assertThat(target.findItem("EST-6")).isEqualTo(source.findItem("EST-6"));
    assertThat(target.findItem("EST-6")).map(Item::productName).contains("Bulldog");
  }

  @Test
  @DisplayName("what no page shows is copied too: the pictures of categories and products")
  void copiesWhatNoPageShows() {
    CatalogCopy.copy(source, target, 10);

    assertThat(target.categoriesFrom("DOGS", 1)).isEqualTo(source.categoriesFrom("DOGS", 1));
    assertThat(target.productsFrom("K9-BD-01", 1).get(0).text().image()).isEqualTo("dog2.gif");
  }

  @Test
  void runningAgainCopiesNothing() {
    CatalogCopy.copy(source, target, 2);

    List<CopyReport> again = CatalogCopy.copy(source, target, 2);

    assertThat(again).extracting(CopyReport::copied).containsExactly(0L, 0L, 0L);
    assertThat(again).allMatch(CopyReport::matches);
  }

  @Test
  @DisplayName("a category that already exists with other content is reported, not overwritten")
  void reportsADifference() {
    target.saveCategory("FISH", new CategoryText("Fishes", "other.gif", null));

    List<CopyReport> reports = CatalogCopy.copy(source, target, 10);

    assertThat(reports.get(0).matches()).isFalse();
    assertThat(reports.get(0).examples()).containsExactly("FISH");
    assertThat(target.findCategory("FISH").orElseThrow().name()).isEqualTo("Fishes");
  }

  private static ItemDetails item(String listPrice, String attribute) {
    return new ItemDetails(
        new BigDecimal(listPrice), new BigDecimal("10.00"), "pet.gif", "A pet", List.of(attribute));
  }
}
