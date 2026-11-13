package com.petstore.catalog;

import com.petstore.catalog.internal.CatalogStore;
import com.petstore.shared.SupportedLocale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * The catalog: categories, products, items and keyword search, in the visitor's language.
 *
 * <p>Replaces the legacy {@code CatalogEJB} session bean and the {@code CatalogHelper} bean that
 * the JSPs used to read the database directly. Pages, JSON endpoints and other modules all go
 * through this one class.
 *
 * <p>The rules here are the ones the legacy code applied above its SQL: how a locale selects the
 * data, how a search text becomes keywords, and what an out-of-range page request returns.
 */
@Service
public class CatalogService {

  private final CatalogStore store;
  private final CatalogProperties properties;

  CatalogService(CatalogStore store, CatalogProperties properties) {
    this.store = store;
    this.properties = properties;
  }

  /** Results per page on the catalog pages. */
  public int pageSize() {
    return properties.pageSize();
  }

  /**
   * All categories, ordered by name. Shown in the sidebar of every page, so the result is cached,
   * as the legacy sidebar was.
   */
  @Cacheable(cacheNames = "catalog.categories", key = "#root.target.code(#locale)")
  public List<Category> categories(Locale locale) {
    return store.findCategories(code(locale));
  }

  public Optional<Category> category(String categoryId, Locale locale) {
    return store.findCategory(categoryId, code(locale));
  }

  public Optional<Product> product(String productId, Locale locale) {
    return store.findProduct(productId, code(locale));
  }

  public Page<Product> products(String categoryId, int start, int count, Locale locale) {
    if (start < 0) {
      return Page.empty();
    }
    return store.findProducts(categoryId, code(locale), start, limit(count));
  }

  /**
   * One item.
   *
   * <p>If the item has no details in the requested language, the details of the default language
   * are returned instead. The legacy code returned {@code null} here, and the cart then failed for
   * any visitor who switched to a language in which one of their items had no details.
   */
  public Optional<Item> item(String itemId, Locale locale) {
    return store
        .findItem(itemId, code(locale))
        .or(() -> store.findItem(itemId, SupportedLocale.DEFAULT.code()));
  }

  public Page<Item> items(String productId, int start, int count, Locale locale) {
    if (start < 0) {
      return Page.empty();
    }
    return store.findItems(productId, code(locale), start, limit(count));
  }

  /**
   * Items matching any keyword of the search text.
   *
   * <p>The text is split on whitespace into keywords, as the legacy DAO did with a {@code
   * StringTokenizer}. A text without keywords finds nothing.
   */
  public Page<Item> search(String text, int start, int count, Locale locale) {
    Set<String> keywords = keywords(text);
    if (keywords.isEmpty() || start < 0) {
      return Page.empty();
    }
    return store.searchItems(keywords, code(locale), start, limit(count));
  }

  /** The code under which data for the visitor's language is stored. */
  public String code(Locale locale) {
    return SupportedLocale.of(locale).code();
  }

  private static Set<String> keywords(String text) {
    Set<String> keywords = new LinkedHashSet<>();
    if (text != null) {
      StringTokenizer tokenizer = new StringTokenizer(text);
      while (tokenizer.hasMoreTokens()) {
        keywords.add(tokenizer.nextToken());
      }
    }
    return keywords;
  }

  /**
   * A count below one is treated as one, which is what the legacy read loop did. A count above the
   * configured maximum is cut down to it.
   */
  private int limit(int count) {
    return Math.min(Math.max(count, 1), properties.maxPageSize());
  }
}
