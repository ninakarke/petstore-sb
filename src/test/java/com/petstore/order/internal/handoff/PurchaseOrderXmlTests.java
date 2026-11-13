package com.petstore.order.internal.handoff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CustomerFixtures;
import com.petstore.order.Handoff;
import com.petstore.order.Order;
import com.petstore.order.OrderFixtures;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The purchase-order document handed to order processing: the contract with a system that is not
 * part of this migration.
 *
 * <p>Legacy sources: {@code components/purchaseorder/.../ejb/PurchaseOrder.java} ({@code toDOM},
 * {@code toXML}), the {@code toDOM} methods of {@code ContactInfo}, {@code Address}, {@code
 * CreditCard} and {@code LineItem}, {@code XMLDocumentUtils.appendChild}, and the DTDs under {@code
 * rsrc/schemas}.
 */
class PurchaseOrderXmlTests {

  private final Order order = OrderFixtures.order("10011", "j2ee", OrderFixtures.NOON);

  @Test
  @DisplayName("the document is valid against the unchanged legacy PurchaseOrder DTD")
  void documentIsValidAgainstTheLegacyDtd() {
    LegacyDtd.validate(PurchaseOrderXml.write(order, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("legacy: the document names the PurchaseOrder 1.1 document type")
  void documentDeclaresTheLegacyDocumentType() {
    assertThat(PurchaseOrderXml.write(order, ZoneOffset.UTC))
        .contains(
            "<!DOCTYPE PurchaseOrder PUBLIC"
                + " \"-//Sun Microsystems, Inc. - J2EE Blueprints Group//DTD PurchaseOrder 1.1//EN\""
                + " \"/com/sun/j2ee/blueprints/purchaseorder/rsrc/schemas/PurchaseOrder.dtd\">");
  }

  @Test
  @DisplayName("legacy toDOM: elements in the legacy order, with the locale attribute")
  void elementsComeInTheLegacyOrder() {
    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(order, ZoneOffset.UTC)).getDocumentElement();

    assertThat(root.getTagName()).isEqualTo("PurchaseOrder");
    assertThat(root.getAttribute("locale")).isEqualTo("en_US");
    assertThat(childNames(root))
        .containsExactly(
            "OrderId",
            "UserId",
            "EmailId",
            "OrderDate",
            "ShippingInfo",
            "BillingInfo",
            "TotalPrice",
            "CreditCard",
            "LineItem",
            "LineItem");
  }

  @Test
  @DisplayName("legacy: order id, user, e-mail, date as yyyy-MM-dd, total")
  void headerValuesAreWritten() {
    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(order, ZoneOffset.UTC)).getDocumentElement();

    assertThat(text(root, "OrderId")).isEqualTo("10011");
    assertThat(text(root, "UserId")).isEqualTo("j2ee");
    assertThat(text(root, "EmailId")).isEqualTo("aaa@bbb.ccc");
    assertThat(text(root, "OrderDate")).isEqualTo("2026-11-13");
    assertThat(new BigDecimal(text(root, "TotalPrice"))).isEqualByComparingTo("68.00");
    // What the legacy consumer does with the value.
    assertThat(Float.parseFloat(text(root, "TotalPrice"))).isEqualTo(68.0f);
  }

  @Test
  @DisplayName("legacy ContactInfo.toDOM / Address.toDOM: family name first, two street lines")
  void contactDetailsAreWritten() {
    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(order, ZoneOffset.UTC)).getDocumentElement();
    Element contact = child(child(root, "ShippingInfo"), "ContactInfo");
    Element address = child(contact, "Address");

    assertThat(childNames(contact))
        .containsExactly("FamilyName", "GivenName", "Address", "Email", "Phone");
    assertThat(text(contact, "FamilyName")).isEqualTo("ABC");
    assertThat(text(contact, "GivenName")).isEqualTo("XYZ");
    assertThat(text(contact, "Phone")).isEqualTo("555-555-5555");
    assertThat(childNames(address))
        .containsExactly("StreetName", "StreetName", "City", "State", "ZipCode", "Country");
    assertThat(address.getElementsByTagName("StreetName").item(0).getTextContent())
        .isEqualTo("1234 Anywhere Street");
    assertThat(address.getElementsByTagName("StreetName").item(1).getTextContent())
        .isEqualTo("Unit 555");
    assertThat(text(address, "ZipCode")).isEqualTo("94303");
    assertThat(childNames(child(child(root, "BillingInfo"), "ContactInfo"))).hasSize(5);
  }

