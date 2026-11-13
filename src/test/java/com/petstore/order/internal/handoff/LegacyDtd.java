package com.petstore.order.internal.handoff;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Validates a document against the DTDs of the legacy application.
 *
 * <p>The DTDs are unchanged copies of the legacy files ({@code PurchaseOrder.dtd} and the four it
 * includes), kept under {@code src/test/resources/legacy/dtd}. Every DTD reference in a document is
 * answered from there, whatever path it names, so nothing is fetched from anywhere else.
 */
final class LegacyDtd {

  private LegacyDtd() {}

  /** Parses and validates. Returns the document, or fails listing every violation. */
  static Document validate(String xml) {
    List<String> violations = new ArrayList<>();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver((publicId, systemId) -> {
        String fileName = systemId.substring(systemId.lastIndexOf('/') + 1);
        InputStream dtd = new ClassPathResource("legacy/dtd/" + fileName).getInputStream();
        InputSource source = new InputSource(dtd);
        source.setSystemId(systemId);
        return source;
      });
      builder.setErrorHandler(
          new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
              violations.add("warning: " + exception.getMessage());
            }

            @Override
            public void error(SAXParseException exception) {
              violations.add("error: " + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) {
              violations.add("fatal: " + exception.getMessage());
            }
          });
      Document document = builder.parse(new InputSource(new StringReader(xml)));
      if (!violations.isEmpty()) {
        throw new AssertionError("Not valid against the legacy DTD: " + violations);
      }
      return document;
    } catch (ParserConfigurationException | SAXException | IOException exception) {
      throw new AssertionError("Not valid against the legacy DTD: " + violations, exception);
    }
  }
}
