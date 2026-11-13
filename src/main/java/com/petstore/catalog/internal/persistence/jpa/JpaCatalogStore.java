package com.petstore.catalog.internal.persistence.jpa;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import com.petstore.catalog.internal.CatalogStore;
import com.petstore.shared.StoreLocale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relational adapter of the catalog port.
 *
 * <p>Replaces the legacy {@code GenericCatalogDAO}. Each query below is the JPQL form of one
 * statement of the legacy {@code CatalogDAOSQL.xml}, with the same joins and the same locale
 * filter. The tables keep the legacy per-locale layout; this adapter always reads and writes the
 * English rows. Three things differ from the legacy statements on purpose:
 *
 * <ul>
 *   <li>item lists and search results have an {@code order by}; the legacy statements had none, so
 *       the content of a page was undefined;
 *   <li>a page is read with an offset and a limit; the legacy DAO opened a scrollable cursor over
 *       the full result and skipped to the start row;
 *   <li>search keywords are lower-cased and their wildcard characters escaped; the legacy DAO
 *       lower-cased only the column, so a keyword with a capital letter never matched.
 * </ul>
 */
@Repository
@Transactional(readOnly = true)
class JpaCatalogStore implements CatalogStore {

  /** The value of the {@code locale} column for the rows this application uses. */
  private static final String LOCALE = StoreLocale.CODE;

  private static final String ITEM_SELECT =
      """
      select new com.petstore.catalog.internal.persistence.jpa.ItemRow(
               p.categoryId, i.productId, pd.name, i.id, d.image, d.description,
               d.attr1, d.attr2, d.attr3, d.attr4, d.attr5, d.listPrice, d.unitCost)
        from ItemEntity i, ItemDetailsEntity d, ProductDetailsEntity pd, ProductEntity p
       where i.id = d.id.itemId
         and i.productId = pd.id.productId
         and p.id = pd.id.productId
         and d.id.locale = pd.id.locale
         and d.id.locale = :locale
      """;

  private static final char LIKE_ESCAPE = '!';

  private final EntityManager entityManager;

  JpaCatalogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  // ---- reads -------------------------------------------------------------------------------

  /** Legacy statement: {@code GET_CATEGORY}. */
  @Override
  public Optional<Category> findCategory(String categoryId) {
    return entityManager
        .createQuery(
            """
            select new com.petstore.catalog.Category(c.id, d.name, d.description)
              from CategoryEntity c, CategoryDetailsEntity d
             where c.id = d.id.categoryId
               and d.id.locale = :locale
               and c.id = :categoryId
            """,
            Category.class)
        .setParameter("locale", LOCALE)
        .setParameter("categoryId", categoryId)
        .getResultStream()
        .findFirst();
  }

  /** Legacy statement: {@code GET_CATEGORIES}. */
  @Override
  public List<Category> findCategories() {
    return entityManager
        .createQuery(
            """
            select new com.petstore.catalog.Category(c.id, d.name, d.description)
              from CategoryEntity c, CategoryDetailsEntity d
             where c.id = d.id.categoryId
               and d.id.locale = :locale
             order by d.name, c.id
            """,
            Category.class)
        .setParameter("locale", LOCALE)
        .getResultList();
  }

  /** Legacy statement: {@code GET_PRODUCT}. */
  @Override
  public Optional<Product> findProduct(String productId) {
    return entityManager
        .createQuery(
            """
            select new com.petstore.catalog.Product(p.id, d.name, d.description)
              from ProductEntity p, ProductDetailsEntity d
             where p.id = d.id.productId
               and d.id.locale = :locale
               and p.id = :productId
            """,
            Product.class)
        .setParameter("locale", LOCALE)
        .setParameter("productId", productId)
        .getResultStream()
        .findFirst();
  }

  /** Legacy statement: {@code GET_PRODUCTS}. */
  @Override
  public Page<Product> findProducts(String categoryId, int start, int count) {
    TypedQuery<Product> query =
        entityManager
            .createQuery(
                """
                select new com.petstore.catalog.Product(p.id, d.name, d.description)
                  from ProductEntity p, ProductDetailsEntity d
                 where p.id = d.id.productId
                   and d.id.locale = :locale
                   and p.categoryId = :categoryId
                 order by d.name, p.id
                """,
                Product.class)
            .setParameter("locale", LOCALE)
            .setParameter("categoryId", categoryId);
    return page(query, start, count, Function.identity());
  }

