package fi.fmi.avi.converter.iwxxm.generic;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public abstract class GenericAviationWeatherMessageParser<T> extends AbstractIWXXMParser<T, GenericAviationWeatherMessage> {

    private final Map<ScannerKey, GenericAviationWeatherMessageScanner> scanners;

    protected GenericAviationWeatherMessageParser(final Map<ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
        this.scanners = requireNonNull(scanners, "scanners");
    }

    protected static void collectTranslationStatus(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final String translatedBulletinID = xpath.compile("@translatedBulletinID").evaluate(featureElement);
        builder.setTranslated(translatedBulletinID != null && !translatedBulletinID.isEmpty());
    }

    @Override
    public ConversionResult<GenericAviationWeatherMessage> convertMessage(final T input, final ConversionHints hints) {
        ConversionResult<GenericAviationWeatherMessage> retval = new ConversionResult<>();
        try {
            final Document doc = parseAsDom(input);

            retval = createAviationWeatherMessage(doc.getDocumentElement(), hints);

        } catch (final Exception ce) {
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
        builder.setNullableXMLNamespace(featureElement.getNamespaceURI());

        final GenericAviationWeatherMessageScanner scanner = scanners.get(new ScannerKey(featureElement.getLocalName()));
        try {
            collectTranslationStatus(featureElement, xpath, builder);
            if (scanner == null) {
                retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Unknown message type '" + featureElement.getLocalName() + "'"));
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
        public FromString(final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }

    public static class FromElement extends GenericAviationWeatherMessageParser<Element> {
        public FromElement(final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        @Override
        protected Document parseAsDom(final Element input) throws ConversionException {
            try {
                return copyAsDocument(input);
            } catch (final RuntimeException | ParserConfigurationException e) {
                throw new ConversionException("Error in parsing input to an XML document", e);
            }
        }
    }

    public static class FromDOM extends GenericAviationWeatherMessageParser<Document> {
        public FromDOM(final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scanners) {
            super(scanners);
        }

        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class ScannerKey {
        private final String documentElementName;

        public ScannerKey(final String documentElementName) {
            this.documentElementName = documentElementName;
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
            return Objects.equals(getDocumentElementName(), that.getDocumentElementName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDocumentElementName());
        }
    }

}
