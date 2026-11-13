package com.petstore.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The seed document: the reference data a new, empty store starts with.
 *
 * <p>It is the legacy application's own data file, {@code Populate-UTF8.xml}, used unchanged. The
 * legacy application read it with a chain of SAX filters inside a servlet that anyone could call to
 * drop and reload the database. Here it is only read at startup, by the module that owns each part
 * of it: users, customers and the catalog.
 *
 * <p>The file declares external DTD fragments. They are deliberately not loaded: the parser is
 * configured so that it never fetches anything the document points to.
 */
public final class SeedDocument {

  private static final String XML_NAMESPACE = XMLConstants.XML_NS_URI;

  private final Element root;

  private SeedDocument(Element root) {
    this.root = root;
  }

  public static SeedDocument load(Resource resource) {
    try (InputStream input = resource.getInputStream()) {
      DocumentBuilder builder = hardenedFactory().newDocumentBuilder();
      // The document refers to DTD files by relative path; answer every such reference with
      // nothing instead of resolving it.
      builder.setEntityResolver(
          (publicId, systemId) -> new InputSource(new java.io.StringReader("")));
      Document document = builder.parse(input);
      return new SeedDocument(document.getDocumentElement());
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot read seed data from " + resource, exception);
    } catch (SAXException | ParserConfigurationException exception) {
      throw new IllegalStateException("Cannot parse seed data from " + resource, exception);
    }
  }

  private static DocumentBuilderFactory hardenedFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory;
  }

  /**
   * The elements reached by following the given element names from the root, for example {@code
   * elements("Catalog", "Categories", "Category")}.
   */
  public List<Element> elements(String... path) {
    List<Element> current = List.of(root);
    for (String name : path) {
      List<Element> next = new ArrayList<>();
      for (Element element : current) {
        next.addAll(children(element, name));
      }
      current = next;
    }
    return current;
  }

  /** Direct child elements with the given name, in document order. */
  public static List<Element> children(Element parent, String name) {
    List<Element> children = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && name.equals(element.getLocalName())) {
        children.add(element);
      }
    }
    return children;
  }

  /** The first direct child element with the given name. */
  public static Optional<Element> child(Element parent, String name) {
    return children(parent, name).stream().findFirst();
  }

  /** Trimmed text of the first direct child with the given name; absent if there is none. */
  public static Optional<String> text(Element parent, String name) {
    return child(parent, name).map(element -> element.getTextContent().trim());
  }

  /** Trimmed text of every direct child with the given name, in document order. */
  public static List<String> texts(Element parent, String name) {
    return children(parent, name).stream().map(element -> element.getTextContent().trim()).toList();
  }

  /**
   * The locale code of an element carrying {@code xml:lang}, in the form the data is stored under:
   * {@code en-US} becomes {@code en_US}. Same rule as the legacy {@code
   * XMLDBHandler.normalizeValue}.
   */
  public static String localeCode(Element element) {
    return element.getAttributeNS(XML_NAMESPACE, "lang").replace('-', '_');
  }
}
