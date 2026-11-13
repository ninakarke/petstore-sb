package com.petstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;

/**
 * Reading the seed document, the legacy {@code Populate-UTF8.xml} used unchanged.
 *
 * <p>Legacy source: {@code apps/petstore/.../tools/populate/XMLDBHandler.java}.
 */
class SeedDocumentTests {

  private final SeedDocument seed =
      SeedDocument.load(new ClassPathResource("seed/Populate-UTF8.xml"));

  @Test
  @DisplayName("legacy seed data: 5 categories, 16 products, 28 items, 4 users, 4 customers")
  void legacySeedFileIsReadCompletely() {
    assertThat(seed.elements("Catalog", "Categories", "Category")).hasSize(5);
    assertThat(seed.elements("Catalog", "Products", "Product")).hasSize(16);
    assertThat(seed.elements("Catalog", "Items", "Item")).hasSize(28);
    assertThat(seed.elements("Users", "User")).hasSize(4);
    assertThat(seed.elements("Customers", "Customer")).hasSize(4);
  }

  @Test
  @DisplayName("legacy: xml:lang=\"en-US\" is stored as en_US")
  void languageTagIsTurnedIntoTheStoredLocaleCode() {
    Element fish = seed.elements("Catalog", "Categories", "Category").get(0);

    assertThat(SeedDocument.children(fish, "CategoryDetails"))
        .extracting(SeedDocument::localeCode)
        .containsExactly("en_US", "ja_JP", "zh_CN");
  }

  @Test
  void childTextIsTrimmedAndAbsentChildrenAreReportedAsAbsent() {
    Element fish = seed.elements("Catalog", "Categories", "Category").get(0);
    Element english = SeedDocument.children(fish, "CategoryDetails").get(0);

    assertThat(SeedDocument.text(english, "Name")).contains("Fish");
    assertThat(SeedDocument.text(english, "Description")).isEmpty();
    assertThat(SeedDocument.child(english, "Image")).isPresent();
  }

  @Test
  void repeatedChildrenAreReadInDocumentOrder() {
    Element firstItem = seed.elements("Catalog", "Items", "Item").get(0);
    Element english = SeedDocument.children(firstItem, "ItemDetails").get(0);

    assertThat(SeedDocument.texts(english, "Attribute")).containsExactly("Large", "Cuddly");
  }

  /** The legacy data file declares external DTD fragments; a hostile file could declare more. */
  @Test
  void externalEntitiesAreNeverFetched() {
    String xml =
        """
        <?xml version="1.0"?>
        <!DOCTYPE Populate [
          <!ENTITY secret SYSTEM "file:///etc/passwd">
        ]>
        <Populate><Users><User id="x">&secret;</User></Users></Populate>
        """;

    SeedDocument document =
        SeedDocument.load(new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8)));

    List<Element> users = document.elements("Users", "User");
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getTextContent()).doesNotContain("root");
  }

  @Test
  void brokenDocumentIsReportedAsAnError() {
    ByteArrayResource broken = new ByteArrayResource("<Populate>".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> SeedDocument.load(broken)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void missingDocumentIsReportedAsAnError() {
    assertThatThrownBy(() -> SeedDocument.load(new ClassPathResource("seed/missing.xml")))
        .isInstanceOf(UncheckedIOException.class);
  }
}
