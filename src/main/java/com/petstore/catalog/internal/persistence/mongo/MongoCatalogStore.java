package com.petstore.catalog.internal.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import com.petstore.catalog.internal.CatalogStore;
import com.petstore.shared.DocumentStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * MongoDB adapter of the catalog port.
 *
 * <p>Three collections, one document per category, product and item. Each method answers the same
 * question as one statement of the legacy {@code CatalogDAOSQL.xml}, and the same contract test
 * suite that the relational adapter passes decides whether it answers it the same way.
 *
 * <p>What the relational schema got from the database, this adapter does itself:
 *
 * <ul>
 *   <li>a product must name an existing category and an item an existing product; there are no
 *       foreign keys, so this is checked when reference data is saved;
 *   <li>lists are ordered by the plain code-point order of MongoDB's default comparison, which is
 *       the order the relational databases are pinned to;
 *   <li>the indexes the queries rely on are created when the adapter starts.
 * </ul>
 */
@Repository
@DocumentStore
class MongoCatalogStore implements CatalogStore, InitializingBean {

  private static final Sort BY_NAME_THEN_ID = Sort.by("name", "_id");
  private static final Sort BY_ID = Sort.by("_id");

  private final MongoOperations mongo;

  MongoCatalogStore(MongoOperations mongo) {
    this.mongo = mongo;
  }

  /** Every list this adapter reads is served by one of these indexes, already in order. */
  @Override
  public void afterPropertiesSet() {
    mongo
        .indexOps(CategoryDocument.class)
        .createIndex(index("by_name").on("name", Sort.Direction.ASC).on("_id", Sort.Direction.ASC));
    mongo
        .indexOps(ProductDocument.class)
        .createIndex(
            index("by_category_and_name")
                .on("categoryId", Sort.Direction.ASC)
                .on("name", Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC));
    mongo
        .indexOps(ItemDocument.class)
        .createIndex(
            index("by_product").on("productId", Sort.Direction.ASC).on("_id", Sort.Direction.ASC));
  }

  private static Index index(String name) {
    return new Index().named(name);
  }

  // ---- reads -------------------------------------------------------------------------------

  /** Legacy statement: {@code GET_CATEGORY}. */
  @Override
  public Optional<Category> findCategory(String categoryId) {
    return Optional.ofNullable(mongo.findById(categoryId, CategoryDocument.class))
        .map(MongoCatalogStore::toCategory);
  }

  /** Legacy statement: {@code GET_CATEGORIES}. */
  @Override
  public List<Category> findCategories() {
    return mongo.find(new Query().with(BY_NAME_THEN_ID), CategoryDocument.class).stream()
        .map(MongoCatalogStore::toCategory)
        .toList();
  }

  /** Legacy statement: {@code GET_PRODUCT}. */
  @Override
  public Optional<Product> findProduct(String productId) {
    return Optional.ofNullable(mongo.findById(productId, ProductDocument.class))
        .map(MongoCatalogStore::toProduct);
  }

  /** Legacy statement: {@code GET_PRODUCTS}. */
  @Override
  public Page<Product> findProducts(String categoryId, int start, int count) {
    Query query = query(where("categoryId").is(categoryId)).with(BY_NAME_THEN_ID);
    return page(query, start, count, ProductDocument.class, MongoCatalogStore::toProduct);
  }

  /** Legacy statement: {@code GET_ITEM}. */
  @Override
  public Optional<Item> findItem(String itemId) {
    return Optional.ofNullable(mongo.findById(itemId, ItemDocument.class))
        .map(MongoCatalogStore::toItem);
  }

  /** Legacy statement: {@code GET_ITEMS}. */
  @Override
  public Page<Item> findItems(String productId, int start, int count) {
    Query query = query(where("productId").is(productId)).with(BY_ID);
    return page(query, start, count, ItemDocument.class, MongoCatalogStore::toItem);
  }