  @Test
  @DisplayName("legacy CreditCard.toDOM and LineItem.toDOM")
  void cardAndLinesAreWritten() {
    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(order, ZoneOffset.UTC)).getDocumentElement();
    Element card = child(root, "CreditCard");
    Element secondLine = (Element) root.getElementsByTagName("LineItem").item(1);

    assertThat(childNames(card)).containsExactly("CardNumber", "CardType", "ExpiryDate");
    assertThat(text(card, "CardType")).isEqualTo("Meow Card");
    assertThat(childNames(secondLine))
        .containsExactly("CategoryId", "ProductId", "ItemId", "LineNum", "Quantity", "UnitPrice");
    assertThat(text(secondLine, "CategoryId")).isEqualTo("DOGS");
    assertThat(text(secondLine, "ItemId")).isEqualTo("EST-6");
    assertThat(text(secondLine, "LineNum")).isEqualTo("1");
    assertThat(text(secondLine, "Quantity")).isEqualTo("1");
    assertThat(Float.parseFloat(text(secondLine, "UnitPrice"))).isEqualTo(18.5f);
  }

  @Test
  @DisplayName(
      "legacy XMLDocumentUtils.appendChild: a missing value is an empty element, not a missing one")
  void missingValuesAreWrittenAsEmptyElements() {
    ContactInfo sparse =
        new ContactInfo(
            "Duke", "B", new Address("1 Street", null, "City", "CA", "1", null), null, "1");
    Order sparseOrder =
        new Order(
            "10012",
            "duke",
            null,
            OrderFixtures.NOON,
            sparse,
            sparse,
            CustomerFixtures.creditCard(),
            OrderFixtures.lines(),
            new BigDecimal("68.00"),
            Handoff.pending(),
            null);

    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(sparseOrder, ZoneOffset.UTC))
            .getDocumentElement();
    Element address = child(child(child(root, "BillingInfo"), "ContactInfo"), "Address");

    assertThat(text(root, "EmailId")).isEmpty();
    assertThat(childNames(address))
        .containsExactly("StreetName", "StreetName", "City", "State", "ZipCode", "Country");
    assertThat(address.getElementsByTagName("StreetName").item(1).getTextContent()).isEmpty();
    assertThat(text(address, "Country")).isEmpty();
  }

  @Test
  void textIsEscaped() {
    ContactInfo tricky =
        new ContactInfo(
            "<Duke>",
            "B & Sons",
            new Address("1 \"Street\"", null, "City", "CA", "1", null),
            null,
            "1");
    Order trickyOrder =
        new Order(
            "10013",
            "duke",
            "a@b.c",
            OrderFixtures.NOON,
            tricky,
            tricky,
            CustomerFixtures.creditCard(),
            OrderFixtures.lines(),
            new BigDecimal("68.00"),
            Handoff.pending(),
            null);

    Element root =
        LegacyDtd.validate(PurchaseOrderXml.write(trickyOrder, ZoneOffset.UTC))
            .getDocumentElement();
    Element contact = child(child(root, "ShippingInfo"), "ContactInfo");

    assertThat(text(contact, "GivenName")).isEqualTo("<Duke>");
    assertThat(text(contact, "FamilyName")).isEqualTo("B & Sons");
  }

  @Test
  @DisplayName("the validation used by these tests does reject a document that breaks the DTD")
  void validationCatchesABrokenDocument() {
    String broken =
        PurchaseOrderXml.write(order, ZoneOffset.UTC).replace("<UserId>j2ee</UserId>", "");

    assertThatThrownBy(() -> LegacyDtd.validate(broken)).isInstanceOf(AssertionError.class);
  }

  private static List<String> childNames(Element parent) {
    List<String> names = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element) {
        names.add(element.getTagName());
      }
    }
    return names;
  }

  private static Element child(Element parent, String name) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && element.getTagName().equals(name)) {
        return element;
      }
    }
    throw new AssertionError("No <" + name + "> in <" + parent.getTagName() + ">");
  }

  private static String text(Element parent, String name) {
    return child(parent, name).getTextContent();
  }
}
