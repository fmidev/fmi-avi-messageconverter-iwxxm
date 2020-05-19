package fi.fmi.avi.converter.iwxxm.v2_1.sigmet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolver;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Parser;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import icao.iwxxm21.SIGMETType;

public abstract class SIGMETIWXXMParser<T> extends AbstractIWXXM21Parser<T, SIGMET> {

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

    protected SIGMET createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<SIGMET> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        SIGMETType input;
        if (SIGMETType.class.isAssignableFrom(source.getClass())) {
            input = (SIGMETType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (SIGMETType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SIGMETType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SIGMET JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SIGMET JAXB element");
        }

        SIGMETProperties properties = new SIGMETProperties();

        //Other specific validation (using JAXB elements)
        result.addIssue(SIGMETIWXXMScanner.collectSIGMETProperties(input, refCtx, properties, hints));
        //Build the TAF:
        Optional<AviationCodeListUser.SigmetAirmetReportStatus> status = properties.get(SIGMETProperties.Name.STATUS,
                AviationCodeListUser.SigmetAirmetReportStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "SIGMET status not known, unable to " + "proceed"));
            return null;
        }

        SIGMETImpl.Builder sigmetBuilder = SIGMETImpl.builder();
        sigmetBuilder.setStatus(status.get());
        properties.get(SIGMETProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent(sigmetBuilder::setIssueTime);

        return sigmetBuilder.build();

    }

    public ConversionResult<SIGMET> convertMessage2(T input, ConversionHints hints) {
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
            SIGMETIWXXMParser.IWXXMValidationEventHandler collector = new SIGMETIWXXMParser.IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom, SIGMETType.class).getValue();
            SIGMETProperties properties = new SIGMETProperties();

            //Other specific validation (using JAXB elements)
            //       result.addIssue(SIGMETIWXXMScanner.collectSIGMETProperties(input, refCtx, properties, hints));

            for (ValidationEvent evt : collector.getEvents()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + evt.getMessage(), evt.getLinkedException()));
            }

            //Reset binder event handler after validation:
            binder.setEventHandler(null);

            refCtx = new ReferredObjectRetrievalContext(dom, binder);

            //Schematron validation:
            //TODO           result.addIssue(SIGMETIWXXMScanner.validateAgainstIWXXMSchematron(dom, hints));

            //Other specific validation (using JAXB elements)
            result.addIssue(SIGMETIWXXMScanner.collectSIGMETProperties(source, refCtx, properties, hints));

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
        SIGMETImpl.Builder SIGMETBuilder = new SIGMETImpl.Builder();
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

    }

    public static class FromDOM extends SIGMETIWXXMParser<Document> {

        /**
         * This implementation simple passes the input Document through.
         *
         * @param input the raw input format
         * @return the parsed DOM
         */
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
            return input;
        }
    }

    public static class FromString extends SIGMETIWXXMParser<String> {
        /**
         * Returns the TAF input message as A DOM Document.
         *
         * @param input the XML Document input as a String
         * @return the input parsed as DOM
         * @throws ConversionException if an exception occurs while converting input to DOM
         */
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }
}