  /**
   * Legacy statement: {@code SEARCH_ITEMS}. Its {@code like '%keyword%'} on three columns becomes a
   * case-insensitive "contains" on the same three fields, with the keyword taken literally.
   *
   * <p>A "contains" match cannot use an index: this query reads every item. That is the price of
   * returning exactly what the legacy search returned, and it is acceptable for a catalog of this
   * size. A catalog too large for it needs a search index, which matches by word and would
   * therefore change the results.
   */
  @Override
  public Page<Item> searchItems(Set<String> keywords, int start, int count) {
    List<Criteria> matches = new ArrayList<>();
    for (String keyword : keywords) {
      Pattern contains = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
      matches.add(where("productName").regex(contains));
      matches.add(where("categoryId").regex(contains));
      matches.add(where("description").regex(contains));
    }
    Query query = query(new Criteria().orOperator(matches)).with(BY_ID);
    return page(query, start, count, ItemDocument.class, MongoCatalogStore::toItem);
  }

  /**
   * Reads one page. One document more than asked for is fetched to learn whether a next page
   * exists, which is all the legacy page object ever knew.
   */
  private <D, T> Page<T> page(
      Query query, int start, int count, Class<D> type, Function<D, T> mapper) {
    List<D> documents = mongo.find(query.skip(start).limit(count + 1), type);
    if (documents.isEmpty()) {
      return Page.empty();
    }
    boolean hasNext = documents.size() > count;
    List<T> items = new ArrayList<>(count);
    for (D document : hasNext ? documents.subList(0, count) : documents) {
      items.add(mapper.apply(document));
    }
    return new Page<>(items, start, count, hasNext);
  }

  private static Category toCategory(CategoryDocument document) {
    return new Category(document.id(), document.name(), document.description());
  }

  private static Product toProduct(ProductDocument document) {
    return new Product(document.id(), document.name(), document.description());
  }

  private static Item toItem(ItemDocument document) {
    return new Item(
        document.categoryId(),
        document.productId(),
        document.productName(),
        document.id(),
        document.image(),
        document.description(),
        document.attributes(),
        document.listPrice(),
        document.unitCost());
  }

  // ---- reading everything -------------------------------------------------------------------

  @Override
  public List<StoredCategory> categoriesFrom(String startId, int limit) {
    return mongo.find(inKeyOrderFrom(startId, limit), CategoryDocument.class).stream()
        .map(document ->
            new StoredCategory(
                document.id(),
                new CategoryText(document.name(), document.image(), document.description())))
        .toList();
  }

  @Override
  public List<StoredProduct> productsFrom(String startId, int limit) {
    return mongo.find(inKeyOrderFrom(startId, limit), ProductDocument.class).stream()
        .map(document ->
            new StoredProduct(
                document.id(),
                document.categoryId(),
                new ProductText(document.name(), document.image(), document.description())))
        .toList();
  }

  @Override
  public List<StoredItem> itemsFrom(String startId, int limit) {
    return mongo.find(inKeyOrderFrom(startId, limit), ItemDocument.class).stream()
        .map(document ->
            new StoredItem(
                document.id(),
                document.productId(),
                new ItemDetails(
                    document.listPrice(),
                    document.unitCost(),
                    document.image(),
                    document.description(),
                    document.attributes())))
        .toList();
  }

  private static Query inKeyOrderFrom(String startId, int limit) {
    Query query = startId == null ? new Query() : query(where("_id").gte(startId));
    return query.with(BY_ID).limit(limit);
  }

  // ---- writes ------------------------------------------------------------------------------

  @Override
  public boolean isEmpty() {
    return !mongo.exists(new Query(), CategoryDocument.class);
  }

  @Override
  public void saveCategory(String categoryId, CategoryText text) {
    mongo.insert(new CategoryDocument(categoryId, text.name(), text.image(), text.description()));
  }

  @Override
  public void saveProduct(String productId, String categoryId, ProductText text) {
    if (!mongo.exists(query(where("_id").is(categoryId)), CategoryDocument.class)) {
      throw new DataIntegrityViolationException(
          "Product " + productId + " names category " + categoryId + ", which does not exist");
    }
    mongo.insert(
        new ProductDocument(productId, categoryId, text.name(), text.image(), text.description()));
  }

  @Override
  public void saveItem(String itemId, String productId, ItemDetails details) {
    ProductDocument product = mongo.findById(productId, ProductDocument.class);
    if (product == null) {
      throw new DataIntegrityViolationException(
          "Item " + itemId + " names product " + productId + ", which does not exist");
    }
    mongo.insert(
        new ItemDocument(
            itemId,
            productId,
            product.name(),
            product.categoryId(),
            details.image(),
            details.description(),
            details.attributes(),
            details.listPrice(),
            details.unitCost()));
  }
}
