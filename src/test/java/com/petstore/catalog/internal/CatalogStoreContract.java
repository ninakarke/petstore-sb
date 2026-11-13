package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The contract every {@link CatalogStore} adapter must fulfil.
 *
 * <p>This suite is written once, against the port, and run unchanged for each data store by a small
 * subclass that supplies the adapter. An adapter that passes it can replace any other adapter
 * without the catalog's logic noticing. That is how the database is changed safely: add an adapter,
 * make this suite pass.
 *
 * <p>The fixture is small and purpose-built so that every rule has an obvious expected result.
 */
public abstract class CatalogStoreContract {

  /** The adapter under test. */
  protected abstract CatalogStore store();

  /** Hook for stores that do not roll back after a test. */
  protected void emptyTheStore() {}

  @BeforeEach
  void loadFixture() {
    emptyTheStore();
    CatalogStore store = store();

    store.saveCategory("FISH", category("Fish"));
    store.saveCategory("DOGS", category("Dogs"));
    store.saveCategory("BIRDS", category("Birds"));

    store.saveProduct("FI-SW-01", "FISH", product("Angelfish", "Salt Water fish from Australia"));
    store.saveProduct("FI-SW-02", "FISH", product("Tiger Shark", "Salt Water fish from Australia"));
    store.saveProduct("FI-FW-01", "FISH", product("Koi", "Fresh Water fish from Japan"));
    store.saveProduct("K9-BD-01", "DOGS", product("Bulldog", "Friendly dog from England"));
    store.saveProduct(
        "AV-CB-01", "BIRDS", product("Amazon Parrot", "Great companion for up to 75 years"));

    store.saveItem(
        "EST-1",
        "FI-SW-01",
        item("16.50", "10.00", "Fresh Water fish from Japan", "Large", "Cuddly"));
    store.saveItem(
        "EST-2", "FI-SW-01", item("16.50", "10.00", "Fresh Water fish from Japan", "Small"));
    store.saveItem("EST-3", "FI-SW-01", item("18.50", "12.00", "Now 50% off", "Toothless"));
    store.saveItem("EST-4", "FI-SW-02", item("18.50", "12.00", "Tagged as big_fish", "Spotted"));
    store.saveItem(
        "EST-6", "K9-BD-01", item("18.50", "12.00", "Friendly dog from England", "Male Adult"));
    store.saveItem(
        "EST-18",
        "AV-CB-01",
        item("193.50", "92.00", "Great companion for up to 75 years", "Adult Male"));
  }

  // ---- categories (legacy GET_CATEGORY, GET_CATEGORIES) --------------------------------------

  @Test
  void categoryIsFoundByItsId() {
    assertThat(store().findCategory("FISH")).contains(new Category("FISH", "Fish", null));
  }

  @Test
  void unknownCategoryIsAbsent() {
    assertThat(store().findCategory("CATS")).isEmpty();
  }

  @Test
  @DisplayName("legacy: categories are ordered by name")
  void categoriesAreOrderedByName() {
    assertThat(store().findCategories())
        .extracting(Category::name)
        .containsExactly("Birds", "Dogs", "Fish");
  }

  // ---- products (legacy GET_PRODUCT, GET_PRODUCTS) -------------------------------------------

  @Test
  void productIsFoundByItsId() {
    assertThat(store().findProduct("FI-SW-01"))
        .contains(new Product("FI-SW-01", "Angelfish", "Salt Water fish from Australia"));
  }

  @Test
  void unknownProductIsAbsent() {
    assertThat(store().findProduct("XX-00")).isEmpty();
  }

