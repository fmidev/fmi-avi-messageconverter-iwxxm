package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
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

        List<ConversionIssue> issues = TAFIWXXMScanner.collectTAFProperties(input, refCtx, properties, hints);
        result.addIssue(issues);

        if (result.getConversionIssues().size() > 0) {
            return null;
        }

        TAFImpl.Builder tafBuilder = TAFImpl.builder();

        properties.get(TAFProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent((metaProps) -> {
            metaProps.get(GenericReportProperties.Name.REPORT_STATUS, AviationWeatherMessage.ReportStatus.class).ifPresent(tafBuilder::setReportStatus);
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

        properties.get(TAFProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent((prop) -> {
            tafBuilder.setIssueTime(prop);
        });

        properties.get(TAFProperties.Name.AERODROME, Aerodrome.class).ifPresent((prop) -> {
            tafBuilder.setAerodrome(prop);
        });

        properties.get(TAFProperties.Name.IS_CANCEL_MESSAGE, Boolean.class).ifPresent(prop -> {
            tafBuilder.setCancelMessage(prop);
        });

        properties.get(TAFProperties.Name.VALID_TIME, PartialOrCompleteTimePeriod.class).ifPresent((prop) -> {
            if(tafBuilder.isCancelMessage()) {
                tafBuilder.setReferredReportValidPeriod(prop);
            } else {

                tafBuilder.setValidityTime(prop);
            }
        });

        properties.get(TAFProperties.Name.BASE_FORECAST, TAFBaseForecastProperties.class).ifPresent((prop) -> {
            tafBuilder.setBaseForecast(getBaseForecast(prop));
        });

        properties.get(TAFProperties.Name.CHANGE_FORECAST, List.class).ifPresent((prop) -> {
            tafBuilder.setChangeForecasts(getChangeForecasts(prop));
        });

        return tafBuilder.build();
    }

    private TAFBaseForecast getBaseForecast(final TAFBaseForecastProperties prop) {
        TAFBaseForecastImpl.Builder baseForecast = TAFBaseForecastImpl.builder();
        if (prop.contains(TAFBaseForecastProperties.Name.TEMPERATURES)) {
            List<TAFAirTemperatureForecast> tempList = new ArrayList<>();
            prop.get(TAFBaseForecastProperties.Name.TEMPERATURES, List.class).get().stream().forEach(obj -> tempList.add((TAFAirTemperatureForecast) obj));
            baseForecast.setTemperatures(tempList);
        }
        if (prop.contains(TAFBaseForecastProperties.Name.FORECAST)) {
            TAFForecastProperties forecastProp = prop.get(TAFBaseForecastProperties.Name.FORECAST, TAFForecastProperties.class).get();
            if (forecastProp.contains(TAFForecastProperties.Name.CLOUD_FORECAST)) {
                baseForecast.setCloud(forecastProp.get(TAFForecastProperties.Name.CLOUD_FORECAST, CloudForecast.class));
            }
            if (forecastProp.contains(TAFForecastProperties.Name.SURFACEWIND)) {
                baseForecast.setSurfaceWind(forecastProp.get(TAFForecastProperties.Name.SURFACEWIND, SurfaceWind.class));
            }
            if (forecastProp.contains(TAFForecastProperties.Name.PREVAILING_VISIBILITY)) {
                baseForecast.setPrevailingVisibility(forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class));
            }
            if (forecastProp.contains(TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR)) {
                baseForecast.setPrevailingVisibilityOperator(
                        forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR, AviationCodeListUser.RelationalOperator.class));
            }
            if (forecastProp.contains(TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK)) {
                baseForecast.setCeilingAndVisibilityOk(forecastProp.get(TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class).get().booleanValue());
            }
            if (forecastProp.contains(TAFForecastProperties.Name.FORECAST_WEATHER)) {
                List<Weather> weatherList = new ArrayList<>();
                forecastProp.get(TAFForecastProperties.Name.FORECAST_WEATHER, List.class).get().stream().forEach(obj -> weatherList.add((Weather) obj));
                baseForecast.setForecastWeather(weatherList);
            }
        }

        return baseForecast.build();
    }

    private List<TAFChangeForecast> getChangeForecasts(final List list) {
        List<TAFChangeForecast> forecasts = new ArrayList<>();
        list.stream().filter(obj -> obj instanceof TAFChangeForecastProperties).forEach(obj -> {
            TAFChangeForecastProperties prop = (TAFChangeForecastProperties) obj;
            TAFChangeForecastImpl.Builder changeForecast = TAFChangeForecastImpl.builder();

            if (prop.contains(TAFChangeForecastProperties.Name.CHANGE_PERIOD)) {
                changeForecast.setPeriodOfChange(prop.get(TAFChangeForecastProperties.Name.CHANGE_PERIOD, PartialOrCompleteTimePeriod.class).get());
            }
            if (prop.contains(TAFChangeForecastProperties.Name.CHANGE_INDICATOR)) {
                changeForecast.setChangeIndicator(
                        prop.get(TAFChangeForecastProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.class).get());
            }

            if (prop.contains(TAFChangeForecastProperties.Name.FORECAST)) {
                TAFForecastProperties forecastProp = prop.get(TAFChangeForecastProperties.Name.FORECAST, TAFForecastProperties.class).get();
                if (forecastProp.contains(TAFForecastProperties.Name.CLOUD_FORECAST)) {
                    changeForecast.setCloud(forecastProp.get(TAFForecastProperties.Name.CLOUD_FORECAST, CloudForecast.class));
                }
                if (forecastProp.contains(TAFForecastProperties.Name.SURFACEWIND)) {
                    changeForecast.setSurfaceWind(forecastProp.get(TAFForecastProperties.Name.SURFACEWIND, SurfaceWind.class));
                }
                if (forecastProp.contains(TAFForecastProperties.Name.PREVAILING_VISIBILITY)) {
                    changeForecast.setPrevailingVisibility(forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class));
                }
                if (forecastProp.contains(TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR)) {
                    changeForecast.setPrevailingVisibilityOperator(
                            forecastProp.get(TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR, AviationCodeListUser.RelationalOperator.class));
                }
                if (forecastProp.contains(TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK)) {
                    changeForecast.setCeilingAndVisibilityOk(forecastProp.get(TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class).get().booleanValue());
                }
                if (forecastProp.contains(TAFForecastProperties.Name.FORECAST_WEATHER)) {
                    List<Weather> weatherList = new ArrayList<>();
                    forecastProp.get(TAFForecastProperties.Name.FORECAST_WEATHER, List.class)
                            .get()
                            .stream()
                            .forEach(weatherObj -> weatherList.add((Weather) weatherObj));
                    changeForecast.setForecastWeather(weatherList);
                }
            }
            forecasts.add(changeForecast.build());
        });
        return forecasts;
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
