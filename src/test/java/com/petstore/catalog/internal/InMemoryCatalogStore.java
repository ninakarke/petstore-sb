package com.petstore.catalog.internal;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * A catalog store that keeps everything in memory, for fast tests of the logic above the port.
 *
 * <p>It is not trusted on its word: it passes the same {@link CatalogStoreContract} as the real
 * adapters (see {@code InMemoryCatalogStoreTests}), so a test that uses it sees the behaviour a
 * database would give.
 */
public class InMemoryCatalogStore implements CatalogStore {

  private record ProductRow(String categoryId, ProductText text) {}

  private record ItemRow(String productId, ItemDetails details) {}

  private final Map<String, CategoryText> categories = new TreeMap<>();
  private final Map<String, ProductRow> products = new TreeMap<>();
  private final Map<String, ItemRow> items = new TreeMap<>();

  @Override
  public Optional<Category> findCategory(String categoryId) {
    return Optional.ofNullable(categories.get(categoryId))
        .map(text -> new Category(categoryId, text.name(), text.description()));
  }

  @Override
  public List<Category> findCategories() {
    return categories.keySet().stream()
        .flatMap(id -> findCategory(id).stream())
        .sorted(Comparator.comparing(Category::name).thenComparing(Category::id))
        .toList();
  }

  @Override
  public Optional<Product> findProduct(String productId) {
    return Optional.ofNullable(products.get(productId))
        .map(row -> new Product(productId, row.text().name(), row.text().description()));
  }

  @Override
  public Page<Product> findProducts(String categoryId, int start, int count) {
    List<Product> all =
        products.entrySet().stream()
            .filter(entry -> entry.getValue().categoryId().equals(categoryId))
            .flatMap(entry -> findProduct(entry.getKey()).stream())
            .sorted(Comparator.comparing(Product::name).thenComparing(Product::id))
            .toList();
    return page(all, start, count);
  }

  @Override
  public Optional<Item> findItem(String itemId) {
    ItemRow row = items.get(itemId);
    if (row == null) {
      return Optional.empty();
    }
    ProductRow product = products.get(row.productId());
    if (product == null) {
      return Optional.empty();
    }
    ItemDetails details = row.details();
    return Optional.of(
        new Item(
            product.categoryId(),
            row.productId(),
            product.text().name(),
            itemId,
            details.image(),
            details.description(),
            details.attributes(),
            details.listPrice(),
            details.unitCost()));
  }

  @Override
  public Page<Item> findItems(String productId, int start, int count) {
    List<Item> all = allItems().filter(item -> item.productId().equals(productId)).toList();
    return page(all, start, count);
  }

  @Override
  public Page<Item> searchItems(Set<String> keywords, int start, int count) {
    List<String> needles =
        keywords.stream().map(keyword -> keyword.toLowerCase(Locale.ROOT)).toList();
    List<Item> all =
        allItems()
            .filter(item ->
                needles.stream()
                    .anyMatch(needle ->
                        contains(item.productName(), needle)
                            || contains(item.categoryId(), needle)
                            || contains(item.description(), needle)))
            .toList();
    return page(all, start, count);
  }

  /** Every item, ordered by item id. */
  private Stream<Item> allItems() {
    return items.keySet().stream().flatMap(id -> findItem(id).stream());
  }

  private static boolean contains(String text, String needle) {
    return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
  }

  private static <T> Page<T> page(List<T> all, int start, int count) {
    if (start >= all.size()) {
      return Page.empty();
    }
    int end = Math.min(start + count, all.size());
    return new Page<>(all.subList(start, end), start, count, end < all.size());
  }

  @Override
  public boolean isEmpty() {
    return categories.isEmpty();
  }

  @Override
  public void saveCategory(String categoryId, CategoryText text) {
    refuseIf(categories.containsKey(categoryId), "Category " + categoryId + " exists");
    categories.put(categoryId, text);
  }

  @Override
  public void saveProduct(String productId, String categoryId, ProductText text) {
    refuseIf(products.containsKey(productId), "Product " + productId + " exists");
    refuseIf(!categories.containsKey(categoryId), "No category " + categoryId);
    products.put(productId, new ProductRow(categoryId, text));
  }

  @Override
  public void saveItem(String itemId, String productId, ItemDetails details) {
    refuseIf(items.containsKey(itemId), "Item " + itemId + " exists");
    refuseIf(!products.containsKey(productId), "No product " + productId);
    items.put(itemId, new ItemRow(productId, details));
  }

  /** What primary and foreign keys do in a database. */
  private static void refuseIf(boolean broken, String message) {
    if (broken) {
      throw new DataIntegrityViolationException(message);
    }
  }

  /** Removes everything. */
  public void clear() {
    categories.clear();
    products.clear();
    items.clear();
  }
}