  @Test
  @DisplayName("legacy: products of a category are ordered by name")
  void productsOfACategoryAreOrderedByName() {
    Page<Product> page = store().findProducts("FISH", 0, 10);

    assertThat(page.items())
        .extracting(Product::name)
        .containsExactly("Angelfish", "Koi", "Tiger Shark");
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  void productsOfOtherCategoriesAreNotListed() {
    assertThat(store().findProducts("DOGS", 0, 10).items())
        .extracting(Product::id)
        .containsExactly("K9-BD-01");
  }

  @Test
  @DisplayName("legacy: a full page reports that more results follow")
  void firstPageReportsThatMoreFollow() {
    Page<Product> page = store().findProducts("FISH", 0, 2);

    assertThat(page.items()).extracting(Product::name).containsExactly("Angelfish", "Koi");
    assertThat(page.start()).isZero();
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("legacy: the last page reports that nothing follows")
  void lastPageReportsThatNothingFollows() {
    Page<Product> page = store().findProducts("FISH", 2, 2);

    assertThat(page.items()).extracting(Product::name).containsExactly("Tiger Shark");
    assertThat(page.start()).isEqualTo(2);
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  @DisplayName("legacy: a start beyond the last result gives the empty page")
  void startBeyondTheEndGivesTheEmptyPage() {
    assertThat(store().findProducts("FISH", 10, 2)).isEqualTo(Page.empty());
  }

  @Test
  @DisplayName("names are ordered by code point on every store: capitals before lower-case")
  void namesAreOrderedByCodePointOnEveryStore() {
    store().saveCategory("CASE", category("Case"));
    store().saveProduct("CS-01", "CASE", product("albino Finch", "Rare"));
    store().saveProduct("CS-02", "CASE", product("Zebra Finch", "Common"));

    // A language-aware sort would put "albino" first and move both products to other pages.
    assertThat(store().findProducts("CASE", 0, 10).items())
        .extracting(Product::name)
        .containsExactly("Zebra Finch", "albino Finch");
  }

  // ---- items (legacy GET_ITEM, GET_ITEMS) ----------------------------------------------------

  @Test
  void itemCarriesItsProductCategoryTextAndPrices() {
    Item item = store().findItem("EST-1").orElseThrow();

    assertThat(item.itemId()).isEqualTo("EST-1");
    assertThat(item.productId()).isEqualTo("FI-SW-01");
    assertThat(item.categoryId()).isEqualTo("FISH");
    assertThat(item.productName()).isEqualTo("Angelfish");
    assertThat(item.description()).isEqualTo("Fresh Water fish from Japan");
    assertThat(item.imageLocation()).isEqualTo("fish.gif");
    assertThat(item.attributes()).containsExactly("Large", "Cuddly");
    assertThat(item.listPrice()).isEqualByComparingTo("16.50");
    assertThat(item.unitCost()).isEqualByComparingTo("10.00");
  }

  @Test
  void unknownItemIsAbsent() {
    assertThat(store().findItem("EST-99")).isEmpty();
  }

  @Test
  @DisplayName("fixed: items of a product come in a defined order, by item id")
  void itemsOfAProductAreOrderedByItemId() {
    Page<Item> page = store().findItems("FI-SW-01", 0, 10);

    assertThat(page.items()).extracting(Item::itemId).containsExactly("EST-1", "EST-2", "EST-3");
    assertThat(page.items()).extracting(Item::productId).containsOnly("FI-SW-01");
    assertThat(page.items()).extracting(Item::categoryId).containsOnly("FISH");
  }

  @Test
  void itemsArePaged() {
    Page<Item> first = store().findItems("FI-SW-01", 0, 2);
    Page<Item> second = store().findItems("FI-SW-01", first.nextStart(), 2);

    assertThat(first.items()).extracting(Item::itemId).containsExactly("EST-1", "EST-2");
    assertThat(first.hasNext()).isTrue();
    assertThat(second.items()).extracting(Item::itemId).containsExactly("EST-3");
    assertThat(second.hasNext()).isFalse();
  }

  // ---- search (legacy SEARCH_ITEMS) ----------------------------------------------------------

  @Test
  @DisplayName("legacy: a keyword is matched as a substring of the product name")
  void matchesProductName() {
    assertThat(search("angel")).containsExactly("EST-1", "EST-2", "EST-3");
  }

  @Test
  @DisplayName("legacy: a keyword is matched against the category id")
  void matchesCategoryId() {
    assertThat(search("dogs")).containsExactly("EST-6");
  }

  @Test
  @DisplayName("legacy: a keyword is matched against the item description")
  void matchesItemDescription() {
    assertThat(search("companion")).containsExactly("EST-18");
  }

  @Test
  @DisplayName("legacy: an item matches if any one of the keywords matches")
  void matchesAnyKeyword() {
    // Ordered by item id as text, so "EST-18" comes before "EST-6".
    assertThat(search("bulldog", "parrot")).containsExactly("EST-18", "EST-6");
  }

  @Test
  @DisplayName("fixed: a keyword with capital letters matches (legacy found nothing)")
  void ignoresTheCaseOfTheKeyword() {
    assertThat(search("ANGEL")).containsExactly("EST-1", "EST-2", "EST-3");
    assertThat(search("Bulldog")).containsExactly("EST-6");
  }

  @Test
  @DisplayName(
      "fixed: % and _ in a keyword are literal characters (legacy treated them as wildcards)")
  void treatsWildcardCharactersLiterally() {
    assertThat(search("%")).containsExactly("EST-3");
    assertThat(search("_")).containsExactly("EST-4");
  }

  @Test
  void searchResultsArePaged() {
    Page<Item> first = store().searchItems(Set.of("fish"), 0, 2);
    Page<Item> second = store().searchItems(Set.of("fish"), first.nextStart(), 2);

    assertThat(first.items()).extracting(Item::itemId).containsExactly("EST-1", "EST-2");
    assertThat(first.hasNext()).isTrue();
    assertThat(second.items()).extracting(Item::itemId).containsExactly("EST-3", "EST-4");
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  void noMatchGivesTheEmptyPage() {
    assertThat(store().searchItems(Set.of("unicorn"), 0, 10)).isEqualTo(Page.empty());
  }

  private List<String> search(String... keywords) {
    return store().searchItems(Set.of(keywords), 0, 10).items().stream().map(Item::itemId).toList();
  }

  // ---- loading -------------------------------------------------------------------------------

  @Test
  void storeWithDataIsNotEmpty() {
    assertThat(store().isEmpty()).isFalse();
  }

  @Test
  @DisplayName("an id can be used only once")
  void idCanBeUsedOnlyOnce() {
    assertThatThrownBy(() -> store().saveCategory("FISH", category("Fish again")))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> store().saveProduct("FI-SW-01", "FISH", product("Other", "Other")))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> store().saveItem("EST-1", "FI-SW-01", item("1.00", "1.00", "Other")))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(store().findCategory("FISH")).map(Category::name).contains("Fish");
    assertThat(store().findProduct("FI-SW-01")).map(Product::name).contains("Angelfish");
    assertThat(store().findItem("EST-1"))
        .map(Item::description)
        .contains("Fresh Water fish from Japan");
  }

  @Test
  @DisplayName("a product must belong to an existing category, on every store")
  void productOfAnUnknownCategoryIsRefused() {
    assertThatThrownBy(() -> store().saveProduct("XX-01", "NOPE", product("Orphan", "Orphan")))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(store().findProduct("XX-01")).isEmpty();
  }

  @Test
  @DisplayName("an item must belong to an existing product, on every store")
  void itemOfAnUnknownProductIsRefused() {
    assertThatThrownBy(() -> store().saveItem("EST-99", "XX-01", item("1.00", "1.00", "Orphan")))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(store().findItem("EST-99")).isEmpty();
  }

  private static CategoryText category(String name) {
    return new CategoryText(name, "icon.gif", null);
  }

  private static ProductText product(String name, String description) {
    return new ProductText(name, "product.jpg", description);
  }

  private static ItemDetails item(
      String listPrice, String unitCost, String description, String... attributes) {
    return new ItemDetails(
        new BigDecimal(listPrice),
        new BigDecimal(unitCost),
        "fish.gif",
        description,
        List.of(attributes));
  }
}
