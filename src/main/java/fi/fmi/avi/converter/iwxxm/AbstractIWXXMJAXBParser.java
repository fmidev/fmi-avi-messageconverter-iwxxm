package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import java.util.List;

/**
 * Shared schema validation, JAXB unmarshalling, and model-construction pipeline.
 */
public abstract class AbstractIWXXMJAXBParser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMParser<T, S> {

    protected abstract JAXBContext getJAXBContext() throws JAXBException;

    /**
     * Unmarshals with XML Schema validation, collects Schematron issues, and delegates
     * model construction to the message parser. That parser retains control over
     * whether collected issues prevent model construction.
     *
     * @param input input message
     * @param hints parsing hints
     * @return the conversion result, including validation and construction issues
     */
    @Override
    public ConversionResult<S> convertMessage(final T input, final ConversionHints hints) {
        final ConversionResult<S> result = new ConversionResult<>();
        final Object source;
        final ReferredObjectRetrievalContext refCtx;

        try {
            final Document dom = parseAsDom(input);
            final XMLSchemaInfo schemaInfo = getSchemaInfo();
            final Binder<Node> binder = getJAXBContext().createBinder();

            //XML Schema validation upon JAXB unmarshal:
            binder.setSchema(schemaInfo.getSchema());
            final IWXXMValidationEventHandler collector = new IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom);

            final List<ValidationEvent> events = collector.getEvents();
            if (events.isEmpty()) {
                //Reset binder event handler after validation:
                binder.setEventHandler(null);

                refCtx = new ReferredObjectRetrievalContext(dom, binder);

                //Schematron validation:
                result.addIssue(validateAgainstIWXXMSchematron(dom, schemaInfo));
                try {
                    result.setConvertedMessage(createPOJO(source, refCtx, result, hints));
                } catch (final IllegalStateException ise) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "All mandatory information for constructing a message object was not available", ise));
                }
            } else {
                for (final ValidationEvent evt : collector.getEvents()) {
                    result.addIssue(
                            new ConversionIssue(ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + evt.getMessage(), evt.getLinkedException()));
                }
            }

        } catch (final ConversionException ce) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Unable to parse input as an XML document", ce));
            return result;
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("Unexpected exception in parsing IWXXM content", e);
        }

        return result;
    }

}
