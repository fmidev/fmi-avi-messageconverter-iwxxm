package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFBaseForecastProperties.Name.MISSING_MESSAGE;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFBaseForecastProperties.Name.TEMPERATURES;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFChangeForecastProperties.Name.CHANGE_INDICATOR;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFChangeForecastProperties.Name.CHANGE_PERIOD;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.CLOUD_FORECAST;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.FORECAST_WEATHER;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.PREVAILING_VISIBILITY;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.SURFACEWIND;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.VISIBILITY_OK;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFProperties.Name.AERODROME;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFProperties.Name.BASE_FORECAST;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFProperties.Name.CHANGE_FORECAST;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFProperties.Name.IS_CANCEL_MESSAGE;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFProperties.Name.VALID_TIME;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.ElevatedPointType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Scanner;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import icao.iwxxm30.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm30.AerodromeCloudForecastType;
import icao.iwxxm30.AerodromeForecastWeatherType;
import icao.iwxxm30.AerodromeSurfaceWindForecastType;
import icao.iwxxm30.AirportHeliportPropertyType;
import icao.iwxxm30.CloudLayerPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastPropertyType;
import icao.iwxxm30.TAFType;

public class TAFIWXXMScanner extends AbstractIWXXM30Scanner {
    public static List<ConversionIssue> collectTAFProperties(final TAFType input, final ReferredObjectRetrievalContext refCtx, final TAFProperties properties,
            final ConversionHints hints) {
        final IssueList issueList = new IssueList();

        final GenericReportProperties meta = new GenericReportProperties();
        issueList.addAll(AbstractIWXXM30Scanner.collectReportMetadata(input, meta, hints));
        properties.set(TAFProperties.Name.REPORT_METADATA, meta);

        if (input.getIssueTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
            if (!issueTime.isPresent()) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(TAFProperties.Name.ISSUE_TIME, issueTime.get());
            }
        }

