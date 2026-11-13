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

/**
 * A catalog store that keeps everything in memory, for fast tests of the logic above the port.
 *
 * <p>It is not trusted on its word: it passes the same {@link CatalogStoreContract} as the real
 * adapters (see {@code InMemoryCatalogStoreTests}), so a test that uses it sees the behaviour a
 * database would give.
 */
public class InMemoryCatalogStore implements CatalogStore {

  private record ProductRow(String categoryId, Map<String, ProductText> text) {}

  private record ItemRow(String productId, Map<String, ItemDetails> details) {}

  private final Map<String, Map<String, CategoryText>> categories = new TreeMap<>();
  private final Map<String, ProductRow> products = new TreeMap<>();
  private final Map<String, ItemRow> items = new TreeMap<>();

  @Override
  public Optional<Category> findCategory(String categoryId, String locale) {
    return Optional.ofNullable(categories.get(categoryId))
        .map(text -> text.get(locale))
        .map(text -> new Category(categoryId, text.name(), text.description()));
  }

  @Override
  public List<Category> findCategories(String locale) {
    return categories.keySet().stream()
        .flatMap(id -> findCategory(id, locale).stream())
        .sorted(Comparator.comparing(Category::name).thenComparing(Category::id))
        .toList();
  }

  @Override
  public Optional<Product> findProduct(String productId, String locale) {
    return Optional.ofNullable(products.get(productId))
        .map(row -> row.text().get(locale))
        .map(text -> new Product(productId, text.name(), text.description()));
  }

  @Override
  public Page<Product> findProducts(String categoryId, String locale, int start, int count) {
    List<Product> all =
        products.entrySet().stream()
            .filter(entry -> entry.getValue().categoryId().equals(categoryId))
            .flatMap(entry -> findProduct(entry.getKey(), locale).stream())
            .sorted(Comparator.comparing(Product::name).thenComparing(Product::id))
            .toList();
    return page(all, start, count);
  }

  @Override
  public Optional<Item> findItem(String itemId, String locale) {
    ItemRow row = items.get(itemId);
    if (row == null) {
      return Optional.empty();
    }
    ItemDetails details = row.details().get(locale);
    ProductRow product = products.get(row.productId());
    if (details == null || product == null || !product.text().containsKey(locale)) {
      return Optional.empty();
    }
    return Optional.of(
        new Item(
            product.categoryId(),
            row.productId(),
            product.text().get(locale).name(),
            itemId,
            details.image(),
            details.description(),
            details.attributes(),
            details.listPrice(),
            details.unitCost()));
  }

  @Override
  public Page<Item> findItems(String productId, String locale, int start, int count) {
    List<Item> all = itemsIn(locale).filter(item -> item.productId().equals(productId)).toList();
    return page(all, start, count);
  }

  @Override
  public Page<Item> searchItems(Set<String> keywords, String locale, int start, int count) {
    List<String> needles =
        keywords.stream().map(keyword -> keyword.toLowerCase(Locale.ROOT)).toList();
    List<Item> all =
        itemsIn(locale)
            .filter(item ->
                needles.stream()
                    .anyMatch(needle ->
                        contains(item.productName(), needle)
                            || contains(item.categoryId(), needle)
                            || contains(item.description(), needle)))
            .toList();
    return page(all, start, count);
  }

  /** Every item that has details in the locale, ordered by item id. */
  private Stream<Item> itemsIn(String locale) {
    return items.keySet().stream().flatMap(id -> findItem(id, locale).stream());
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
  public void saveCategory(String categoryId, Map<String, CategoryText> textByLocale) {
    categories.put(categoryId, Map.copyOf(textByLocale));
  }

  @Override
  public void saveProduct(
      String productId, String categoryId, Map<String, ProductText> textByLocale) {
    products.put(productId, new ProductRow(categoryId, Map.copyOf(textByLocale)));
  }

  @Override
  public void saveItem(String itemId, String productId, Map<String, ItemDetails> detailsByLocale) {
    items.put(itemId, new ItemRow(productId, Map.copyOf(detailsByLocale)));
  }

  /** Removes everything. */
  public void clear() {
    categories.clear();
    products.clear();
    items.clear();
  }
}
