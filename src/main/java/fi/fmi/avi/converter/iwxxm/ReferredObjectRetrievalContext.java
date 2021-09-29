package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
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
    private final Map<String, Object> identifiedObjects = new HashMap<>();
    private final Map<String, Map<QName, List<String>>> nilReasons = new HashMap<>();
    private final Binder<Node> binder;

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
            final XPathFactory factory = XPathFactory.newInstance();
            final XPath xpath = factory.newXPath();
            final IWXXMNamespaceContext namespaceContext = new IWXXMNamespaceContext();
            xpath.setNamespaceContext(namespaceContext);

            // Force identifying om:result elements in the Node <-> JAXElemement mapping by unmarshalling them explicitly
            // (this are not bound to JAXBElements by default, due to being inside an anyType containing element).
            XPathExpression expr = xpath.compile("//om:result/*[@gml:id]");
            NodeList hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                final Node hit = hits.item(i);
                binder.unmarshal(hit);
            }

            //Find the elements with gml:id which have actually been internally referred to within this document:
            expr = xpath.compile("//*[@gml:id and //*/@xlink:href=concat('#',@gml:id)]");
            hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                final Node hit = hits.item(i);
                final NamedNodeMap attrs = hit.getAttributes();
                final Node idNode = attrs.getNamedItem("gml:id");
                Object elem = binder.getJAXBNode(hit);
                if (elem != null) {
                    if (elem instanceof JAXBElement) {
                        elem = ((JAXBElement<?>) elem).getValue();
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
                final Node parent = hits.item(i);
                final String parentKey = getNodeParentPath(parent);
                final Map<QName, List<String>> reasonsForThisParent = nilReasons.computeIfAbsent(parentKey, key -> new HashMap<>());
                final NodeList children = parent.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    final Node hit = children.item(j);
                    if (Node.ELEMENT_NODE == hit.getNodeType()) {
                        final QName childKey = new QName(hit.getNamespaceURI(), hit.getLocalName());
                        final List<String> reasonsForElement = reasonsForThisParent.computeIfAbsent(childKey, key -> new ArrayList<>());
                        final NamedNodeMap attrs = hit.getAttributes();
                        if (attrs != null) {
                            final Node nilReason = attrs.getNamedItem("nilReason");
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
        final String key;
        if (gmlId.startsWith("#")) {
            key = gmlId.substring(1);
        } else {
            key = gmlId;
        }
        if (identifiedObjects.containsKey(key)) {
            final Object o = identifiedObjects.get(key);
            if (clz.isAssignableFrom(o.getClass())) {
                return Optional.of((T) o);
            } else {
                throw new IllegalArgumentException("The referred object is not of type " + clz);
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> getNilReasonForNthChild(final Object jaxbElement, final QName elementName, final int index) {
        final Node n = this.binder.getXMLNode(jaxbElement);
        final String pathKey = getNodeParentPath(n);
        final Map<QName, List<String>> reasonsForParent = this.nilReasons.get(pathKey);
        if (reasonsForParent != null) {
            final List<String> reasonsForElement = reasonsForParent.get(elementName);
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
        final List<String> path = new ArrayList<>();
        StringBuilder sb;
        NodeList children;
        int childIndex;
        while (parent != null) {
            final Node idAttr = child.getAttributes().getNamedItem("gml:id");
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
                final Node c = children.item(i);
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
}
