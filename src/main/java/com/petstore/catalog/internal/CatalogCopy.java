package com.petstore.catalog.internal;

import com.petstore.catalog.internal.CatalogStore.StoredCategory;
import com.petstore.catalog.internal.CatalogStore.StoredItem;
import com.petstore.catalog.internal.CatalogStore.StoredProduct;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreCopy;
import java.util.List;

/**
 * Copies a whole catalog from one store to another, through the catalog port on both sides.
 *
 * <p>Categories go first, then products, then items: a product needs its category and an item its
 * product, on every store.
 */
public final class CatalogCopy {

  private CatalogCopy() {}

  public static List<CopyReport> copy(CatalogStore source, CatalogStore target, int batchSize) {
    CopyReport categories =
        StoreCopy.copy(
            "categories",
            StoredCategory::id,
            source::categoriesFrom,
            target::categoriesFrom,
            category -> target.saveCategory(category.id(), category.text()),
            batchSize);
    CopyReport products =
        StoreCopy.copy(
            "products",
            StoredProduct::id,
            source::productsFrom,
            target::productsFrom,
            product -> target.saveProduct(product.id(), product.categoryId(), product.text()),
            batchSize);
    CopyReport items =
        StoreCopy.copy(
            "items",
            StoredItem::id,
            source::itemsFrom,
            target::itemsFrom,
            item -> target.saveItem(item.id(), item.productId(), item.details()),
            batchSize);
    return List.of(categories, products, items);
  }
}
