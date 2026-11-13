package com.petstore.catalog.internal;

import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import com.petstore.shared.SeedDocument;
import com.petstore.shared.SeedProperties;
import com.petstore.shared.StoreLocale;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Loads the catalog part of the seed document into an empty catalog store, once, at startup.
 *
 * <p>Replaces the catalog half of the legacy {@code PopulateServlet} and its populator classes
 * ({@code CategoryPopulator}, {@code ProductPopulator}, {@code ItemPopulator} and their details
 * counterparts). Differences:
 *
 * <ul>
 *   <li>it is not reachable over HTTP; the legacy servlet let any visitor drop and reload the
 *       database;
 *   <li>it never drops anything, and does nothing if the store already holds data;
 *   <li>it writes through the persistence port, so the same loader fills any data store;
 *   <li>the seed file carries every text in three languages; only the English text is loaded.
 * </ul>
 *
 * <p>Element-to-field mapping is the legacy one: {@code Attribute[0..4]} become the item's
 * attributes.
 */
@Component
@Order(10)
class CatalogSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

  private final CatalogStore store;
  private final SeedProperties properties;

  CatalogSeeder(CatalogStore store, SeedProperties properties) {
    this.store = store;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.enabled()) {
      log.info("Catalog seed data: disabled");
      return;
    }
    if (!store.isEmpty()) {
      log.info("Catalog seed data: skipped, the catalog already holds data");
      return;
    }
    SeedDocument seed = SeedDocument.load(properties.location());

    List<Element> categories = seed.elements("Catalog", "Categories", "Category");
    for (Element category : categories) {
      Element details = english(category, "CategoryDetails").orElseThrow();
      store.saveCategory(
          category.getAttribute("id"),
          new CategoryText(
              SeedDocument.text(details, "Name").orElseThrow(),
              SeedDocument.text(details, "Image").orElse(null),
              SeedDocument.text(details, "Description").orElse(null)));
    }

    List<Element> products = seed.elements("Catalog", "Products", "Product");
    for (Element product : products) {
      Element details = english(product, "ProductDetails").orElseThrow();
      store.saveProduct(
          product.getAttribute("id"),
          product.getAttribute("category"),
          new ProductText(
              SeedDocument.text(details, "Name").orElseThrow(),
              SeedDocument.text(details, "Image").orElse(null),
              SeedDocument.text(details, "Description").orElse(null)));
    }

    List<Element> items = seed.elements("Catalog", "Items", "Item");
    for (Element item : items) {
      Element details = english(item, "ItemDetails").orElseThrow();
      store.saveItem(
          item.getAttribute("id"),
          item.getAttribute("product"),
          new ItemDetails(
              new BigDecimal(SeedDocument.text(details, "ListPrice").orElseThrow()),
              new BigDecimal(SeedDocument.text(details, "UnitCost").orElseThrow()),
              SeedDocument.text(details, "Image").orElseThrow(),
              SeedDocument.text(details, "Description").orElseThrow(),
              SeedDocument.texts(details, "Attribute")));
    }

    log.info(
        "Catalog seed data: loaded {} categories, {} products and {} items",
        categories.size(),
        products.size(),
        items.size());
  }

  /** The English one of the per-language detail elements of a category, product or item. */
  private static Optional<Element> english(Element parent, String detailsName) {
    return SeedDocument.children(parent, detailsName).stream()
        .filter(details -> StoreLocale.CODE.equals(SeedDocument.localeCode(details)))
        .findFirst();
  }
}
