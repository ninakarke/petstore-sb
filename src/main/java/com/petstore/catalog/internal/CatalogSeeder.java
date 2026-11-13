package com.petstore.catalog.internal;

import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.CatalogStore.ItemDetails;
import com.petstore.catalog.internal.CatalogStore.ProductText;
import com.petstore.shared.SeedDocument;
import com.petstore.shared.SeedProperties;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *   <li>it writes through the persistence port, so the same loader fills any data store.
 * </ul>
 *
 * <p>Element-to-field mapping is the legacy one: {@code Attribute[0..4]} become the item's
 * attributes, and {@code xml:lang="en-US"} is stored as {@code en_US}.
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
      Map<String, CategoryText> text = new LinkedHashMap<>();
      for (Element details : SeedDocument.children(category, "CategoryDetails")) {
        text.put(
            SeedDocument.localeCode(details),
            new CategoryText(
                SeedDocument.text(details, "Name").orElseThrow(),
                SeedDocument.text(details, "Image").orElse(null),
                SeedDocument.text(details, "Description").orElse(null)));
      }
      store.saveCategory(category.getAttribute("id"), text);
    }

    List<Element> products = seed.elements("Catalog", "Products", "Product");
    for (Element product : products) {
      Map<String, ProductText> text = new LinkedHashMap<>();
      for (Element details : SeedDocument.children(product, "ProductDetails")) {
        text.put(
            SeedDocument.localeCode(details),
            new ProductText(
                SeedDocument.text(details, "Name").orElseThrow(),
                SeedDocument.text(details, "Image").orElse(null),
                SeedDocument.text(details, "Description").orElse(null)));
      }
      store.saveProduct(product.getAttribute("id"), product.getAttribute("category"), text);
    }

    List<Element> items = seed.elements("Catalog", "Items", "Item");
    for (Element item : items) {
      Map<String, ItemDetails> detailsByLocale = new LinkedHashMap<>();
      for (Element details : SeedDocument.children(item, "ItemDetails")) {
        detailsByLocale.put(
            SeedDocument.localeCode(details),
            new ItemDetails(
                new BigDecimal(SeedDocument.text(details, "ListPrice").orElseThrow()),
                new BigDecimal(SeedDocument.text(details, "UnitCost").orElseThrow()),
                SeedDocument.text(details, "Image").orElseThrow(),
                SeedDocument.text(details, "Description").orElseThrow(),
                SeedDocument.texts(details, "Attribute")));
      }
      store.saveItem(item.getAttribute("id"), item.getAttribute("product"), detailsByLocale);
    }

    log.info(
        "Catalog seed data: loaded {} categories, {} products and {} items",
        categories.size(),
        products.size(),
        items.size());
  }
}
