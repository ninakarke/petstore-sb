package com.petstore.catalog.internal;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import java.math.BigDecimal;
import java.util.List;
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
 * <p>The legacy statements also took a locale. This application is English only, so the port has no
 * such parameter; an adapter for a store that keeps the legacy per-locale layout supplies the
 * English code itself.
 */
public interface CatalogStore {

  // ---- reads -------------------------------------------------------------------------------

  Optional<Category> findCategory(String categoryId);

  /** All categories, ordered by name. */
  List<Category> findCategories();

  Optional<Product> findProduct(String productId);

  /** Products of a category, ordered by name and then id. */
  Page<Product> findProducts(String categoryId, int start, int count);

  Optional<Item> findItem(String itemId);

  /** Items of a product, ordered by item id. */
  Page<Item> findItems(String productId, int start, int count);

  /**
   * Items for which at least one keyword occurs, ignoring case, in the product name, the category
   * id or the item description. Ordered by item id.
   *
   * @param keywords the keywords, taken literally; never empty
   */
  Page<Item> searchItems(Set<String> keywords, int start, int count);

  // ---- writes, used to load reference data ----------------------------------------------------

  boolean isEmpty();

  void saveCategory(String categoryId, CategoryText text);

  void saveProduct(String productId, String categoryId, ProductText text);

  void saveItem(String itemId, String productId, ItemDetails details);

  /** Text of a category. */
  record CategoryText(String name, String image, String description) {}

  /** Text of a product. */
  record ProductText(String name, String image, String description) {}

  /** Text and prices of an item. */
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
