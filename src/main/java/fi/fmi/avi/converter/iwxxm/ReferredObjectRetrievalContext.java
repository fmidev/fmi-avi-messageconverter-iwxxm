package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides functionality for resolving internal GML (xlink) references within the document.
 */
public class ReferredObjectRetrievalContext {
    private Map<String, Object> identifiedObjects = new HashMap<>();
    private Map<String, Map<QName, List<String>>> nilReasons = new HashMap<>();
    private Binder<Node> binder;

    /**
     * Constructs a resolver. Pre-scans the references used with the given document.
     *
     * @param dom
     *         the Document
     * @param jaxbBinder
     *         binder used to reference between DOM Elements and their JAXB Element counterparts.
     */
    public ReferredObjectRetrievalContext(final org.w3c.dom.Document dom, final Binder<Node> jaxbBinder) {
        Objects.requireNonNull(dom, "DOM document cannot be null");
        Objects.requireNonNull(jaxbBinder, "Binder cannot be null");
        this.binder = jaxbBinder;
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            HashMap<String, String> prefMap = new HashMap<>();
            prefMap.put("gml", "http://www.opengis.net/gml/3.2");
            prefMap.put("xlink", "http://www.w3.org/1999/xlink");
            prefMap.put("om", "http://www.opengis.net/om/2.0");
            prefMap.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            prefMap.put("iwxxm", "http://icao.int/iwxxm/2.1");
            SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
            xpath.setNamespaceContext(namespaces);

            // Force identifying om:result elements in the Node <-> JAXElemement mapping by unmarshalling them explicitly
            // (this are not bound to JAXBElements by default, due to being inside an anyType containing element).
            XPathExpression expr = xpath.compile("//om:result/*[@gml:id]");
            NodeList hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                Node hit = hits.item(i);
                binder.unmarshal(hit);
            }

            //Find the elements with gml:id which have actually been internally referred to within this document:
            expr = xpath.compile("//*[@gml:id and //*/@xlink:href=concat('#',@gml:id)]");
            hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                Node hit = hits.item(i);
                NamedNodeMap attrs = hit.getAttributes();
                Node idNode = attrs.getNamedItem("gml:id");
                Object elem = binder.getJAXBNode(hit);
                if (elem != null) {
                    if (elem instanceof JAXBElement) {
                        elem = ((JAXBElement) elem).getValue();
                    }
                    identifiedObjects.put(idNode.getNodeValue(), elem);
                }
            }

            //Build a lookup hash for nilReason attribute values.
            //These are not available using JAXB tooling because the xsi:nil="true" elements
            //evaluate to null objects.
            expr = xpath.compile("//*[*/@xsi:nil='true' and */@nilReason]");
            hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                Node parent = hits.item(i);
                String parentKey = getNodeParentPath(parent);
                Map<QName, List<String>> reasonsForThisParent = nilReasons.computeIfAbsent(parentKey, (key) -> new HashMap<>());
                NodeList children = parent.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node hit = children.item(j);
                    if (Node.ELEMENT_NODE == hit.getNodeType()) {
                        QName childKey = new QName(hit.getNamespaceURI(), hit.getLocalName());
                        List<String> reasonsForElement = reasonsForThisParent.computeIfAbsent(childKey,(key) -> new ArrayList<>());
                        NamedNodeMap attrs = hit.getAttributes();
                        if (attrs != null) {
                            Node nilReason = attrs.getNamedItem("nilReason");
                            if (nilReason != null) {
                                reasonsForElement.add(nilReason.getNodeValue());
                            } else {
                                reasonsForElement.add(null);
                            }
                        } else {
                            reasonsForElement.add(null);
                        }
                    }
                }
            }

        } catch (XPathExpressionException | JAXBException e) {
            throw new RuntimeException("Unexpected exception in finding identified GML objects", e);
        }
    }

    /**
     * Returns a referenced JAXB object with the given gml:id and of the given type.
     *
     * @param gmlId
     *         the gml:id of the object
     * @param clz
     *         class of type T
     * @param <T>
     *         the intended object type
     *
     * @return the referred JAXB element if available
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getReferredObject(final String gmlId, final Class<T> clz) {
        String key;
        if (gmlId.startsWith("#")) {
            key = gmlId.substring(1);
        } else {
            key = gmlId;
        }
        if (identifiedObjects.containsKey(key)) {
            Object o = identifiedObjects.get(key);
            if (clz.isAssignableFrom(o.getClass())) {
                return Optional.of((T) o);
            } else {
                throw new IllegalArgumentException("The referred object is not of type " + clz);
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> getNilReasonForNthChild(final Object jaxbElement, final QName elementName, int index) {
        Node n = this.binder.getXMLNode(jaxbElement);
        String pathKey = getNodeParentPath(n);
        Map<QName, List<String>> reasonsForParent = this.nilReasons.get(pathKey);
        if (reasonsForParent != null) {
            List<String> reasonsForElement = reasonsForParent.get(elementName);
            if (reasonsForElement != null && reasonsForElement.size() > index) {
                return Optional.of(reasonsForElement.get(index));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private String getNodeParentPath(final Node n) {
        Node parent = n.getParentNode();
        if (parent == null) {
            return null;
        }
        Node child = n;
        List<String> path = new ArrayList<>();
        StringBuilder sb;
        NodeList children;
        int childIndex = 0;
        while (parent != null) {
            Node idAttr = child.getAttributes().getNamedItem("gml:id");
            sb = new StringBuilder();
            sb.append(child.getNamespaceURI());
            sb.append(':');
            sb.append(child.getLocalName());
            sb.append('[');
            if (idAttr != null) {
                sb.append("@gml:id=");
                sb.append('\'');
                sb.append(idAttr.getNodeValue());
                sb.append('\'');
            }
            children = parent.getChildNodes();
            childIndex = -1;
            for (int i = 0; i < children.getLength(); i++) {
                Node c = children.item(i);
                if (Node.ELEMENT_NODE == c.getNodeType() && (c.getNamespaceURI() + c.getLocalName()).equals(child.getNamespaceURI() + child.getLocalName())) {
                    childIndex++;
                    if (c == child) {
                        break;
                    }
                }
            }
            if (idAttr == null) {
                sb.append(childIndex);
            }
            sb.append(']');
            path.add(sb.toString());
            child = parent;
            if (idAttr != null) {
                parent = null;
            } else {
                parent = parent.getParentNode();
            }
        }
        Collections.reverse(path);
        return String.join("/", path);
    }
    /**
     * Get the JAXBinder object associated with this instance.
     *
     * @return the binder.
     */
    public Binder<Node> getJAXBBinder() {
        return this.binder;
    }

    private static class SimpleNamespaceContext implements NamespaceContext {

        private Map<String, String> prefMap = new HashMap<>();

        public SimpleNamespaceContext(final Map<String, String> prefMap) {
            this.prefMap.putAll(prefMap);
        }

        @Override
        public String getNamespaceURI(final String prefix) {
            return prefMap.get(prefix);
        }

        @Override
        public String getPrefix(final String uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator getPrefixes(final String uri) {
            throw new UnsupportedOperationException();
        }

    }
}
