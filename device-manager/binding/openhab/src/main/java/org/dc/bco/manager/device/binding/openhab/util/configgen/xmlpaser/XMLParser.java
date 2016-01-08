package org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import nu.xom.*;
import org.slf4j.LoggerFactory;

public class XMLParser {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(XMLParser.class);

    public enum NumberOfNodes {

        ARBITRARY, AT_LEAST_ONE, AT_MOST_ONE, EXACT_ONE
    };

    /**
     *
     * @param fileName
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     * @throws IOException
     */
    public synchronized static Document createDocumentFromFile(String fileName) throws XMLParsingException, IOException {
        Document doc = null;
        try {
            doc = new Builder().build(new File(fileName));
        } catch (ParsingException e) {
            throw new XMLParsingException("Can not parse file " + fileName, e);
        } catch (IOException e) {
            throw new IOException("Can not access file " + fileName, e);
        }
        return doc;
    }

    /**
     *
     * @param file
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     * @throws IOException
     */
    public synchronized static Document createDocumentFromFile(File file) throws XMLParsingException, IOException {
        Document doc = null;
        try {
            doc = new Builder().build(file);
        } catch (ParsingException e) {
            throw new XMLParsingException("Can not parse file " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new IOException("Can not access file " + file.getAbsolutePath(), e);
        }
        return doc;
    }

    /**
     *
     * @param xmlDoc
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static Document createDocumentFromString(String xmlDoc) throws XMLParsingException {
        try {
            return new Builder().build(xmlDoc.trim(), "");
        } catch (ParsingException | IOException e) {
            throw new XMLParsingException("Can not parse string " + xmlDoc, e);
        }
    }

    /**
     *
     * @param xmlDoc
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static Element createElementFromString(String xmlDoc) throws XMLParsingException {
        try {
            return new Builder().build(xmlDoc.trim(), "").getRootElement();
        } catch (ParsingException | IOException e) {
            throw new XMLParsingException("Can not parse string " + xmlDoc, e);
        }
    }

    public static Iterable<Element> toIterable(final Elements elements) {
        return () -> new Iterator<Element>() {
            private int i = 0;
            private int size = elements.size();

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Element next() {
                return elements.get(i++);
            }

            @Override
            public void remove() {
                throw new AssertionError("Not supported for Elementterator!");
            }
        };
    }

    public static Iterable<Node> toIterable(final Nodes nodes) {
        return () -> new Iterator<Node>() {
            private int i = 0;
            private int size = nodes.size();

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Node next() {
                return nodes.get(i++);
            }

            @Override
            public void remove() {
                nodes.remove(i++);
            }
        };
    }

    public static Iterable<Element> toIterableElement(final Nodes nodes) {
        return () -> new Iterator<Element>() {
            private int i = 0;
            private int size = nodes.size();

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Element next() {
                return (Element) nodes.get(i++);
            }

            @Override
            public void remove() {
                nodes.remove(i++);
            }
        };
    }

    /**
     *
     * @param nodeName
     * @param node
     * @param expectedNumberOfNodes
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static Nodes extractNodesByNodeName(String nodeName, Node node, NumberOfNodes expectedNumberOfNodes) throws XMLParsingException {
        return extractNodesByXpath(".//" + nodeName, node, expectedNumberOfNodes);
    }

    public static Element extractOneChildElementByXPath(String xPath, Node node) throws MissingNodeException, OverissueNodeException, NotOneNodeException {
        return (Element) extractNodesByXpath(xPath, node, NumberOfNodes.EXACT_ONE).get(0);
    }

    public static Nodes extractNodesByXpath(String xpath, Node node, NumberOfNodes expectedNumberOfNodes) throws MissingNodeException, OverissueNodeException, NotOneNodeException {
        Nodes nodes = node.query(xpath);
        switch (expectedNumberOfNodes) {
        case ARBITRARY: {
            break;
        }
        case AT_LEAST_ONE: {
            if (nodes.size() < 1) {
                throw new MissingNodeException(xpath, node);
            }
            break;

        }
        case AT_MOST_ONE: {
            if (nodes.size() > 1) {
                throw new OverissueNodeException(xpath, nodes, node);
            }
            break;
        }
        case EXACT_ONE: {
            if (nodes.size() != 1) {
                throw new NotOneNodeException(xpath, nodes, node);
            }
            break;
        }
        default:
            throw new AssertionError("Found not handled value[" + expectedNumberOfNodes.name() + "]!");
        }
        return nodes;
    }

    /**
     *
     * @param nodeName
     * @param node
     * @param expectedNumberOfNodes
     * @param throwException
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     *
     */
    public static Nodes extractNodesByNodeName(String nodeName, Node node, int expectedNumberOfNodes, boolean throwException) throws XMLParsingException {
        return extractNodesByXpath(".//" + nodeName, node, expectedNumberOfNodes, throwException);
    }

    public static Nodes extractNodesByXpath(String xpath, Node node, int expectedNumberOfNodes, boolean throwException) throws XMLParsingException {
        Nodes nodes = node.query(xpath);
        if (nodes.size() == expectedNumberOfNodes) {
            return nodes;
        }

        if (throwException) {
            logger.error("Expected " + expectedNumberOfNodes + " to be found with xPath " + xpath + ", found " + nodes.size());
            throw new XMLParsingException("Expected " + expectedNumberOfNodes + " to be found with xPath " + xpath + ", found " + nodes.size());
        }
        return nodes;
    }

    /**
     * <p>
     * Returns the first Attribute with name attrName from Document doc. Uses xPath "//
     *
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     * @param throwException flag set throw exception if no such Attribute can be found.	<br>
     * @param attrName
     * @param doc
     * @return
     */
    public static String extractAttributeValue(String attrName, Node doc, boolean throwException) throws XMLParsingException {
        String xpath = "descendant-or-self::*/@" + attrName;
        Nodes nodes = doc.query(xpath);
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof Attribute) {
                return nodes.get(i).getValue();
            }
        }
        if (throwException) {
            throw new XMLParsingException("No Attribute " + attrName + " in document:\n" + doc.toXML());
        } else {
            return null;
        }
    }

    /**
     *
     * @param nodeName
     * @param doc
     * @param expectedNumberOfNodes
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static HashSet<String> extractValues(String nodeName, Node doc, NumberOfNodes expectedNumberOfNodes) throws XMLParsingException {
        HashSet<String> names = new HashSet<>();
        Nodes nodes = extractNodesByNodeName(nodeName, doc, expectedNumberOfNodes);
        for (int i = 0; i < nodes.size(); i++) {
            names.add(nodes.get(i).getValue());
        }
        return names;
    }

    /**
     *
     * @param nodeName
     * @param doc
     * @param expectedNumberOfNodes
     * @param throwException
     * @return
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static HashSet<String> extractValues(String nodeName, Node doc, int expectedNumberOfNodes, boolean throwException) throws XMLParsingException {
        HashSet<String> names = new HashSet<>();
        Nodes nodes = extractNodesByNodeName(nodeName, doc, expectedNumberOfNodes, throwException);
        for (int i = 0; i < nodes.size(); i++) {
            names.add(nodes.get(i).getValue());
        }
        return names;
    }

    /**
     * Does nothing if nodes contains at least one node. Throws InvalidCfgDocException otherwise.
     *
     * @param nodes
     * @param nodeNameForDebugging
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public void existenceCheck(Nodes nodes, String nodeNameForDebugging) throws XMLParsingException {
        if (nodes.size() == 0) {
            throw new XMLParsingException("Message doesn't contain a " + nodeNameForDebugging + "node!");
        }
    }

    /**
     * Does nothing if only one of nodes1 or nodes2 contains more than zero nodes. Throws InvalidCfgDocException otherwise.
     *
     * @param nodes1
     * @param nodes2
     * @param nodeTypeForDebugging
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static void xorCheck(Nodes nodes1, Nodes nodes2, String nodeTypeForDebugging) throws XMLParsingException {
        if (nodes1.size() > 0 && nodes2.size() > 0) {
            throw new XMLParsingException("Message contains more than one " + nodeTypeForDebugging + " node. Only one permitted.");
        }
    }

    /**
     * Does nothing if only one of nodes1, nodes2 or nodes3 contains more than zero nodes. Throws InvalidCfgDocException otherwise.
     *
     *
     * @param nodes1
     * @param nodes2
     * @param nodes3
     * @param nodeTypeForDebugging
     * @throws org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParsingException
     */
    public static void xorCheck(Nodes nodes1, Nodes nodes2, Nodes nodes3, String nodeTypeForDebugging) throws XMLParsingException {
        if (nodes1.size() > 0 && nodes2.size() > 0 && nodes3.size() > 0) {
            throw new XMLParsingException("Message contains more than one " + nodeTypeForDebugging + " node. Only one permitted.");
        }
    }

    /**
     *
     * @param doc
     * @return
     */
    public static String serialize(Node doc) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Serializer ser = new Serializer(outStream);
        ser.setIndent(3);
        try {
            ser.write(doc.getDocument());
            return outStream.toString();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Couldn't transform doc to prettyXMLString. Returning old document.");
            return doc.toXML();
        }
    }

    /**
     * Creates a pretty formatted version of doc. Note: does not remove line breaks!
     *
     * @param doc
     * @return
     */
    public static Node normalizeFormatting(Node doc) {
        try {
            return createDocumentFromString(normalizeFormattingAsString(doc));
        } catch (XMLParsingException e) {
            e.printStackTrace();
            logger.error("Couldn't normalize formatting. Returning old document.");
            return doc;
        }
    }

    public static String normalizeFormattingAsString(Node doc) {
        Node copy = doc.copy();
        try {
            doc = skipNlTabWs(doc);
            return serialize(doc);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Couldn't normalize formatting. Returning old document.");
            return copy.toXML();
        }
    }

    /**
     *
     * @param doc
     * @return
     */
    public static Document normalizeFormatting(Document doc) {
        return (Document) normalizeFormatting((Node) doc);
    }

    /**
     *
     * @param docs
     * @return
     */
    public static Nodes normalizeFormatting(Nodes docs) {
        Nodes n = new Nodes();
        for (int i = 0; i < docs.size(); i++) {
            n.append(normalizeFormatting(docs.get(i)));
        }
        return n;
    }

    /**
     *
     * @param oldDoc
     * @return
     */
    public static Node skipNlTabWs(Node oldDoc) {
        Node copy = oldDoc.copy();
        try {
            String oldString = oldDoc.toXML();
            String newString = oldString.replace("\n", "");
            newString = newString.replace("\t", "");
            newString = newString.trim();
            return createDocumentFromString(newString);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            logger.error("Couldn't skipNlTabWs. Returning old document.");
            return copy;
        }
    }

    protected Element getElementFromXPath(String xPath, Document document) {
        Nodes regNodes = document.query(xPath);
        if (regNodes.size() != 1) {
            logger.error("Invalide data from XPath[" + xPath + "]!");
            return null;
        }
        return (Element) regNodes.get(0);
    }

    /**
     *
     * @param xPath
     * @param document
     * @return
     * @deprecated realy bad implementation!
     */
    @Deprecated
    protected static Iterator<Element> getElementsFromXPath(String xPath, Document document) {
        Nodes regNodes = document.query(xPath);

        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < regNodes.size(); i++) {
            try {
                elements.add((Element) regNodes.get(i));
            } catch (ClassCastException e) {
                logger.error("Invalide data from XPath[" + xPath + "]!", e);
            }
        }
        return elements.iterator();
    }

    public static boolean parseBooleanElementValue(String elementName, Element sourceElement) {
        try {
            return Boolean.parseBoolean(XMLParser.parseOneChildElement(elementName, sourceElement).getValue());
        } catch (Exception ex) {
            return false;
        }
    }

    public static Element parseOneChildElement(String elementName, Element sourceElement) throws MissingElementException, OverissueElementException {
        Elements elements = sourceElement.getChildElements(elementName);
        if (elements.size() == 0) {
            throw new MissingElementException(elementName, sourceElement);
        } else if (elements.size() != 1) {
            throw new OverissueElementException(elementName, elements, sourceElement);
        }
        return elements.get(0);
    }

    public static String parseAttributeValue(String attributeName, Element sourceElement) throws MissingAttributeException {
        Attribute attribute = sourceElement.getAttribute(attributeName);
        if (attribute == null) {
            throw new MissingAttributeException(attributeName, sourceElement);
        }
        return attribute.getValue();
    }

    public static boolean parseBooleanAttributeValue(String attributeName, Element sourceElement) {
        try {
            return Boolean.parseBoolean(parseAttributeValue(attributeName, sourceElement));
        } catch (MissingAttributeException ex) {
            return false;
        }
    }

    public static int parseIntegerAttributeValue(String attributeName, Element sourceElement) throws MissingAttributeException, XMLParsingException {
        try {
            return Integer.parseInt(parseAttributeValue(attributeName, sourceElement));
        } catch (NumberFormatException e) {
            throw new XMLParsingException("Could not parse integer attribute[" + attributeName + "] for element[" + sourceElement.getQualifiedName() + "].", e);
        }
    }

    public static <T extends Enum<T>> T parseEnumAttributeValue(String attributeName, Element sourceElement, Class<T> enumType) throws MissingAttributeException, XMLParsingException {
        String attributeValue = parseAttributeValue(attributeName, sourceElement);
        try {
            return Enum.valueOf(enumType, attributeValue);
        } catch (java.lang.IllegalArgumentException e) {
            throw new XMLParsingException("Could not resolve enum value[" + attributeValue + "] out of attribute[" + attributeName + "] for element[" + sourceElement.getQualifiedName() + "].", e);
        }
    }

    public static Elements parseChildElements(Element sourceElement, String childElementName, boolean atLeastOne) throws XMLParsingException {
        Elements childElements = sourceElement.getChildElements(childElementName);
        if (atLeastOne && childElements.size() == 0) {
            throw new XMLParsingException("Missing at least one element[" + childElementName + "] for parent element[" + sourceElement.getQualifiedName() + "].");
        }
        return childElements;
    }

    public static String fixXML(String xml) {
        xml = xml.replaceAll("zdef-?[^:]+:", ""); // [\\d]+:
        return xml;
    }
}
