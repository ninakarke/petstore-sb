package com.petstore.catalog.internal;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The persistence port of the catalog: everything the catalog needs from a data store, stated in
 * catalog terms.
 *
 * <p>The read methods correspond one to one to the statements of the legacy {@code
 * CatalogDAOSQL.xml} ({@code GET_CATEGORY}, {@code GET_CATEGORIES}, {@code GET_PRODUCT}, {@code
 * GET_PRODUCTS}, {@code GET_ITEM}, {@code GET_ITEMS}, {@code SEARCH_ITEMS}). Every adapter must
 * pass the same contract test suite, which is what allows the data store to be changed without
 * touching the catalog's logic.
 *
 * <p>Text and prices are stored once per locale, under codes such as {@code en_US}. A read for a
 * locale that has no data returns nothing; falling back to another locale is a decision for the
 * service, not the store.
 */
public interface CatalogStore {

  // ---- reads -------------------------------------------------------------------------------

  Optional<Category> findCategory(String categoryId, String locale);

  /** All categories that have text in the locale, ordered by name. */
  List<Category> findCategories(String locale);

  Optional<Product> findProduct(String productId, String locale);

  /** Products of a category, ordered by name and then id. */
  Page<Product> findProducts(String categoryId, String locale, int start, int count);

  Optional<Item> findItem(String itemId, String locale);

  /** Items of a product, ordered by item id. */
  Page<Item> findItems(String productId, String locale, int start, int count);

  /**
   * Items for which at least one keyword occurs, ignoring case, in the product name, the category
   * id or the item description. Ordered by item id.
   *
   * @param keywords the keywords, taken literally; never empty
   */
  Page<Item> searchItems(Set<String> keywords, String locale, int start, int count);

  // ---- writes, used to load reference data ----------------------------------------------------

  boolean isEmpty();

  void saveCategory(String categoryId, Map<String, CategoryText> textByLocale);

  void saveProduct(String productId, String categoryId, Map<String, ProductText> textByLocale);

  void saveItem(String itemId, String productId, Map<String, ItemDetails> detailsByLocale);

  /** Text of a category in one locale. */
  record CategoryText(String name, String image, String description) {}

  /** Text of a product in one locale. */
  record ProductText(String name, String image, String description) {}

  /** Text and prices of an item in one locale. */
  record ItemDetails(
      BigDecimal listPrice,
      BigDecimal unitCost,
      String image,
      String description,
      List<String> attributes) {

    public ItemDetails {
      attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
  }
}
