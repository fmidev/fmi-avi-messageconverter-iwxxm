package fi.fmi.avi.converter.iwxxm;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationWeatherMessage;
import icao.iwxxm21.ReportType;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractIWXXMParser<T, S extends AviationWeatherMessage> extends IWXXMConverterBase implements AviMessageSpecificConverter<T, S> {

    private static Templates iwxxmTemplates;

    protected static Document parseStringToDOM(final String input) throws ConversionException {
        Document retval = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
            retval = db.parse(bais);
        } catch (Exception e) {
            throw new ConversionException("Error in parsing input as to an XML document", e);
        }
        return retval;
    }

    /**
     * Returns the TAF input message as A DOM Document.
     *
     * @param input
     *         the XML Document input as a String
     *
     * @return the input parsed as DOM
     *
     * @throws ConversionException
     *         if an exception occurs while converting input to DOM
     */
    protected abstract Document parseAsDom(final T input) throws ConversionException;

    protected abstract S createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<S> result,
            final ConversionHints hints);

    /**
     * Converts a message into a POJO.
     *
     * The IWXXM TAF message parsing is done in two phases:
     * &lt;ul&gt;
     * &lt;li&gt;In the first phase the IWXXM DOM document is validated against the it's
     * XML Schema and Schematron rules and (if validation passed), the JAXB objects created from the the
     * DOM scanned for all the necessary property values for creating MessageConverter Java model objects. Additional validation
     * for the document structure and content is also done in this phase.&lt;/li&gt;
     * &lt;li&gt;In the second phase the model objects are created and populated from the property data
     * collected in the first phase.&lt;/li&gt;
     * &lt;/ul&gt;
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the conversion result
     */
    @Override
    public ConversionResult<S> convertMessage(T input, ConversionHints hints) {
        ConversionResult<S> result = new ConversionResult<>();
        Object source;
        ReferredObjectRetrievalContext refCtx;

        try {
            Document dom = parseAsDom(input);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
            schemaFactory.setResourceResolver(resolver);
            //Secure processing does not allow "file" protocol loading for schemas:
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema iwxxmSchema = schemaFactory.newSchema(ReportType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));

            Binder<Node> binder = getJAXBContext().createBinder();

            //XML Schema validation upon JAXB unmarshal:
            binder.setSchema(iwxxmSchema);
            IWXXMValidationEventHandler collector = new IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom);

            List<ValidationEvent> events = collector.getEvents();
            if (events.isEmpty()) {
                //Reset binder event handler after validation:
                binder.setEventHandler(null);

                refCtx = new ReferredObjectRetrievalContext(dom, binder);

                //Schematron validation:
                result.addIssue(validateAgainstIWXXMSchematron(dom, hints));

                result.setConvertedMessage(createPOJO(source, refCtx, result, hints));
            } else {
                for (ValidationEvent evt : collector.getEvents()) {
                    result.addIssue(
                            new ConversionIssue(ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + evt.getMessage(), evt.getLinkedException()));
                }
            }

        } catch (ConversionException ce) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Unable to parse input as an XML document", ce));
            return result;
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("Unexpected exception in parsing IWXXM content", e);
        }

        return result;

    }

    /**
     * Checks the DOM Document against the official IWXXM 2.1.1 Schematron validation rules.
     * Uses a pre-generated XLS transformation file producing the Schematron SVRL report.
     *
     * @param input
     *         IWXXM message Document
     * @param hints
     *         conversion hints to guide the validaton
     *
     * @return the list of Schematron validation issues (failed asserts)
     */
    protected static IssueList validateAgainstIWXXMSchematron(final Document input, final ConversionHints hints) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new IWXXMNamespaceContext());
        IssueList retval = new IssueList();
        try {
            DOMResult schematronOutput = new DOMResult();
            Transformer transformer = getIwxxmTemplates().newTransformer();
            DOMSource dSource = new DOMSource(input);
            transformer.transform(dSource, schematronOutput);
            NodeList failedAsserts = (NodeList) xPath.evaluate("//svrl:failed-assert/svrl:text", schematronOutput.getNode(), XPathConstants.NODESET);
            if (failedAsserts != null) {
                for (int i = 0; i < failedAsserts.getLength(); i++) {
                    Node node = failedAsserts.item(i).getFirstChild();
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Failed Schematron assertation: " + node.getNodeValue());
                }
            }
        } catch (TransformerException | XPathExpressionException e) {
            throw new RuntimeException("Unable to apply XSLT pre-compiled Schematron validation rules to the document to validate", e);
        }
        return retval;
    }

    /*
       Performance optimization: use a pre-compiled the Templates object
       for running the XSL transformations required for IWXXM Schematron
       validation. This makes each validation 3-4 times faster.
   */
    private synchronized static Templates getIwxxmTemplates() {
        if (iwxxmTemplates == null) {
            TransformerFactory tFactory = TransformerFactory.newInstance();

            try {
                iwxxmTemplates = tFactory.newTemplates(
                        new StreamSource(ReportType.class.getClassLoader().getResourceAsStream("schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl")));
            } catch (Exception e) {
                throw new RuntimeException("Unable to read XSL file for IWXXM 2.1.1 Schematron validation, make sure the the file exists in " + " classpath "
                        + "location 'schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl' ");
            }
        }
        return iwxxmTemplates;
    }

    private static class IWXXMValidationEventHandler implements ValidationEventHandler {

        private List<ValidationEvent> events = new ArrayList<>();

        @Override
        public boolean handleEvent(final ValidationEvent event) {
            events.add(event);
            return true;
        }

        List<ValidationEvent> getEvents() {
            return events;
        }

    }
}
