package com.petstore.order.internal.handoff;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * A stand-in for the order-processing application, which is not part of this migration.
 *
 * <p>In the legacy system the message-driven bean {@code PurchaseOrderMDB} of the "opc" application
 * took purchase orders off the queue, stored them and started their approval. This stub only takes
 * them off the queue and writes one line to the log, so that the hand-off can be watched end to end
 * when the storefront runs on its own. With a real consumer on the queue it is switched off ({@code
 * petstore.order.handoff.stub-consumer=false}).
 */
@Component
@ConditionalOnExpression(
    "'${petstore.order.handoff.transport:jms}' == 'jms'"
        + " and ${petstore.order.handoff.stub-consumer:true}")
class OrderProcessingStub {

  private static final Logger log = LoggerFactory.getLogger(OrderProcessingStub.class);

  @JmsListener(destination = "${petstore.order.handoff.queue:jms/opc/OrderQueue}")
  void receive(String purchaseOrderXml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
      Document document = builder.parse(new InputSource(new StringReader(purchaseOrderXml)));
      log.info(
          "Order processing (stub) received purchase order {} from user {}, total {}",
          text(document, "OrderId"),
          text(document, "UserId"),
          text(document, "TotalPrice"));
    } catch (Exception unreadable) {
      log.warn(
          "Order processing (stub) received a message it could not read: {}",
          unreadable.toString());
    }
  }

  private static String text(Document document, String element) {
    return document.getElementsByTagName(element).item(0).getTextContent();
  }
}
