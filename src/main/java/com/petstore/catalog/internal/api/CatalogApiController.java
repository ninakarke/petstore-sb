package com.petstore.catalog.internal.api;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.shared.SupportedLocale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The catalog as JSON: the same data and rules as the catalog pages, for clients other than a
 * browser.
 *
 * <p>The legacy application had no such interface. It is a second, thin adapter over {@link
 * CatalogService}; no business rule lives here. The catalog is public and read-only, as it was on
 * the legacy pages.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "Categories, products, items and search. Public, read-only.")
class CatalogApiController {

  private static final String LOCALE_HELP =
      "Language of the returned text and prices: en_US, ja_JP or zh_CN.";
  private static final String START_HELP = "Zero-based position of the first result to return.";
  private static final String COUNT_HELP =
      "Page size. Defaults to the page size of the web pages; capped by the server.";

  private final CatalogService catalog;

  CatalogApiController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/categories")
  @Operation(summary = "List all categories, ordered by name")
  List<Category> categories(
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    return catalog.categories(locale(locale));
  }

  @GetMapping("/categories/{categoryId}")
  @Operation(summary = "Get one category")
  Category category(
      @PathVariable String categoryId,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    return catalog.category(categoryId, locale(locale)).orElseThrow(this::notFound);
  }

  @GetMapping("/categories/{categoryId}/products")
  @Operation(summary = "List the products of a category, ordered by name")
  PageResponse<Product> products(
      @PathVariable String categoryId,
      @Parameter(description = START_HELP) @RequestParam(defaultValue = "0") int start,
      @Parameter(description = COUNT_HELP) @RequestParam(required = false) Integer count,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    Locale language = locale(locale);
    catalog.category(categoryId, language).orElseThrow(this::notFound);
    return PageResponse.of(catalog.products(categoryId, start, count(count), language));
  }

  @GetMapping("/products/{productId}")
  @Operation(summary = "Get one product")
  Product product(
      @PathVariable String productId,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    return catalog.product(productId, locale(locale)).orElseThrow(this::notFound);
  }

  @GetMapping("/products/{productId}/items")
  @Operation(summary = "List the items of a product, ordered by item id")
  PageResponse<Item> items(
      @PathVariable String productId,
      @Parameter(description = START_HELP) @RequestParam(defaultValue = "0") int start,
      @Parameter(description = COUNT_HELP) @RequestParam(required = false) Integer count,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    Locale language = locale(locale);
    catalog.product(productId, language).orElseThrow(this::notFound);
    return PageResponse.of(catalog.items(productId, start, count(count), language));
  }

  @GetMapping("/items/{itemId}")
  @Operation(summary = "Get one item with its prices")
  Item item(
      @PathVariable String itemId,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    return catalog.item(itemId, locale(locale)).orElseThrow(this::notFound);
  }

  @GetMapping("/search")
  @Operation(
      summary = "Search items",
      description =
          "Returns the items for which any of the whitespace-separated keywords occurs, ignoring"
              + " case, in the product name, the category id or the item description.")
  PageResponse<Item> search(
      @RequestParam(defaultValue = "") String keywords,
      @Parameter(description = START_HELP) @RequestParam(defaultValue = "0") int start,
      @Parameter(description = COUNT_HELP) @RequestParam(required = false) Integer count,
      @Parameter(description = LOCALE_HELP) @RequestParam(defaultValue = "en_US") String locale) {
    return PageResponse.of(catalog.search(keywords, start, count(count), locale(locale)));
  }

  private int count(Integer requested) {
    return requested == null ? catalog.pageSize() : requested;
  }

  private Locale locale(String code) {
    return SupportedLocale.fromCode(code)
        .map(SupportedLocale::locale)
        .orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unsupported locale '" + code + "'"));
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
