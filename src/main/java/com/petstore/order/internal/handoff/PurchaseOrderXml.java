package com.petstore.order.internal.handoff;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.order.Order;
import com.petstore.order.OrderLine;
import com.petstore.shared.StoreLocale;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writes an order as the purchase-order document the legacy order-processing application expects.
 *
 * <p>This is the contract with a system that is not part of this migration, so it is reproduced
 * exactly: the element names and their order, the {@code locale} attribute, the date format and the
 * document type declaration are those of the legacy {@code PurchaseOrder.toXML()} (document type
 * "PurchaseOrder 1.1"). As there, an absent value is written as an empty element, never left out.
 * Tests validate the output against the legacy DTD.
 *
 * <p>One difference in form, not in meaning: amounts are written as exact decimals ({@code 68.00})
 * where the legacy code printed a {@code float} ({@code 68.0}). The legacy consumer reads the value
 * with {@code Float.parseFloat}, which accepts both.
 */
public final class PurchaseOrderXml {

  /** Legacy: {@code PurchaseOrder.DTD_PUBLIC_ID}. */
  public static final String DTD_PUBLIC_ID =
      "-//Sun Microsystems, Inc. - J2EE Blueprints Group//DTD PurchaseOrder 1.1//EN";

  /** Legacy: {@code PurchaseOrder.DTD_SYSTEM_ID}. */
  public static final String DTD_SYSTEM_ID =
      "/com/sun/j2ee/blueprints/purchaseorder/rsrc/schemas/PurchaseOrder.dtd";

  /** Legacy: {@code new SimpleDateFormat("yyyy-MM-dd")}. */
  private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private PurchaseOrderXml() {}

  public static String write(Order order, ZoneId zone) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = document.createElement("PurchaseOrder");
      root.setAttribute("locale", StoreLocale.CODE);
      document.appendChild(root);

      append(root, "OrderId", order.orderId());
      append(root, "UserId", order.userId());
      append(root, "EmailId", order.email());
      append(root, "OrderDate", ORDER_DATE.format(order.placedAt().atZone(zone)));
      child(root, "ShippingInfo").appendChild(contactInfo(document, order.shipTo()));
      child(root, "BillingInfo").appendChild(contactInfo(document, order.billTo()));
      append(root, "TotalPrice", order.totalPrice().toPlainString());
      root.appendChild(creditCard(document, order.creditCard()));
      for (OrderLine line : order.lines()) {
        root.appendChild(lineItem(document, line));
      }

      TransformerFactory factory = TransformerFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, DTD_PUBLIC_ID);
      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_SYSTEM_ID);
      StringWriter xml = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(xml));
      return xml.toString();
    } catch (ParserConfigurationException | TransformerException exception) {
      throw new IllegalStateException(
          "Cannot write the purchase order " + order.orderId(), exception);
    }
  }

  private static Element contactInfo(Document document, ContactInfo contact) {
    Element element = document.createElement("ContactInfo");
    append(element, "FamilyName", contact.familyName());
    append(element, "GivenName", contact.givenName());
    Address address = contact.address();
    Element addressElement = child(element, "Address");
    append(addressElement, "StreetName", address.streetName1());
    append(addressElement, "StreetName", address.streetName2());
    append(addressElement, "City", address.city());
    append(addressElement, "State", address.state());
    append(addressElement, "ZipCode", address.zipCode());
    append(addressElement, "Country", address.country());
    append(element, "Email", contact.email());
    append(element, "Phone", contact.telephone());
    return element;
  }

  private static Element creditCard(Document document, CreditCard card) {
    Element element = document.createElement("CreditCard");
    append(element, "CardNumber", card.cardNumber());
    append(element, "CardType", card.cardType());
    append(element, "ExpiryDate", card.expiryDate());
    return element;
  }

  private static Element lineItem(Document document, OrderLine line) {
    Element element = document.createElement("LineItem");
    append(element, "CategoryId", line.categoryId());
    append(element, "ProductId", line.productId());
    append(element, "ItemId", line.itemId());
    append(element, "LineNum", String.valueOf(line.lineNumber()));
    append(element, "Quantity", String.valueOf(line.quantity()));
    append(element, "UnitPrice", line.unitPrice().toPlainString());
    return element;
  }

  private static Element child(Element parent, String name) {
    Element element = parent.getOwnerDocument().createElement(name);
    parent.appendChild(element);
    return element;
  }

  /** Legacy: {@code XMLDocumentUtils.appendChild}, which wrote an empty element for no value. */
  private static void append(Element parent, String name, String value) {
    child(parent, name).setTextContent(value != null ? value : "");
  }
}