        if (input.getValidPeriod() != null) {
            final Optional<ZonedDateTime> startTimeZoned = getStartTime(input.getValidPeriod().getTimePeriod(), refCtx);
            final Optional<ZonedDateTime> endTimeZoned = getEndTime(input.getValidPeriod().getTimePeriod(), refCtx);
            if (startTimeZoned.isPresent() && endTimeZoned.isPresent()) {
                Optional<PartialOrCompleteTimeInstant> startTime = Optional.of(PartialOrCompleteTimeInstant.of(startTimeZoned.get()));
                Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(endTimeZoned.get()));

                final PartialOrCompleteTimePeriod validPeriod = PartialOrCompleteTimePeriod.builder().setStartTime(startTime).setEndTime(endTime).build();

                properties.set(VALID_TIME, validPeriod);
            }
        }

        if (input.getAerodrome() != null) {
            AirportHeliportPropertyType airportHeliportPropertyType = input.getAerodrome();

            AerodromeImpl.Builder aerodrome = AerodromeImpl.builder();
            List<AirportHeliportTimeSlicePropertyType> timeslices = airportHeliportPropertyType.getAirportHeliport().getTimeSlice();
            for (AirportHeliportTimeSlicePropertyType timeslice : timeslices) {
                AirportHeliportTimeSliceType airportHeliportTimeSlice = timeslice.getAirportHeliportTimeSlice();

                aerodrome.setDesignator(airportHeliportTimeSlice.getDesignator().getValue());
                if (airportHeliportTimeSlice.getDesignatorIATA() != null) {
                    aerodrome.setDesignatorIATA(airportHeliportTimeSlice.getDesignatorIATA().getValue());
                }
                aerodrome.setName(airportHeliportTimeSlice.getPortName().getValue());
                aerodrome.setLocationIndicatorICAO(airportHeliportTimeSlice.getLocationIndicatorICAO().getValue());

                if (airportHeliportTimeSlice.getARP() != null) {
                    ElevatedPointType arp = airportHeliportTimeSlice.getARP().getElevatedPoint();

                    ElevatedPointImpl.Builder elevatedPoint = ElevatedPointImpl.builder();
                    elevatedPoint.setCrs(CoordinateReferenceSystemImpl.builder()
                            .setAxisLabels(arp.getAxisLabels())
                            .setName(arp.getSrsName())
                            .setDimension(arp.getSrsDimension().intValue())
                            .build());
                    elevatedPoint.setCoordinates(arp.getPos().getValue());
                    elevatedPoint.setElevationUom(arp.getElevation().getUom());
                    elevatedPoint.setElevationValue(Double.valueOf(arp.getElevation().getValue()));
                    elevatedPoint.setVerticalDatum(arp.getVerticalDatum().getValue());

                    aerodrome.setReferencePoint(elevatedPoint.build());
                }
            }
            properties.set(AERODROME, aerodrome.build());
        }

        if (input.getBaseForecast() != null) {
            if (input.getBaseForecast().getMeteorologicalAerodromeForecast() == null) {
                properties.set(MISSING_MESSAGE, true);
                return issueList;
            }
            TAFBaseForecastProperties baseForecastProperties = new TAFBaseForecastProperties();

            setTAFBaseForecastProperties(baseForecastProperties, input.getBaseForecast(), issueList, refCtx, hints);
            properties.set(BASE_FORECAST, baseForecastProperties);
        }

        if (input.getChangeForecast() != null) {
            List<TAFChangeForecastProperties> changeForecasts = new ArrayList<>();

            for (MeteorologicalAerodromeForecastPropertyType forecast : input.getChangeForecast()) {
                TAFChangeForecastProperties changeForecastProperties = new TAFChangeForecastProperties();

                setTAFChangeForecstProperties(changeForecastProperties, forecast, refCtx, issueList, hints);

                changeForecasts.add(changeForecastProperties);
            }
            properties.set(CHANGE_FORECAST, changeForecasts);
        }

        if (input.getCancelledReportValidPeriod() != null) {
            final Optional<ZonedDateTime> cancelStartTimeZoned = getStartTime(input.getCancelledReportValidPeriod().getTimePeriod(), refCtx);
            final Optional<ZonedDateTime> cancelEndTimeZoned = getEndTime(input.getCancelledReportValidPeriod().getTimePeriod(), refCtx);
            if (cancelStartTimeZoned.isPresent() && cancelEndTimeZoned.isPresent()) {
                Optional<PartialOrCompleteTimeInstant> startTime = Optional.of(PartialOrCompleteTimeInstant.of(cancelStartTimeZoned.get()));
                Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(cancelEndTimeZoned.get()));

                final PartialOrCompleteTimePeriod cancelPeriod = PartialOrCompleteTimePeriod.builder().setStartTime(startTime).setEndTime(endTime).build();

                properties.set(VALID_TIME, cancelPeriod);
            }
            properties.set(IS_CANCEL_MESSAGE, true);
        }

        return issueList;
    }

    private static void setTAFBaseForecastProperties(final TAFBaseForecastProperties props, final MeteorologicalAerodromeForecastPropertyType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, ConversionHints hints) {
        if (input.getMeteorologicalAerodromeForecast() != null) {
            if (input.getMeteorologicalAerodromeForecast().getTemperature().size() > 0) {
                List<TAFAirTemperatureForecast> temperatureForecasts = new ArrayList<>();
                for (AerodromeAirTemperatureForecastPropertyType type : input.getMeteorologicalAerodromeForecast().getTemperature()) {
                    temperatureForecasts.add(setTemperatures(type, refCtx));
                }

                props.set(TEMPERATURES, temperatureForecasts);
            }

            TAFForecastProperties forecatsProperties = new TAFForecastProperties();
            setTAFForecastProperties(forecatsProperties, input, issueList, refCtx, hints);

            props.set(TAFBaseForecastProperties.Name.FORECAST, forecatsProperties);
        } else {
            props.set(MISSING_MESSAGE, true);
        }
    }

    private static void setTAFChangeForecstProperties(final TAFChangeForecastProperties props, final MeteorologicalAerodromeForecastPropertyType input,
            final ReferredObjectRetrievalContext refCtx, final List<ConversionIssue> issueList, ConversionHints hints) {
        final Optional<ZonedDateTime> changeForecastStart = getStartTime(input.getMeteorologicalAerodromeForecast().getPhenomenonTime().getTimePeriod(),
                refCtx);
        final Optional<ZonedDateTime> changeForecastEnd = getEndTime(input.getMeteorologicalAerodromeForecast().getPhenomenonTime().getTimePeriod(), refCtx);
        if (changeForecastStart.isPresent() && changeForecastEnd.isPresent()) {
            Optional<PartialOrCompleteTimeInstant> startTime = Optional.of(PartialOrCompleteTimeInstant.of(changeForecastStart.get()));
            Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(changeForecastEnd.get()));

            final PartialOrCompleteTimePeriod changePeriod = PartialOrCompleteTimePeriod.builder().setStartTime(startTime).setEndTime(endTime).build();

            props.set(CHANGE_PERIOD, changePeriod);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Valid period is required in change forecasts"));
        }

        if (input.getMeteorologicalAerodromeForecast().getChangeIndicator() != null) {
            props.set(CHANGE_INDICATOR,
                    AviationCodeListUser.TAFChangeIndicator.valueOf(input.getMeteorologicalAerodromeForecast().getChangeIndicator().value()));
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Change indicator is required in change forecasts"));
        }

        TAFForecastProperties forecatsProperties = new TAFForecastProperties();

        setTAFForecastProperties(forecatsProperties, input, issueList, refCtx, hints);

        props.set(TAFChangeForecastProperties.Name.FORECAST, forecatsProperties);
    }

    private static void setTAFForecastProperties(final TAFForecastProperties props, final MeteorologicalAerodromeForecastPropertyType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, ConversionHints hints) {

        props.set(VISIBILITY_OK, input.getMeteorologicalAerodromeForecast().isCloudAndVisibilityOK());

        if (input.getMeteorologicalAerodromeForecast().getPrevailingVisibility() != null) {
            props.set(PREVAILING_VISIBILITY, NumericMeasureImpl.builder()
                    .setUom(input.getMeteorologicalAerodromeForecast().getPrevailingVisibility().getUom())
                    .setValue(input.getMeteorologicalAerodromeForecast().getPrevailingVisibility().getValue())
                    .build());
        }

        if (input.getMeteorologicalAerodromeForecast().getPrevailingVisibilityOperator() != null) {
            props.set(PREVAILING_VISIBILIT_OPERATOR,
                    AviationCodeListUser.RelationalOperator.valueOf(input.getMeteorologicalAerodromeForecast().getPrevailingVisibilityOperator().value()));
        }

        if (input.getMeteorologicalAerodromeForecast().getWeather().size() > 0) {
            List<Weather> weatherList = new ArrayList<>();
            for (AerodromeForecastWeatherType weatherType : input.getMeteorologicalAerodromeForecast().getWeather()) {
                withWeatherBuilderFor(weatherType, hints, value -> weatherList.add(value.build())
                        , issue -> issueList.add(issue));
                //weatherList.add(WeatherImpl.builder().setCode(weatherType.getHref()).build());
            }
            props.set(FORECAST_WEATHER, weatherList);
        }

        if (input.getMeteorologicalAerodromeForecast().getSurfaceWind() != null) {
            props.set(SURFACEWIND, setSurfaceWind(input.getMeteorologicalAerodromeForecast().getSurfaceWind().getAerodromeSurfaceWindForecast()));
        }

        if (input.getMeteorologicalAerodromeForecast().getCloud() != null) {
            props.set(CLOUD_FORECAST,
                    CloudForecastImpl.builder().setLayers(setCloud(input.getMeteorologicalAerodromeForecast().getCloud().getAerodromeCloudForecast())).build());
        }
    }

    private static TAFAirTemperatureForecast setTemperatures(final AerodromeAirTemperatureForecastPropertyType input,
            final ReferredObjectRetrievalContext refCtx) {
        TAFAirTemperatureForecastImpl.Builder temp = TAFAirTemperatureForecastImpl.builder();

        if (input.getAerodromeAirTemperatureForecast().getMaximumAirTemperature() != null) {
            temp.setMaxTemperature(NumericMeasureImpl.builder()
                    .setUom(input.getAerodromeAirTemperatureForecast().getMaximumAirTemperature().getUom())
                    .setValue(input.getAerodromeAirTemperatureForecast().getMaximumAirTemperature().getValue())
                    .build());
        }

        if (input.getAerodromeAirTemperatureForecast().getMaximumAirTemperatureTime() != null) {
            Optional<PartialOrCompleteTimeInstant> maxTempTime = getCompleteTimeInstant(
                    input.getAerodromeAirTemperatureForecast().getMaximumAirTemperatureTime(), refCtx);
            if (maxTempTime.isPresent()) {
                temp.setMaxTemperatureTime(maxTempTime.get());
            }
        }

        if (input.getAerodromeAirTemperatureForecast().getMinimumAirTemperature() != null) {
            temp.setMinTemperature(NumericMeasureImpl.builder()
                    .setUom(input.getAerodromeAirTemperatureForecast().getMinimumAirTemperature().getUom())
                    .setValue(input.getAerodromeAirTemperatureForecast().getMinimumAirTemperature().getValue())
                    .build());
        }

        if (input.getAerodromeAirTemperatureForecast().getMinimumAirTemperatureTime() != null) {
            Optional<PartialOrCompleteTimeInstant> minTempTime = getCompleteTimeInstant(
                    (input.getAerodromeAirTemperatureForecast().getMinimumAirTemperatureTime()), refCtx);
            if (minTempTime.isPresent()) {
                temp.setMinTemperatureTime(minTempTime.get());
            }
        }
        return temp.build();
    }

    private static SurfaceWind setSurfaceWind(final AerodromeSurfaceWindForecastType swForecast) {
        SurfaceWindImpl.Builder surfaceWind = SurfaceWindImpl.builder();

        surfaceWind.setVariableDirection(swForecast.isVariableWindDirection());

        if (swForecast.getMeanWindDirection() != null) {
            surfaceWind.setMeanWindDirection(NumericMeasureImpl.builder()
                    .setUom(swForecast.getMeanWindDirection().getUom())
                    .setValue(swForecast.getMeanWindDirection().getValue())
                    .build());
        }

        if (swForecast.getMeanWindSpeed() != null) {
            surfaceWind.setMeanWindSpeed(
                    NumericMeasureImpl.builder().setUom(swForecast.getMeanWindSpeed().getUom()).setValue(swForecast.getMeanWindSpeed().getValue()).build());
        }

        if (swForecast.getWindGustSpeed() != null) {
            surfaceWind.setWindGust(
                    NumericMeasureImpl.builder().setUom(swForecast.getWindGustSpeed().getUom()).setValue(swForecast.getWindGustSpeed().getValue()).build());
        }

        return surfaceWind.build();
    }

    private static List<CloudLayer> setCloud(final AerodromeCloudForecastType cloudForecast) {
        List<CloudLayer> cloudLayers = new ArrayList<>();

        for (CloudLayerPropertyType layer : cloudForecast.getLayer()) {
            CloudLayerImpl.Builder cloudLayer = CloudLayerImpl.builder();
            if (layer.getCloudLayer().getAmount() != null) {
                cloudLayer.setAmount(AviationCodeListUser.CloudAmount.valueOf(layer.getCloudLayer()
                        .getAmount()
                        .getHref()
                        .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME.length())));
            }
            if (layer.getCloudLayer().getBase() != null) {
                cloudLayer.setBase(NumericMeasureImpl.builder()
                        .setUom(layer.getCloudLayer().getBase().getUom())
                        .setValue(layer.getCloudLayer().getBase().getValue())
                        .build());
            }
            if (layer.getCloudLayer().getCloudType() != null) {
                cloudLayer.setCloudType(AviationCodeListUser.CloudType.valueOf(layer.getCloudLayer()
                        .getCloudType()
                        .getValue()
                        .getHref()
                        .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE.length())));
            }

            cloudLayers.add(cloudLayer.build());
        }
        return cloudLayers;
    }

}
