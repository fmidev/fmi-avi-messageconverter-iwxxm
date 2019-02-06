package fi.fmi.avi.converter.iwxxm.taf;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.GeoPosition;
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
import fi.fmi.avi.model.taf.immutable.TAFReferenceImpl;
import icao.iwxxm21.TAFType;

/**
 * Common functionality for conversions related to reading IWXXM TAFs.
 *
 * @param <T> the type of the raw input message
 */
public abstract class AbstractTAFIWXXMParser<T> extends AbstractIWXXMParser<T, TAF> {

    protected TAF createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<TAF> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        TAFType input;
        if (TAFType.class.isAssignableFrom(source.getClass())) {
            input = (TAFType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (TAFType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (TAFType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a TAF JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a TAF JAXB element");
        }

        TAFProperties properties = new TAFProperties();

        //Other specific validation (using JAXB elements)
        result.addIssue(IWXXMTAFScanner.collectTAFProperties(input, refCtx, properties, hints));

        //Build the TAF:
        Optional<AviationCodeListUser.TAFStatus> status = properties.get(TAFProperties.Name.STATUS, AviationCodeListUser.TAFStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "TAF status not known, unable to " + "proceed"));
            return null;
        }

        TAFImpl.Builder tafBuilder = new TAFImpl.Builder();
        tafBuilder.setStatus(status.get());
        properties.get(TAFProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent(tafBuilder::setIssueTime);

        if (!AviationCodeListUser.TAFStatus.MISSING.equals(status.get())) {
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
            fctProp.get(OMObservationProperties.Name.RESULT, TAFForecastRecordProperties.class).ifPresent((recordProp) -> {
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

    private TAFBaseForecast createBaseForecast(final TAFForecastRecordProperties source) {
        TAFBaseForecastImpl.Builder builder = new TAFBaseForecastImpl.Builder();
        source.get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
        source.get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                .ifPresent(builder::setPrevailingVisibilityOperator);
        source.get(TAFForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class).ifPresent(builder::setSurfaceWind);
        Optional<Boolean> nsw = source.get(TAFForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
        List<Weather> weather = source.getList(TAFForecastRecordProperties.Name.WEATHER, Weather.class);
        if (nsw.isPresent() && nsw.get()) {
            builder.setNoSignificantWeather(true);
        } else if (!weather.isEmpty()){
            builder.setForecastWeather(weather);
        }
        source.get(TAFForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
        List<TAFAirTemperatureForecast> temps = source.getList(TAFForecastRecordProperties.Name.TEMPERATURE, TAFAirTemperatureForecast.class);
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        source.get(TAFForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        return builder.build();
    }

    private TAFChangeForecast createChangeForecast(final OMObservationProperties source) {
        TAFChangeForecastImpl.Builder builder = new TAFChangeForecastImpl.Builder();
        source.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimePeriod.class).ifPresent(builder::setPeriodOfChange);
        Optional<TAFForecastRecordProperties> recordProps = source.get(OMObservationProperties.Name.RESULT, TAFForecastRecordProperties.class);
        if (recordProps.isPresent()) {
            recordProps.get()
                    .get(TAFForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.class)
                    .ifPresent(builder::setChangeIndicator);
            recordProps.get().get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
            recordProps.get()
                    .get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                    .ifPresent(builder::setPrevailingVisibilityOperator);
            recordProps.get().get(TAFForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class).ifPresent(builder::setSurfaceWind);
            Optional<Boolean> nsw = recordProps.get().get(TAFForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
            List<Weather> weather = recordProps.get().getList(TAFForecastRecordProperties.Name.WEATHER, Weather.class);
            if (nsw.isPresent() && nsw.get()) {
                builder.setNoSignificantWeather(true);
            } else if (!weather.isEmpty()){
                builder.setForecastWeather(weather);
            }
            recordProps.get().get(TAFForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(builder::setCloud);
            source.get(TAFForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
        }
        return builder.build();
    }

}
