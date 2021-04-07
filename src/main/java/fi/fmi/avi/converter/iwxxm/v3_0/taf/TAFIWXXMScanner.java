package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFBaseForecastProperties.Name.MISSING_MESSAGE;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFBaseForecastProperties.Name.TEMPERATURES;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFChangeForecastProperties.Name.CHANGE_INDICATOR;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFChangeForecastProperties.Name.CHANGE_PERIOD;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.CLOUD_FORECAST;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.FORECAST_WEATHER;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.NO_SIGNIFICANT_WEATHER;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.PREVAILING_VISIBILITY;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.PREVAILING_VISIBILIT_OPERATOR;
import static fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFForecastProperties.Name.SURFACEWIND;
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
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import icao.iwxxm30.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm30.AerodromeCloudForecastPropertyType;
import icao.iwxxm30.AerodromeCloudForecastType;
import icao.iwxxm30.AerodromeForecastWeatherType;
import icao.iwxxm30.AerodromeSurfaceWindForecastType;
import icao.iwxxm30.AirportHeliportPropertyType;
import icao.iwxxm30.CloudLayerPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastType;
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
                final Optional<PartialOrCompleteTimeInstant> startTime = Optional.of(PartialOrCompleteTimeInstant.of(startTimeZoned.get()));
                final Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(endTimeZoned.get()));

                final PartialOrCompleteTimePeriod validPeriod = PartialOrCompleteTimePeriod.builder().setStartTime(startTime).setEndTime(endTime).build();

                properties.set(VALID_TIME, validPeriod);
            }
        }

        if (input.getAerodrome() != null) {
            final AirportHeliportPropertyType airportHeliportPropertyType = input.getAerodrome();

            final AerodromeImpl.Builder aerodrome = AerodromeImpl.builder();
            final List<AirportHeliportTimeSlicePropertyType> timeslices = airportHeliportPropertyType.getAirportHeliport().getTimeSlice();
            for (final AirportHeliportTimeSlicePropertyType timeslice : timeslices) {
                final AirportHeliportTimeSliceType airportHeliportTimeSlice = timeslice.getAirportHeliportTimeSlice();

                aerodrome.setDesignator(airportHeliportTimeSlice.getDesignator().getValue());
                if (airportHeliportTimeSlice.getDesignatorIATA() != null) {
                    aerodrome.setDesignatorIATA(airportHeliportTimeSlice.getDesignatorIATA().getValue());
                }
                aerodrome.setName(airportHeliportTimeSlice.getPortName().getValue());
                aerodrome.setLocationIndicatorICAO(airportHeliportTimeSlice.getLocationIndicatorICAO().getValue());

                if (airportHeliportTimeSlice.getARP() != null) {
                    final ElevatedPointType arp = airportHeliportTimeSlice.getARP().getElevatedPoint();

                    final ElevatedPointImpl.Builder elevatedPoint = ElevatedPointImpl.builder();
                    elevatedPoint.setCrs(CoordinateReferenceSystemImpl.builder()
                            .setAxisLabels(arp.getAxisLabels())
                            .setName(arp.getSrsName())
                            .setDimension(arp.getSrsDimension().intValue())
                            .build());
                    elevatedPoint.setCoordinates(arp.getPos().getValue());
                    elevatedPoint.setElevationUom(arp.getElevation().getUom());
                    elevatedPoint.setElevationValue(Double.parseDouble(arp.getElevation().getValue()));
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
            final TAFBaseForecastProperties baseForecastProperties = new TAFBaseForecastProperties();

            setTAFBaseForecastProperties(baseForecastProperties, input.getBaseForecast().getMeteorologicalAerodromeForecast(), issueList, refCtx, hints);
            properties.set(BASE_FORECAST, baseForecastProperties);
        }

        if (input.getChangeForecast() != null) {
            final List<TAFChangeForecastProperties> changeForecasts = new ArrayList<>();

            for (final MeteorologicalAerodromeForecastPropertyType forecast : input.getChangeForecast()) {
                final TAFChangeForecastProperties changeForecastProperties = new TAFChangeForecastProperties();

                setTAFChangeForecastProperties(changeForecastProperties, forecast.getMeteorologicalAerodromeForecast(), refCtx, issueList, hints);

                changeForecasts.add(changeForecastProperties);
            }
            properties.set(CHANGE_FORECAST, changeForecasts);
        }

        if (input.isIsCancelReport() != null && input.isIsCancelReport()) {
            properties.set(IS_CANCEL_MESSAGE, true);

            if (input.getCancelledReportValidPeriod() != null) {
                final Optional<ZonedDateTime> cancelStartTimeZoned = getStartTime(input.getCancelledReportValidPeriod().getTimePeriod(), refCtx);
                final Optional<ZonedDateTime> cancelEndTimeZoned = getEndTime(input.getCancelledReportValidPeriod().getTimePeriod(), refCtx);
                if (!cancelStartTimeZoned.isPresent() && !cancelEndTimeZoned.isPresent()) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cancellation period is missing."));
                } else {
                    final PartialOrCompleteTimePeriod.Builder cancelPeriod = PartialOrCompleteTimePeriod.builder();
                    if (cancelStartTimeZoned.isPresent()) {
                        cancelPeriod.setStartTime(Optional.of(PartialOrCompleteTimeInstant.of(cancelStartTimeZoned.get())));
                    } else {
                        issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Cancellation start time is missing."));
                    }

                    if (cancelEndTimeZoned.isPresent()) {
                        cancelPeriod.setEndTime(Optional.of(PartialOrCompleteTimeInstant.of(cancelEndTimeZoned.get())));
                    } else {
                        issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Cancellation end time is " + "missing."));
                    }

                    properties.set(VALID_TIME, cancelPeriod.build());
                }
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cancellation period is missing."));
            }
        }

        return issueList;
    }

    private static void setTAFBaseForecastProperties(final TAFBaseForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, final ConversionHints hints) {
        if (input == null) {
            props.set(MISSING_MESSAGE, true);
            return;
        }
        if (!input.getTemperature().isEmpty()) {
            props.set(TEMPERATURES, input.getTemperature().stream()//
                    .map(type -> setTemperatures(type, refCtx))//
                    .collect(toImmutableList()));
        }

        final TAFForecastProperties forecatsProperties = new TAFForecastProperties();
        setTAFForecastProperties(forecatsProperties, input, issueList, refCtx, hints);
        props.set(TAFBaseForecastProperties.Name.FORECAST, forecatsProperties);
    }

    private static void setTAFChangeForecastProperties(final TAFChangeForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final ReferredObjectRetrievalContext refCtx, final List<ConversionIssue> issueList, final ConversionHints hints) {
        if (input == null) {
            return;
        }
        final Optional<ZonedDateTime> changeForecastStart = getStartTime(input.getPhenomenonTime().getTimePeriod(), refCtx);
        final Optional<ZonedDateTime> changeForecastEnd = getEndTime(input.getPhenomenonTime().getTimePeriod(), refCtx);
        if (changeForecastStart.isPresent() && changeForecastEnd.isPresent()) {
            final Optional<PartialOrCompleteTimeInstant> startTime = Optional.of(PartialOrCompleteTimeInstant.of(changeForecastStart.get()));
            final Optional<PartialOrCompleteTimeInstant> endTime = Optional.of(PartialOrCompleteTimeInstant.of(changeForecastEnd.get()));

            final PartialOrCompleteTimePeriod changePeriod = PartialOrCompleteTimePeriod.builder().setStartTime(startTime).setEndTime(endTime).build();

            props.set(CHANGE_PERIOD, changePeriod);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Valid period is required in change forecasts"));
        }

        if (input.getChangeIndicator() != null) {
            props.set(CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.valueOf(input.getChangeIndicator().value()));
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Change indicator is required in change forecasts"));
        }

        final TAFForecastProperties forecatsProperties = new TAFForecastProperties();

        setTAFForecastProperties(forecatsProperties, input, issueList, refCtx, hints);

        props.set(TAFChangeForecastProperties.Name.FORECAST, forecatsProperties);
    }

    private static void setTAFForecastProperties(final TAFForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, final ConversionHints hints) {

        props.set(CLOUD_AND_VISIBILITY_OK, input.isCloudAndVisibilityOK());

        if (input.getPrevailingVisibility() != null) {
            asNumericMeasure(input.getPrevailingVisibility())//
                    .ifPresent(nm -> props.set(PREVAILING_VISIBILITY, nm));
        }

        if (input.getPrevailingVisibilityOperator() != null) {
            props.set(PREVAILING_VISIBILIT_OPERATOR, AviationCodeListUser.RelationalOperator.valueOf(input.getPrevailingVisibilityOperator().value()));
        }

        if (isNoSignificantWeather(input.getWeather())) {
            props.set(NO_SIGNIFICANT_WEATHER, true);
        } else if (!input.getWeather().isEmpty()) {
            final List<Weather> weatherList = new ArrayList<>();
            for (final AerodromeForecastWeatherType weatherType : input.getWeather()) {
                withWeatherBuilderFor(weatherType, hints, value -> weatherList.add(value.build()), issueList::add);
            }
            if (!weatherList.isEmpty()) {
                props.set(FORECAST_WEATHER, toUnmodifiableList(weatherList));
            }
        }

        if (input.getSurfaceWind() != null) {
            props.set(SURFACEWIND, setSurfaceWind(input.getSurfaceWind().getAerodromeSurfaceWindForecast()));
        }

        if (isNoSignificantCloud(input.getCloud())) {
            final TAFCloudForecastProperties prop = new TAFCloudForecastProperties();
            prop.set(TAFCloudForecastProperties.Name.NO_SIGNIFICANT_CLOUD, true);
            props.set(CLOUD_FORECAST, prop);
        } else if (input.getCloud() != null && input.getCloud().getNilReason().isEmpty() && input.getCloud().getAerodromeCloudForecast() != null) {
            props.set(CLOUD_FORECAST, setCloudForecast(input.getCloud().getAerodromeCloudForecast(), issueList));
        }
    }

    private static boolean isNoSignificantWeather(final List<AerodromeForecastWeatherType> weather) {
        return weather != null && weather.size() == 1 && weather.get(0)
                .getNilReason()
                .contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
    }

    private static boolean isNoSignificantCloud(final AerodromeCloudForecastPropertyType cloud) {
        return cloud != null && cloud.getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
    }

    private static TAFAirTemperatureForecast setTemperatures(final AerodromeAirTemperatureForecastPropertyType input,
            final ReferredObjectRetrievalContext refCtx) {
        final TAFAirTemperatureForecastImpl.Builder builder = TAFAirTemperatureForecastImpl.builder();

        if (input.getAerodromeAirTemperatureForecast().getMaximumAirTemperature() != null) {
            asNumericMeasure(input.getAerodromeAirTemperatureForecast().getMaximumAirTemperature())//
                    .ifPresent(builder::setMaxTemperature);
        }

        if (input.getAerodromeAirTemperatureForecast().getMaximumAirTemperatureTime() != null) {
            getCompleteTimeInstant(input.getAerodromeAirTemperatureForecast().getMaximumAirTemperatureTime(), refCtx)//
                    .ifPresent(builder::setMaxTemperatureTime);
        }

        if (input.getAerodromeAirTemperatureForecast().getMinimumAirTemperature() != null) {
            asNumericMeasure(input.getAerodromeAirTemperatureForecast().getMinimumAirTemperature())//
                    .ifPresent(builder::setMinTemperature);
        }

        if (input.getAerodromeAirTemperatureForecast().getMinimumAirTemperatureTime() != null) {
            getCompleteTimeInstant((input.getAerodromeAirTemperatureForecast().getMinimumAirTemperatureTime()), refCtx)//
                    .ifPresent(builder::setMinTemperatureTime);
        }
        return builder.build();
    }

    private static SurfaceWind setSurfaceWind(final AerodromeSurfaceWindForecastType swForecast) {
        final SurfaceWindImpl.Builder surfaceWind = SurfaceWindImpl.builder();

        surfaceWind.setVariableDirection(swForecast.isVariableWindDirection());

        if (swForecast.getMeanWindDirection() != null) {
            surfaceWind.setMeanWindDirection(asNumericMeasure(swForecast.getMeanWindDirection()));

        }

        if (swForecast.getMeanWindSpeed() != null) {
            asNumericMeasure(swForecast.getMeanWindSpeed())//
                    .ifPresent(surfaceWind::setMeanWindSpeed);
        }

        if (swForecast.getWindGustSpeed() != null) {
            asNumericMeasure(swForecast.getWindGustSpeed())//
                    .ifPresent(surfaceWind::setWindGust);
        }

        return surfaceWind.build();
    }

    private static TAFCloudForecastProperties setCloudForecast(final AerodromeCloudForecastType input, final List<ConversionIssue> issueList) {
        final TAFCloudForecastProperties prop = new TAFCloudForecastProperties();

        if (input.getVerticalVisibility() == null //
                || input.getVerticalVisibility().isNil() //
                || !input.getVerticalVisibility().getValue().getNilReason().isEmpty()) {
            prop.set(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY_MISSING, true);
        } else if (input.getVerticalVisibility() != null) {
            asNumericMeasure(input.getVerticalVisibility().getValue())//
                    .ifPresent(nm -> prop.set(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY, nm));
        }
        if (input.getLayer() != null) {
            prop.set(TAFCloudForecastProperties.Name.CLOUD_LAYER, setCloudLayers(input.getLayer()));
        }

        if (!prop.contains(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY) && prop.getList(TAFCloudForecastProperties.Name.CLOUD_LAYER, CloudLayer.class)
                .isEmpty()) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No vertical visibility or cloud layers in forecast"));
        }
        return prop;
    }

    private static List<CloudLayer> setCloudLayers(final List<CloudLayerPropertyType> input) {
        final List<CloudLayer> cloudLayers = new ArrayList<>();

        for (final CloudLayerPropertyType layer : input) {
            final CloudLayerImpl.Builder cloudLayer = CloudLayerImpl.builder();
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
        return toUnmodifiableList(cloudLayers);
    }

}
