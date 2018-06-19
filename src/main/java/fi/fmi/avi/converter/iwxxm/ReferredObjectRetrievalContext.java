package fi.fmi.avi.converter.iwxxm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Preconditions;

/**
 * Provides functionality for resolving internal GML (xlink) references within the document.
 */
public class ReferredObjectRetrievalContext {
    private Map<String, Object> identifiedObjects = new HashMap<>();

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
        Preconditions.checkNotNull(dom, "DOM document cannot be null");
        Preconditions.checkNotNull(jaxbBinder, "Binder cannot be null");
        this.binder = jaxbBinder;
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            HashMap<String, String> prefMap = new HashMap<>();
            prefMap.put("gml", "http://www.opengis.net/gml/3.2");
            prefMap.put("xlink", "http://www.w3.org/1999/xlink");
            SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
            xpath.setNamespaceContext(namespaces);
            //Find the elements with gml:id which have actually been internally referred to within this document:
            XPathExpression expr = xpath.compile("//*[//*/@xlink:href=concat('#',@gml:id)]");
            NodeList hits = (NodeList) expr.evaluate(dom.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < hits.getLength(); i++) {
                Node hit = hits.item(i);
                NamedNodeMap attrs = hit.getAttributes();
                Node idNode = attrs.getNamedItem("gml:id");
                Object elem = jaxbBinder.getJAXBNode(hit);
                if (elem != null) {
                    if (elem instanceof JAXBElement) {
                        elem = ((JAXBElement) elem).getValue();
                    }
                    identifiedObjects.put(idNode.getNodeValue(), elem);
                }
            }
        } catch (XPathExpressionException e) {
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
        public Iterator<?> getPrefixes(final String uri) {
            throw new UnsupportedOperationException();
        }

    }
}
