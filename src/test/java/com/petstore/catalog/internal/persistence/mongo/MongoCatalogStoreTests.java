package com.petstore.catalog.internal.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.internal.CatalogStore;
import com.petstore.catalog.internal.CatalogStoreContract;
import com.petstore.testsupport.MongoStoreTest;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;

/**
 * The catalog store contract on a real MongoDB server: the same tests the relational adapter passes
 * on H2 and PostgreSQL. The tests below it are about what is specific to this store, the shape of
 * the documents.
 */
@MongoStoreTest
@Import(MongoCatalogStore.class)
class MongoCatalogStoreTests extends CatalogStoreContract {

  @Autowired private CatalogStore store;
  @Autowired private MongoTemplate mongo;

  @Override
  protected CatalogStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    for (String collection : List.of("items", "products", "categories")) {
      mongo.remove(new Query(), collection);
    }
  }

  @Test
  void storeWithoutCategoriesIsEmpty() {
    emptyTheStore();

    assertThat(store.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("an item is one document that carries its product's name and category")
  void itemIsOneSelfContainedDocument() {
    Document item = mongo.getCollection("items").find(new Document("_id", "EST-1")).first();

    assertThat(item)
        .containsEntry("productId", "FI-SW-01")
        .containsEntry("productName", "Angelfish")
        .containsEntry("categoryId", "FISH")
        .containsEntry("attributes", List.of("Large", "Cuddly"));
  }

  @Test
  @DisplayName("prices are stored as decimal numbers, not as text or binary floating point")
  void pricesAreStoredAsDecimals() {
    Document item = mongo.getCollection("items").find(new Document("_id", "EST-1")).first();

    assertThat(item.get("listPrice")).isEqualTo(Decimal128.parse("16.50"));
    assertThat(item.get("unitCost")).isEqualTo(Decimal128.parse("10.00"));
  }

  @Test
  @DisplayName("documents do not name the Java classes they were written from")
  void documentsCarryNoClassName() {
    for (String collection : List.of("items", "products", "categories")) {
      assertThat(mongo.getCollection(collection).find().first()).doesNotContainKey("_class");
    }
  }

  @Test
  @DisplayName("the indexes that the listings are read from exist")
  void indexesExist() {
    assertThat(mongo.indexOps("categories").getIndexInfo())
        .extracting(IndexInfo::getName)
        .contains("by_name");
    assertThat(mongo.indexOps("products").getIndexInfo())
        .extracting(IndexInfo::getName)
        .contains("by_category_and_name");
    assertThat(mongo.indexOps("items").getIndexInfo())
        .extracting(IndexInfo::getName)
        .contains("by_product");
  }
}
