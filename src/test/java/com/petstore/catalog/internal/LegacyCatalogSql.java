package com.petstore.catalog.internal;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Product;
import com.petstore.shared.SeedDocument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Element;

/**
 * Executes the legacy catalog SQL exactly as the legacy DAO did.
 *
 * <p>The statements are read from an unchanged copy of the legacy {@code CatalogDAOSQL.xml} (the
 * {@code oracle} block, which uses plain comma joins). A statement is assembled from its fragments
 * with the algorithm of {@code GenericCatalogDAO.buildSQLStatement}, every parameter is bound as a
 * string, and columns are read by position and trimmed, as in {@code GenericCatalogDAO}. The result
 * is what the legacy application would have read from the same data.
 */
final class LegacyCatalogSql {

  private record Fragment(String text, int parameterCount, boolean variable) {}

  private final JdbcTemplate jdbc;
  private final Map<String, List<Fragment>> statements = new LinkedHashMap<>();

  LegacyCatalogSql(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    SeedDocument document = SeedDocument.load(new ClassPathResource("legacy/CatalogDAOSQL.xml"));
    Element oracle =
        document.elements("DAOStatements").stream()
            .filter(element -> "oracle".equals(element.getAttribute("database")))
            .findFirst()
            .orElseThrow();
    for (Element statement : SeedDocument.children(oracle, "SQLStatement")) {
      List<Fragment> fragments = new ArrayList<>();
      for (Element fragment : SeedDocument.children(statement, "SQLFragment")) {
        fragments.add(
            new Fragment(
                fragment.getTextContent(),
                Integer.parseInt(fragment.getAttribute("parameterNb")),
                "VARIABLE".equals(fragment.getAttribute("occurrence"))));
      }
      statements.put(statement.getAttribute("method"), fragments);
    }
  }

  /** Same algorithm as the legacy {@code buildSQLStatement}. */
  String sql(String method, int parameterCount) {
    StringBuilder sql = new StringBuilder();
    int remaining = parameterCount;
    for (Fragment fragment : statements.get(method)) {
      if (fragment.variable()) {
        while (remaining > 0 && remaining >= fragment.parameterCount()) {
          sql.append(fragment.text());
          remaining -= fragment.parameterCount();
        }
      } else {
        sql.append(fragment.text());
        remaining -= fragment.parameterCount();
      }
    }
    return sql.toString();
  }

  List<Category> categories(String locale) {
    return jdbc.query(
        sql("GET_CATEGORIES", 1),
        (rs, row) -> new Category(rs.getString(1).trim(), rs.getString(2), rs.getString(3)),
        locale);
  }

  List<Category> category(String categoryId, String locale) {
    return jdbc.query(
        sql("GET_CATEGORY", 2),
        (rs, row) -> new Category(categoryId, rs.getString(1), rs.getString(2)),
        locale,
        categoryId);
  }

  List<Product> products(String categoryId, String locale) {
    return jdbc.query(
        sql("GET_PRODUCTS", 2),
        (rs, row) -> new Product(rs.getString(1).trim(), rs.getString(2), rs.getString(3)),
        locale,
        categoryId);
  }

  List<Product> product(String productId, String locale) {
    return jdbc.query(
        sql("GET_PRODUCT", 2),
        (rs, row) -> new Product(productId, rs.getString(1), rs.getString(2)),
        locale,
        productId);
  }

  /** Columns: catid, productid, name, image, descn, attr1..attr5, listprice, unitcost. */
  List<Item> item(String itemId, String locale) {
    return jdbc.query(
        sql("GET_ITEM", 2),
        (rs, row) ->
            item(rs, rs.getString(1).trim(), rs.getString(2).trim(), rs.getString(3), itemId, 4),
        locale,
        itemId);
  }

  /** Columns: catid, name, itemid, image, descn, attr1..attr5, listprice, unitcost. */
  List<Item> items(String productId, String locale) {
    return jdbc.query(
        sql("GET_ITEMS", 2),
        (rs, row) ->
            item(rs, rs.getString(1).trim(), productId, rs.getString(2), rs.getString(3).trim(), 4),
        locale,
        productId);
  }

  /** Columns: catid, productid, name, itemid, image, descn, attr1..attr5, listprice, unitcost. */
  List<Item> search(String locale, String... keywords) {
    List<Object> parameters = new ArrayList<>();
    parameters.add(locale);
    for (String keyword : keywords) {
      // The legacy DAO did not change the case of the keyword.
      parameters.add("%" + keyword + "%");
      parameters.add("%" + keyword + "%");
      parameters.add("%" + keyword + "%");
    }
    return jdbc.query(
        sql("SEARCH_ITEMS", parameters.size()),
        (rs, row) ->
            item(
                rs,
                rs.getString(1).trim(),
                rs.getString(2).trim(),
                rs.getString(3),
                rs.getString(4).trim(),
                5),
        parameters.toArray());
  }

  /** Reads image, descn, attr1..attr5, listprice, unitcost starting at the given column. */
  private static Item item(
      ResultSet rs,
      String categoryId,
      String productId,
      String productName,
      String itemId,
      int firstColumn)
      throws SQLException {
    int column = firstColumn;
    String image = rs.getString(column++).trim();
    String description = rs.getString(column++);
    List<String> attributes =
        Stream.of(
                rs.getString(column++),
                rs.getString(column++),
                rs.getString(column++),
                rs.getString(column++),
                rs.getString(column++))
            .filter(Objects::nonNull)
            .toList();
    return new Item(
        categoryId,
        productId,
        productName,
        itemId,
        image,
        description,
        attributes,
        rs.getBigDecimal(column++),
        rs.getBigDecimal(column));
  }
}
