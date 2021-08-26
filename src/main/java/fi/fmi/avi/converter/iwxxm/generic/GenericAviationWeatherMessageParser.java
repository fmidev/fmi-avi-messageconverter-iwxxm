package fi.fmi.avi.converter.iwxxm.generic;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class GenericAviationWeatherMessageParser<T> extends AbstractIWXXMParser<T, GenericAviationWeatherMessage> {

    private final Map<ScannerKey, GenericAviationWeatherMessageScanner> scanners;

    public GenericAviationWeatherMessageParser(Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
        this.scanners = scanners;
    }

    @Override
    public ConversionResult<GenericAviationWeatherMessage> convertMessage(final T input, final ConversionHints hints) {
        ConversionResult<GenericAviationWeatherMessage> retval = new ConversionResult<>();
        try {
            final Document doc = parseAsDom(input);

            retval = createAviationWeatherMessage(doc.getDocumentElement(), hints);

        } catch (Exception ce) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Error in parsing input", ce));
        }

        return retval;
    }

    protected ConversionResult<GenericAviationWeatherMessage> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        final ConversionResult<GenericAviationWeatherMessage> retval = new ConversionResult<>();
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        builder.setMessageFormat(GenericAviationWeatherMessage.Format.IWXXM);

        GenericAviationWeatherMessageScanner scanner = scanners.get(new ScannerKey(featureElement.getNamespaceURI(), featureElement.getLocalName()));
        try {
            if (scanner == null) {
                retval.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                        "Unknown message type '" + featureElement.getLocalName() + "', unable to parse as generic message"));
            } else {
                retval.addIssue(scanner.collectMessage(featureElement, xpath, builder));
            }

            try {
                final StringWriter sw = new StringWriter();
                final Result output = new StreamResult(sw);
                final TransformerFactory tFactory = TransformerFactory.newInstance();
                final Transformer transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                final DOMSource dsource = new DOMSource(featureElement);
                transformer.transform(dsource, output);
                builder.setOriginalMessage(sw.toString());
            } catch (final TransformerException e) {
                retval.addIssue(
                        new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unable to write the message content as " + "string",
                                e));
            }
            retval.setConvertedMessage(builder.build());
        } catch (final XPathExpressionException xpee) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                    "Error in parsing content as a GenericAviationWeatherMessage", xpee));
        }
        return retval;
    }

    @Override
    protected GenericAviationWeatherMessage createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<GenericAviationWeatherMessage> result, final ConversionHints hints) {
        return null;
    }

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        return null;
    }

    public static class FromString extends GenericAviationWeatherMessageParser<String> {
        public FromString(Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }

    public static class FromElement extends GenericAviationWeatherMessageParser<Element> {
        public FromElement(Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        protected Document parseAsDom(final Element input) throws ConversionException {
            final Document retval;
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            try {
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
                final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.newDocument();
                doc.appendChild(doc.importNode(input, true));
                retval = doc;
            } catch (final RuntimeException | ParserConfigurationException e) {
                throw new ConversionException("Error in parsing input to an XML document", e);
            }
            return retval;
        }
    }

    public static class FromDOM extends GenericAviationWeatherMessageParser<Document> {
        public FromDOM(Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class ScannerKey {
        private String namespaceURI;
        private String documentElementName;

        public ScannerKey(String namespaceURI, String documentElementName) {
            this.namespaceURI = namespaceURI;
            this.documentElementName = documentElementName;
        }

        public String getNamespaceURI() {
            return namespaceURI;
        }

        public String getDocumentElementName() {
            return documentElementName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ScannerKey that = (ScannerKey) o;
            return Objects.equals(getNamespaceURI(), that.getNamespaceURI()) && Objects.equals(getDocumentElementName(), that.getDocumentElementName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespaceURI(), getDocumentElementName());
        }
    }

}
