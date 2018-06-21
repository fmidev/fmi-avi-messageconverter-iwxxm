package fi.fmi.avi.converter.iwxxm.taf;

import java.time.ZonedDateTime;
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
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.IWXXMSchemaResourceResolver;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.GeoPosition;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFSurfaceWind;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import fi.fmi.avi.model.taf.immutable.TAFReferenceImpl;
import icao.iwxxm21.TAFType;

/**
 * Common functionality for conversions related to reading IWXXM TAFs.
 *
 * @param <T> the type of the raw input message
 */
public abstract class AbstractTAFIWXXMParser<T> extends IWXXMConverterBase implements AviMessageSpecificConverter<T, TAF> {

    /**
     * Returns the TAF input message as A DOM Document.
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
     * Converts a TAF message into a TAF object.
     *
     * The IWXXM TAF message parsing is done in two phases:
     * &lt;ul&gt;
     *      &lt;li&gt;In the first phase the IWXXM DOM document is validated against the it's
     * XML Schema and Schematron rules and (if validation passed), the JAXB objects created from the the
     * DOM scanned for all the necessary property values for creating MessageConverter Java model objects. Additional validation
     * for the document structure and content is also done in this phase.&lt;/li&gt;
     *      &lt;li&gt;In the second phase the model objects are created and populated from the property data
     *      collected in the first phase.&lt;/li&gt;
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
    public ConversionResult<TAF> convertMessage(T input, ConversionHints hints) {
        ConversionResult<TAF> result = new ConversionResult<>();
        TAFType source;
        ReferredObjectRetrievalContext refCtx;

        try {
            Document dom = parseAsDom(input);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
            schemaFactory.setResourceResolver(resolver);
            //Secure processing does not allow "file" protocol loading for schemas:
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema iwxxmSchema = schemaFactory.newSchema(TAFType.class.getResource("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));

            Binder<Node> binder = getJAXBContext().createBinder();

            //XML Schema validation upon JAXB unmarshal:
            binder.setSchema(iwxxmSchema);
            IWXXMValidationEventHandler collector = new IWXXMValidationEventHandler();
            binder.setEventHandler(collector);
            source = binder.unmarshal(dom, TAFType.class).getValue();
            TAFProperties properties = new TAFProperties(source);

            for (ValidationEvent evt : collector.getEvents()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "XML Schema validation issue: " + evt.getMessage(), evt.getLinkedException()));
            }

            //Reset binder event handler after validation:
            binder.setEventHandler(null);

            refCtx = new ReferredObjectRetrievalContext(dom, binder);

            //Schematron validation:
            result.addIssue(IWXXMTAFScanner.validateAgainstIWXXMSchematron(dom, hints));

            //Other specific validation (using JAXB elements)
            result.addIssue(IWXXMTAFScanner.collectTAFProperties(source, refCtx, properties, hints));

            //Build the TAF:
            Optional<AviationCodeListUser.TAFStatus> status = properties.get(TAFProperties.Name.STATUS, AviationCodeListUser.TAFStatus.class);
            if (!status.isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "TAF status not known, unable to " + "proceed"));
                return result;
            }

            result.setConvertedMessage(createTAF(properties, status.get(), result));
        } catch (ConversionException ce) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Unable to parse input as an XML document", ce));
            return result;
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("Unexpected exception in parsing IWXXM content", e);
        }

        return result;

    }

    private TAF createTAF(final TAFProperties properties, final AviationCodeListUser.TAFStatus status, final ConversionResult<TAF> result) {
        TAFImpl.Builder tafBuilder = new TAFImpl.Builder();
        tafBuilder.setStatus(status);
        properties.get(TAFProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent(tafBuilder::setIssueTime);

        if (!AviationCodeListUser.TAFStatus.MISSING.equals(status)) {
            properties.get(TAFProperties.Name.VALID_TIME, PartialOrCompleteTimePeriod.class).ifPresent(tafBuilder::setValidityTime);
            List<OMObservationProperties> fctProps = properties.getList(TAFProperties.Name.CHANGE_FORECAST, OMObservationProperties.class);
            if (!fctProps.isEmpty()) {
                List<TAFChangeForecast> changeForecasts = new ArrayList<>();
                for (OMObservationProperties fctProp : fctProps) {
                    changeForecasts.add(createChangeForecast(fctProp));
                }
                tafBuilder.setChangeForecasts(changeForecasts);
            }
        }

        properties.get(TAFProperties.Name.BASE_FORECAST, OMObservationProperties.class).ifPresent((fctProp) -> {
            Optional<Aerodrome> aerodrome = fctProp.get(OMObservationProperties.Name.AERODROME, Aerodrome.class);
            Optional<GeoPosition> samplingPos = fctProp.get(OMObservationProperties.Name.SAMPLING_POINT, GeoPosition.class);
            if (aerodrome.isPresent()) {
                if (!aerodrome.get().getReferencePoint().isPresent()) {
                    if (samplingPos.isPresent()) {
                        //Use the sampling pos a fallback for a missing aerodrome referencePoint:
                        aerodrome = Optional.of(AerodromeImpl.immutableCopyOf(aerodrome.get()).toBuilder()
                                .setReferencePoint(samplingPos.get())
                                .build());
                    }
                }
            }
            aerodrome.ifPresent(tafBuilder::setAerodrome);
            fctProp.get(OMObservationProperties.Name.RESULT, ForecastRecordProperties.class).ifPresent((recordProp) -> {
                tafBuilder.setBaseForecast(createBaseForecast(recordProp));
            });
        });

        Optional<Aerodrome> previousReportAerodrome = properties.get(TAFProperties.Name.PREV_REPORT_AERODROME, Aerodrome.class);
        Optional<PartialOrCompleteTimePeriod> previousReportValidTime = properties.get(TAFProperties.Name.PREV_REPORT_VALID_TIME,
                PartialOrCompleteTimePeriod.class);
        if (previousReportAerodrome.isPresent() && previousReportValidTime.isPresent()) {
            tafBuilder.setReferredReport(
                    new TAFReferenceImpl.Builder().setAerodrome(previousReportAerodrome.get()).setValidityTime(previousReportValidTime.get()).build());
        }

        properties.get(TAFProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent((metaProps) -> {
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class)
                    .ifPresent(tafBuilder::setPermissibleUsage);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                    .ifPresent(tafBuilder::setPermissibleUsageReason);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class).ifPresent(tafBuilder::setPermissibleUsageSupplementary);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).ifPresent(tafBuilder::setTranslatedBulletinID);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                    .ifPresent(tafBuilder::setTranslatedBulletinReceptionTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class).ifPresent(tafBuilder::setTranslationCentreDesignator);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).ifPresent(tafBuilder::setTranslationCentreName);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).ifPresent(tafBuilder::setTranslationTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).ifPresent(tafBuilder::setTranslatedTAC); //!!!
        });

        return tafBuilder.build();
    }

    private TAFBaseForecast createBaseForecast(final ForecastRecordProperties source) {
        TAFBaseForecastImpl.Builder builder = new TAFBaseForecastImpl.Builder();
        source.get(ForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
        source.get(ForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class).ifPresent(builder::setPrevailingVisibilityOperator);
        source.get(ForecastRecordProperties.Name.SURFACE_WIND, TAFSurfaceWind.class).ifPresent(builder::setSurfaceWind);
        Optional<Boolean> nsw = source.get(ForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
        List<Weather> weather = source.getList(ForecastRecordProperties.Name.WEATHER, Weather.class);
        if (nsw.isPresent() && nsw.get()) {
            builder.setNoSignificantWeather(true);
        } else if (!weather.isEmpty()){
            builder.setForecastWeather(weather);
        }
        source.get(ForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
        List<TAFAirTemperatureForecast> temps = source.getList(ForecastRecordProperties.Name.TEMPERATURE, TAFAirTemperatureForecast.class);
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        source.get(ForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        return builder.build();
    }

    private TAFChangeForecast createChangeForecast(final OMObservationProperties source) {
        TAFChangeForecastImpl.Builder builder = new TAFChangeForecastImpl.Builder();
        source.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimePeriod.class).ifPresent(builder::setPeriodOfChange);
        Optional<ForecastRecordProperties> recordProps = source.get(OMObservationProperties.Name.RESULT, ForecastRecordProperties.class);
        if (recordProps.isPresent()) {
            recordProps.get().get(ForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.class).ifPresent(builder::setChangeIndicator);
            recordProps.get().get(ForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
            recordProps.get().get(ForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class).ifPresent(builder::setPrevailingVisibilityOperator);
            recordProps.get().get(ForecastRecordProperties.Name.SURFACE_WIND, TAFSurfaceWind.class).ifPresent(builder::setSurfaceWind);
            Optional<Boolean> nsw = recordProps.get().get(ForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
            List<Weather> weather = recordProps.get().getList(ForecastRecordProperties.Name.WEATHER, Weather.class);
            if (nsw.isPresent() && nsw.get()) {
                builder.setNoSignificantWeather(true);
            } else if (!weather.isEmpty()){
                builder.setForecastWeather(weather);
            }
            recordProps.get().get(ForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
            source.get(ForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        }
        return builder.build();
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
