package com.petstore.catalog.internal.web;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * The catalog pages: products of a category, items of a product, one item, and search results.
 *
 * <p>Legacy equivalents: the screens {@code category}, {@code product}, {@code item} and {@code
 * search}. Those were JSPs that set properties on a session-scoped helper bean and let it query the
 * database while the page was being rendered. Here the controller asks the catalog service and
 * hands the result to a template.
 *
 * <p>Paging keeps the legacy {@code start} parameter. The page size is no longer taken from the
 * URL.
 */
@Controller
class CatalogPagesController {

  private final CatalogService catalog;

  CatalogPagesController(CatalogService catalog) {
    this.catalog = catalog;
  }

  /** Legacy: {@code category.screen?category_id=...}. */
  @GetMapping("/categories/{categoryId}")
  String category(
      @PathVariable String categoryId, @RequestParam(defaultValue = "0") int start, Model model) {
    Category category = catalog.category(categoryId).orElseThrow(this::notFound);
    model.addAttribute("category", category);
    model.addAttribute("products", catalog.products(categoryId, start, catalog.pageSize()));
    return "catalog/category";
  }

  /** Legacy: {@code product.screen?product_id=...}. */
  @GetMapping("/products/{productId}")
  String product(
      @PathVariable String productId, @RequestParam(defaultValue = "0") int start, Model model) {
    Product product = catalog.product(productId).orElseThrow(this::notFound);
    model.addAttribute("product", product);
    model.addAttribute("items", catalog.items(productId, start, catalog.pageSize()));
    return "catalog/product";
  }

  /** Legacy: {@code item.screen?item_id=...}. */
  @GetMapping("/items/{itemId}")
  String item(@PathVariable String itemId, Model model) {
    Item item = catalog.item(itemId).orElseThrow(this::notFound);
    model.addAttribute("item", item);
    return "catalog/item";
  }

  /** Legacy: {@code search.screen?keywords=...}. */
  @GetMapping("/search")
  String search(
      @RequestParam(defaultValue = "") String keywords,
      @RequestParam(defaultValue = "0") int start,
      Model model) {
    model.addAttribute("keywords", keywords);
    model.addAttribute("results", catalog.search(keywords, start, catalog.pageSize()));
    return "catalog/search";
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
