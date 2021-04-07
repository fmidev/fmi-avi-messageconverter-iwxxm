package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Parser;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationCodeListUser.RelationalOperator;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import icao.iwxxm30.TAFType;

public abstract class TAFIWXXMParser<T> extends AbstractIWXXM30Parser<T, TAF> {

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

        final List<ConversionIssue> issues = TAFIWXXMScanner.collectTAFProperties(input, refCtx, properties, hints);
        result.addIssue(issues);

        if (result.getConversionIssues().size() > 0) {
            return null;
        }

        final TAFImpl.Builder tafBuilder = TAFImpl.builder();

        properties.get(TAFProperties.Name.REPORT_METADATA, GenericReportProperties.class)//
                .ifPresent(metaProps -> {
                    metaProps.get(GenericReportProperties.Name.REPORT_STATUS, AviationWeatherMessage.ReportStatus.class)//
                            .ifPresent(tafBuilder::setReportStatus);
                    metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class)
                            .ifPresent(tafBuilder::setPermissibleUsage);
                    metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                            .ifPresent(tafBuilder::setPermissibleUsageReason);
                    metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class)//
                            .ifPresent(tafBuilder::setPermissibleUsageSupplementary);
                    metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class)//
                            .ifPresent(tafBuilder::setTranslatedBulletinID);
                    metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                            .ifPresent(tafBuilder::setTranslatedBulletinReceptionTime);
                    metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class)//
                            .ifPresent(tafBuilder::setTranslationCentreDesignator);
                    metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class)//
                            .ifPresent(tafBuilder::setTranslationCentreName);
                    metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class)//
                            .ifPresent(tafBuilder::setTranslationTime);
                    metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class)//
                            .ifPresent(tafBuilder::setTranslatedTAC); //!!!
                });

        tafBuilder.setTranslated((tafBuilder.getTranslatedBulletinID().isPresent() || tafBuilder.getTranslatedBulletinReceptionTime().isPresent()
                || tafBuilder.getTranslatedTAC().isPresent() || tafBuilder.getTranslationCentreDesignator().isPresent() || tafBuilder.getTranslationCentreName()
                .isPresent() || tafBuilder.getTranslationTime().isPresent()));

        properties.get(TAFProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class)//
                .ifPresent(tafBuilder::setIssueTime);

        properties.get(TAFProperties.Name.AERODROME, Aerodrome.class)//
                .ifPresent(tafBuilder::setAerodrome);

        properties.get(TAFProperties.Name.IS_CANCEL_MESSAGE, Boolean.class)//
                .ifPresent(tafBuilder::setCancelMessage);

        properties.get(TAFProperties.Name.VALID_TIME, PartialOrCompleteTimePeriod.class)//
                .ifPresent(prop -> {
                    if (tafBuilder.isCancelMessage()) {
                        tafBuilder.setReferredReportValidPeriod(prop);
                    } else {

                        tafBuilder.setValidityTime(prop);
                    }
                });

        properties.get(TAFProperties.Name.BASE_FORECAST, TAFBaseForecastProperties.class)//
                .ifPresent(prop -> tafBuilder.setBaseForecast(toBaseForecast(prop)));

        if (properties.contains(TAFProperties.Name.CHANGE_FORECAST)) {
            tafBuilder.setChangeForecasts(properties.getList(TAFProperties.Name.CHANGE_FORECAST, TAFChangeForecastProperties.class).stream()//
                    .map(this::toChangeForecast)//
                    .collect(toImmutableList()));
        }
        return tafBuilder.build();
    }

    private TAFBaseForecast toBaseForecast(final TAFBaseForecastProperties baseForecastProp) {
        final TAFBaseForecastImpl.Builder builder = TAFBaseForecastImpl.builder();

        if (baseForecastProp.contains(TAFBaseForecastProperties.Name.TEMPERATURES)) {
            builder.setTemperatures(baseForecastProp.getList(TAFBaseForecastProperties.Name.TEMPERATURES, TAFAirTemperatureForecast.class));
        }

        baseForecastProp.get(TAFBaseForecastProperties.Name.FORECAST, TAFForecastProperties.class)
                .ifPresent(forecastProp -> setTAFForecastProperties(builder, forecastProp));
        return builder.build();
    }

    private TAFChangeForecastImpl toChangeForecast(final TAFChangeForecastProperties changeForecastProp) {
        final TAFChangeForecastImpl.Builder builder = TAFChangeForecastImpl.builder();

        changeForecastProp.get(TAFChangeForecastProperties.Name.CHANGE_PERIOD, PartialOrCompleteTimePeriod.class).ifPresent(builder::setPeriodOfChange);
        changeForecastProp.get(TAFChangeForecastProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.class)
                .ifPresent(builder::setChangeIndicator);

        changeForecastProp.get(TAFChangeForecastProperties.Name.FORECAST, TAFForecastProperties.class)//
                .ifPresent(forecastProp -> setTAFForecastProperties(builder, forecastProp));
        return builder.build();
    }

    private <F extends TAFForecast, B extends TAFForecast.Builder<F, B>> void setTAFForecastProperties(final TAFForecast.Builder<F, B> builder,
            final TAFForecastProperties forecastProp) {
        forecastProp.get(TAFForecastProperties.Name.CLOUD_FORECAST, TAFCloudForecastProperties.class)
                .ifPresent(prop -> builder.setCloud(toCloudForecast(prop)));
        forecastProp.get(TAFForecastProperties.Name.SURFACEWIND, SurfaceWind.class).ifPresent(builder::setSurfaceWind);
        forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).ifPresent(builder::setPrevailingVisibility);
        forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR, RelationalOperator.class)
                .ifPresent(builder::setPrevailingVisibilityOperator);
        forecastProp.get(TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class).ifPresent(builder::setCeilingAndVisibilityOk);
        if (forecastProp.contains(TAFForecastProperties.Name.FORECAST_WEATHER)) {
            builder.setForecastWeather(forecastProp.getList(TAFForecastProperties.Name.FORECAST_WEATHER, Weather.class));
        }
        forecastProp.get(TAFForecastProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class).ifPresent(builder::setNoSignificantWeather);
    }

    private CloudForecast toCloudForecast(final TAFCloudForecastProperties prop) {
        final CloudForecastImpl.Builder cloudForecast = CloudForecastImpl.builder();
        if (prop.contains(TAFCloudForecastProperties.Name.CLOUD_LAYER)) {
            cloudForecast.setLayers(prop.getList(TAFCloudForecastProperties.Name.CLOUD_LAYER, CloudLayer.class));
        }
        cloudForecast.setNoSignificantCloud(prop.get(TAFCloudForecastProperties.Name.NO_SIGNIFICANT_CLOUD, Boolean.class).orElse(false));
        prop.get(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY, NumericMeasure.class).ifPresent(cloudForecast::setVerticalVisibility);
        prop.get(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY_MISSING, Boolean.class).ifPresent(cloudForecast::setVerticalVisibilityMissing);
        return cloudForecast.build();
    }

    public static class FromDOM extends TAFIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends TAFIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }
}
