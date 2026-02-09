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
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import java.util.List;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractIWXXMAixm511WxParser<T, S extends AviationWeatherMessageOrCollection> extends AbstractIWXXMParser<T, S> {

    /**
     * Converts a message into a POJO.
     * <p>
     * The IWXXM TAF message parsing is done in two phases:
     * &lt;ul&gt;
     * &lt;li&gt;In the first phase the IWXXM DOM document is validated against its
     * XML Schema and Schematron rules and (if validation passed), the JAXB objects created from the
     * DOM scanned for all the necessary property values for creating MessageConverter Java model objects. Additional validation
     * for the document structure and content is also done in this phase.&lt;/li&gt;
     * &lt;li&gt;In the second phase the model objects are created and populated from the property data
     * collected in the first phase.&lt;/li&gt;
     * &lt;/ul&gt;
     *
     * @param input input message
     * @param hints parsing hints
     * @return the conversion result
     */
    @Override
    public ConversionResult<S> convertMessage(final T input, final ConversionHints hints) {
        final ConversionResult<S> result = new ConversionResult<>();
        final Object source;
        final ReferredObjectRetrievalContext refCtx;

        try {
            final Document dom = parseAsDom(input);
            final XMLSchemaInfo schemaInfo = getSchemaInfo();
            final Binder<Node> binder = AbstractIWXXMAixm511WxSerializer.getAixm511WxJAXBContext().createBinder();

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
                            "All mandatory information for " + "constructing a message object was not available", ise));
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
