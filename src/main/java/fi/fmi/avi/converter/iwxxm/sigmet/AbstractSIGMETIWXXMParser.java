package fi.fmi.avi.converter.iwxxm.sigmet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolver;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.WSSIGMETImpl;
import icao.iwxxm21.SIGMETType;

public abstract class AbstractSIGMETIWXXMParser<T> extends IWXXMConverterBase implements AviMessageSpecificConverter<T, SIGMET> {

    /**
     * Returns the SIGMET input message as A DOM Document.
     *
     * @param input
     *         the raw input format
     *
     * @return the input parsed as DOM
     *
     * @throws ConversionException
     *         if an exception occurs while converting input to DOM
     */
    protected abstract Document parseAsDom(final T input) throws ConversionException;

    /**
     * Converts a SIGMET message into a SIGMET object.
     *
     * The IWXXM SIGMET message parsing is done in two phases:
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
    public ConversionResult<SIGMET> convertMessage(T input, ConversionHints hints) {
        ConversionResult<SIGMET> result = new ConversionResult<>();
        SIGMETType source;
        ReferredObjectRetrievalContext refCtx;

        try {
            Document dom = parseAsDom(input);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
            schemaFactory.setResourceResolver(resolver);
            //Secure processing does not allow "file" protocol loading for schemas:
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema iwxxmSchema = schemaFactory.newSchema(SIGMETType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));

            Binder<Node> binder = getJAXBContext().createBinder();

            //XML Schema validation upon JAXB unmarshal:
            binder.setSchema(iwxxmSchema);
            AbstractSIGMETIWXXMParser.IWXXMValidationEventHandler collector = new AbstractSIGMETIWXXMParser.IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom, SIGMETType.class).getValue();
            SIGMETProperties properties = new SIGMETProperties(source);

            for (ValidationEvent evt : collector.getEvents()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + evt.getMessage(), evt.getLinkedException()));
            }

            //Reset binder event handler after validation:
            binder.setEventHandler(null);

            refCtx = new ReferredObjectRetrievalContext(dom, binder);

            //Schematron validation:
            result.addIssue(IWXXMSIGMETScanner.validateAgainstIWXXMSchematron(dom, hints));

            //Other specific validation (using JAXB elements)
            result.addIssue(IWXXMSIGMETScanner.collectSIGMETProperties(source, refCtx, properties, hints));

            //Build the SIGMET:
            Optional<AviationCodeListUser.SigmetAirmetReportStatus> status = properties.get(SIGMETProperties.Name.STATUS,
                    AviationCodeListUser.SigmetAirmetReportStatus.class);
            if (!status.isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "SIGMET status not known, unable to " +
                        "proceed"));
                return result;
            }

            result.setConvertedMessage(createSIGMET(properties, status.get(), result));
        } catch (ConversionException ce) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Unable to parse input as an XML document", ce));
            return result;
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("Unexpected exception in parsing IWXXM content", e);
        }

        return result;
    }

    private SIGMET createSIGMET(final SIGMETProperties properties, final AviationCodeListUser.SigmetAirmetReportStatus status,
            final ConversionResult<SIGMET> result) {
        WSSIGMETImpl.Builder SIGMETBuilder = new WSSIGMETImpl.Builder();
        SIGMETBuilder.setStatus(status);

        return SIGMETBuilder.build();
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

    }}