  /** Legacy statement: {@code GET_ITEM}. */
  @Override
  public Optional<Item> findItem(String itemId) {
    return entityManager
        .createQuery(ITEM_SELECT + " and i.id = :itemId", ItemRow.class)
        .setParameter("locale", LOCALE)
        .setParameter("itemId", itemId)
        .getResultStream()
        .findFirst()
        .map(ItemRow::toItem);
  }

  /** Legacy statement: {@code GET_ITEMS}. */
  @Override
  public Page<Item> findItems(String productId, int start, int count) {
    TypedQuery<ItemRow> query =
        entityManager
            .createQuery(ITEM_SELECT + " and i.productId = :productId order by i.id", ItemRow.class)
            .setParameter("locale", LOCALE)
            .setParameter("productId", productId);
    return page(query, start, count, ItemRow::toItem);
  }

  /**
   * Legacy statement: {@code SEARCH_ITEMS}. One group of three {@code like} conditions per keyword,
   * joined by {@code or}, exactly as the legacy statement was assembled from its fragments.
   */
  @Override
  public Page<Item> searchItems(Set<String> keywords, int start, int count) {
    List<String> patterns = keywords.stream().map(JpaCatalogStore::likePattern).toList();

    StringBuilder jpql = new StringBuilder(ITEM_SELECT).append(" and (");
    for (int i = 0; i < patterns.size(); i++) {
      if (i > 0) {
        jpql.append(" or ");
      }
      jpql.append(
          """
          (lower(pd.name) like :k%1$d escape '%2$c'
           or lower(p.categoryId) like :k%1$d escape '%2$c'
           or lower(d.description) like :k%1$d escape '%2$c')
          """
              .formatted(i, LIKE_ESCAPE));
    }
    jpql.append(") order by i.id");

    TypedQuery<ItemRow> query =
        entityManager.createQuery(jpql.toString(), ItemRow.class).setParameter("locale", LOCALE);
    for (int i = 0; i < patterns.size(); i++) {
      query.setParameter("k" + i, patterns.get(i));
    }
    return page(query, start, count, ItemRow::toItem);
  }

  /**
   * Reads one page. One row more than asked for is fetched to learn whether a next page exists,
   * which is all the legacy page object ever knew.
   */
  private static <R, T> Page<T> page(
      TypedQuery<R> query, int start, int count, Function<R, T> mapper) {
    List<R> rows = query.setFirstResult(start).setMaxResults(count + 1).getResultList();
    if (rows.isEmpty()) {
      return Page.empty();
    }
    boolean hasNext = rows.size() > count;
    List<T> items = new ArrayList<>(count);
    for (R row : hasNext ? rows.subList(0, count) : rows) {
      items.add(mapper.apply(row));
    }
    return new Page<>(items, start, count, hasNext);
  }

  /** Turns a keyword into a "contains, ignoring case" pattern in which nothing is a wildcard. */
  private static String likePattern(String keyword) {
    StringBuilder escaped = new StringBuilder("%");
    for (char character : keyword.toLowerCase(Locale.ROOT).toCharArray()) {
      if (character == '%' || character == '_' || character == LIKE_ESCAPE) {
        escaped.append(LIKE_ESCAPE);
      }
      escaped.append(character);
    }
    return escaped.append('%').toString();
  }

  // ---- writes ------------------------------------------------------------------------------

  @Override
  public boolean isEmpty() {
    return entityManager
            .createQuery("select count(c) from CategoryEntity c", Long.class)
            .getSingleResult()
        == 0;
  }

  @Override
  @Transactional
  public void saveCategory(String categoryId, CategoryText text) {
    entityManager.persist(new CategoryEntity(categoryId));
    entityManager.persist(
        new CategoryDetailsEntity(
            new CategoryDetailsId(categoryId, LOCALE),
            text.name(),
            text.image(),
            text.description()));
  }

  @Override
  @Transactional
  public void saveProduct(String productId, String categoryId, ProductText text) {
    entityManager.persist(new ProductEntity(productId, categoryId));
    entityManager.persist(
        new ProductDetailsEntity(
            new ProductDetailsId(productId, LOCALE),
            text.name(),
            text.image(),
            text.description()));
  }

  @Override
  @Transactional
  public void saveItem(String itemId, String productId, ItemDetails details) {
    entityManager.persist(new ItemEntity(itemId, productId));
    entityManager.persist(
        new ItemDetailsEntity(
            new ItemDetailsId(itemId, LOCALE),
            details.listPrice(),
            details.unitCost(),
            details.image(),
            details.description(),
            details.attributes()));
  }
}
