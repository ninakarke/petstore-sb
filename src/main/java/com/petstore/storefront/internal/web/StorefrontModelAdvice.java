package com.petstore.storefront.internal.web;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Category;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies what the page layout needs on every page.
 *
 * <p>In the legacy application each layout fragment fetched its own data: {@code sidebar.jsp}
 * created a catalog helper and queried the database from inside the page. Here the data is put in
 * the model before the page is rendered, and templates only display it.
 */
@ControllerAdvice
class StorefrontModelAdvice {

  private final CatalogService catalog;

  StorefrontModelAdvice(CatalogService catalog) {
    this.catalog = catalog;
  }

  /** The category list of the sidebar. Legacy: {@code sidebar.jsp}. */
  @ModelAttribute("sidebarCategories")
  List<Category> sidebarCategories() {
    return catalog.categories();
  }
}
