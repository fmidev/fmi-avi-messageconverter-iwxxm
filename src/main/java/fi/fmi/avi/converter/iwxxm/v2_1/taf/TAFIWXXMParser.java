package fi.fmi.avi.converter.iwxxm.v2_1.taf;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Parser;
import fi.fmi.avi.converter.iwxxm.v2_1.OMObservationProperties;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.ElevatedPoint;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import icao.iwxxm21.TAFType;

/**
 * Common functionality for conversions related to reading IWXXM TAFs.
 *
 * @param <T>
 *         the type of the raw input message
 */
public abstract class TAFIWXXMParser<T> extends AbstractIWXXM21Parser<T, TAF> {

    @Override
    protected TAF createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<TAF> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        final TAFType input;
        if (TAFType.class.isAssignableFrom(source.getClass())) {
            input = (TAFType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            final JAXBElement<?> je = (JAXBElement<?>) source;
            if (TAFType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (TAFType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a TAF JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a TAF JAXB element");
        }

        final TAFProperties properties = new TAFProperties();

        //Other specific validation (using JAXB elements)
        result.addIssue(TAFIWXXMScanner.collectTAFProperties(input, refCtx, properties, hints));

        //Build the TAF:
        final TAFImpl.Builder tafBuilder = TAFImpl.builder();

        final AviationCodeListUser.TAFStatus tafStatus = properties.get(TAFProperties.Name.STATUS, AviationCodeListUser.TAFStatus.class).orElse(null);
        if (tafStatus == null) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "TAF status not known, unable to " + "proceed"));
            return null;
        }
        tafBuilder//
                .setReportStatus(tafStatus.getReportStatus())//
                .setCancelMessage(tafStatus.isCancelMessage());
        properties.get(TAFProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent(tafBuilder::setIssueTime);

        if (!AviationCodeListUser.TAFStatus.MISSING.equals(tafStatus)) {
            properties.get(TAFProperties.Name.VALID_TIME, PartialOrCompleteTimePeriod.class).ifPresent(tafBuilder::setValidityTime);
            final List<OMObservationProperties> fctProps = properties.getList(TAFProperties.Name.CHANGE_FORECAST, OMObservationProperties.class);
            if (!fctProps.isEmpty()) {
                tafBuilder.setChangeForecasts(fctProps.stream()//
                        .map(this::createChangeForecast)//
                        .collect(toImmutableList()));
            }
        }

        final Aerodrome[] aerodrome = new Aerodrome[1];
        properties.get(TAFProperties.Name.BASE_FORECAST, OMObservationProperties.class).ifPresent(fctProp -> {
            aerodrome[0] = fctProp.get(OMObservationProperties.Name.AERODROME, Aerodrome.class).orElse(null);
            final Optional<ElevatedPoint> samplingPos = fctProp.get(OMObservationProperties.Name.SAMPLING_POINT, ElevatedPoint.class);
            if (aerodrome[0] != null) {
                if (!aerodrome[0].getReferencePoint().isPresent()) {
                    samplingPos.ifPresent(
                            //Use the sampling pos a fallback for a missing aerodrome referencePoint:
                            elevatedPoint -> aerodrome[0] = AerodromeImpl.Builder.from(aerodrome[0]).setReferencePoint(elevatedPoint).build());
                }
                tafBuilder.setAerodrome(aerodrome[0]);
            }
            if (tafStatus != AviationCodeListUser.TAFStatus.MISSING) {
                fctProp.get(OMObservationProperties.Name.RESULT, TAFForecastRecordProperties.class)
                        .ifPresent(recordProp -> tafBuilder.setBaseForecast(createBaseForecast(recordProp)));
            }
        });

        if (aerodrome[0] == null) {
            // Read aerodrome from previous report only if not present in "current" report.
            properties.get(TAFProperties.Name.PREV_REPORT_AERODROME, Aerodrome.class)//
                    .ifPresent(tafBuilder::setAerodrome);
        }

        properties.get(TAFProperties.Name.PREV_REPORT_VALID_TIME, PartialOrCompleteTimePeriod.class)//
                .ifPresent(tafBuilder::setReferredReportValidPeriod);

        properties.get(TAFProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent(metaProps -> {
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

    private TAFBaseForecast createBaseForecast(final TAFForecastRecordProperties source) {
        final TAFBaseForecastImpl.Builder builder = TAFBaseForecastImpl.builder();
        source.get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
        source.get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                .ifPresent(builder::setPrevailingVisibilityOperator);
        source.get(TAFForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class).ifPresent(builder::setSurfaceWind);
        final Optional<Boolean> nsw = source.get(TAFForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
        final List<Weather> weather = source.getList(TAFForecastRecordProperties.Name.WEATHER, Weather.class);
        if (nsw.orElse(false)) {
            builder.setNoSignificantWeather(true);
        } else if (!weather.isEmpty()) {
            builder.setForecastWeather(weather);
        }
        source.get(TAFForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
        final List<TAFAirTemperatureForecast> temps = source.getList(TAFForecastRecordProperties.Name.TEMPERATURE, TAFAirTemperatureForecast.class);
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        source.get(TAFForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        return builder.build();
    }

    private TAFChangeForecast createChangeForecast(final OMObservationProperties source) {
        final TAFChangeForecastImpl.Builder builder = TAFChangeForecastImpl.builder();
        source.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimePeriod.class).ifPresent(builder::setPeriodOfChange);
        final Optional<TAFForecastRecordProperties> recordProps = source.get(OMObservationProperties.Name.RESULT, TAFForecastRecordProperties.class);
        if (recordProps.isPresent()) {
            recordProps.get()
                    .get(TAFForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.class)
                    .ifPresent(builder::setChangeIndicator);
            recordProps.get().get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
            recordProps.get()
                    .get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                    .ifPresent(builder::setPrevailingVisibilityOperator);
            recordProps.get().get(TAFForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class).ifPresent(builder::setSurfaceWind);
            final Optional<Boolean> nsw = recordProps.get().get(TAFForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
            final List<Weather> weather = recordProps.get().getList(TAFForecastRecordProperties.Name.WEATHER, Weather.class);
            if (nsw.isPresent() && nsw.get()) {
                builder.setNoSignificantWeather(true);
            } else if (!weather.isEmpty()) {
                builder.setForecastWeather(weather);
            }
            recordProps.get().get(TAFForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
            source.get(TAFForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        }
        return builder.build();
    }

    public static class FromString extends TAFIWXXMParser<String> {
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
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }

    public static class FromDOM extends TAFIWXXMParser<Document> {
        /**
         * Returns the TAF input message as A DOM Document.
         *
         * @param input
         *         the XML Document input as a String
         *
         * @return the input parsed as DOM
         */
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

}
