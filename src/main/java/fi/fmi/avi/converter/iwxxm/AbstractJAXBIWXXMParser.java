package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import icao.iwxxm21.ReportType;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractJAXBIWXXMParser<T, S extends AviationWeatherMessageOrCollection> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S> {

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
            //TODO: move into a an IWXXM 2.0 common abstract class when available
            final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
            schemaInfo.addSchemaSource(ReportType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
            schemaInfo.setSchematronRules(ReportType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));

            Binder<Node> binder = getJAXBContext().createBinder();

            //XML Schema validation upon JAXB unmarshal:
            binder.setSchema(schemaInfo.getSchema());
            IWXXMValidationEventHandler collector = new IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom);

            List<ValidationEvent> events = collector.getEvents();
            if (events.isEmpty()) {
                //Reset binder event handler after validation:
                binder.setEventHandler(null);

                refCtx = new ReferredObjectRetrievalContext(dom, binder);

                //Schematron validation:
                result.addIssue(validateAgainstIWXXMSchematron(dom, schemaInfo, hints));
                try {
                    result.setConvertedMessage(createPOJO(source, refCtx, result, hints));
                } catch (IllegalStateException ise) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "All mandatory information for "
                            + "constructing a message object was not available", ise));
                }
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
