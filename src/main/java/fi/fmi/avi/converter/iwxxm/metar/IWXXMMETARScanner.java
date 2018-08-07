package fi.fmi.avi.converter.iwxxm.metar;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import icao.iwxxm21.*;
import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.immutable.ObservedSurfaceWindImpl;

;

/**
 * Created by rinne on 25/07/2018.
 */
public class IWXXMMETARScanner extends AbstractIWXXMScanner {

    public static List<ConversionIssue> collectMETARProperties(final MeteorologicalAerodromeObservationReportType input,
            final ReferredObjectRetrievalContext refCtx,
            final METARProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        MeteorologicalAerodromeReportStatusType status = input.getStatus();
        if (status != null) {
            properties.set(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.valueOf(status.name()));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is not given"));
        }

        properties.set(METARProperties.Name.SPECI, Boolean.valueOf(input instanceof SPECIType));

        // report metadata
        GenericReportProperties meta = new GenericReportProperties(input);
        retval.addAll(collectReportMetadata(input, meta, hints));
        properties.set(METARProperties.Name.REPORT_METADATA, meta);

        if (MeteorologicalAerodromeReportStatusType.MISSING == status) {
            boolean missingFound = false;
            for (String nilReason : input.getObservation().getNilReason()) {
                if ("missing".equals(nilReason)) {
                    missingFound = true;
                }
            }
            if (!missingFound) {
                retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "No nilReason 'missing' was found in result for " + "a missing METAR");
            }

            if (!input.getTrendForecast().isEmpty()) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Missing METARs may not contain Trend forecasts");
            }
            return retval;
        }

        properties.set(METARProperties.Name.AUTOMATED, input.isAutomatedStation());

        // observation
        Optional<OMObservationType> observation = resolveProperty(input.getObservation(), OMObservationType.class, refCtx);
        if (observation.isPresent()) {
            OMObservationProperties obsProps = new OMObservationProperties(observation.get());
            retval.addAll(collectObservationProperties(observation.get(), refCtx, obsProps, hints));
            properties.set(METARProperties.Name.OBSERVATION, obsProps);
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No Observation in METAR");
        }

        // trends
        for (OMObservationPropertyType obsProp : input.getTrendForecast()) {
            Optional<OMObservationType> trend = resolveProperty(obsProp, OMObservationType.class, refCtx);
            if (trend.isPresent()) {
                OMObservationProperties trendProps = new OMObservationProperties(trend.get());
                retval.addAll(collectTrendProperties(trend.get(), refCtx, trendProps, hints));
                properties.addToList(METARProperties.Name.TREND_FORECAST, trendProps);
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No Observation in METAR");
            }
        }

        return retval;
    }

    private static IssueList collectObservationProperties(final OMObservationType obs, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {
        IssueList retval = collectCommonObsMetadata(obs, refCtx, properties, "METAR Observation", hints);
        Optional<MeteorologicalAerodromeObservationRecordType> obsRecord = getAerodromeObservationRecordResult(obs, refCtx);
        if (obsRecord.isPresent()) {
            ObservationRecordProperties obsProps = new ObservationRecordProperties(obsRecord.get());

            //Surface wind (M)
            AerodromeSurfaceWindPropertyType windProp = obsRecord.get().getSurfaceWind();
            if (windProp != null) {
                withSurfaceWindBuilderFor(windProp, refCtx, (builder) -> {
                    obsProps.set(ObservationRecordProperties.Name.SURFACE_WIND, builder.build());
                }, retval::add);
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Surface wind is missing in non-missing METAR Observation");
            }

            Optional<NumericMeasure> airTemp = asNumericMeasure(obsRecord.get().getAirTemperature());
            if (airTemp.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.AIR_TEMPERATURE, airTemp.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Air temperature is missing in non-missing METAR Observation");
            }

            Optional<NumericMeasure> dewpointTemp = asNumericMeasure(obsRecord.get().getDewpointTemperature());
            if (dewpointTemp.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.DEWPOINT_TEMPERATURE, dewpointTemp.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Dewpoint temperature is missing in non-missing METAR Observation");
            }

            Optional<NumericMeasure> qnh = asNumericMeasure(obsRecord.get().getQnh());
            if (qnh.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.QNH, qnh.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "QNH is missing in non-missing METAR Observation");
            }

            for (AerodromeRecentWeatherType weather:obsRecord.get().getRecentWeather()) {
                withWeatherBuilderFor(weather, hints, (builder) -> {
                    obsProps.addToList(ObservationRecordProperties.Name.RECENT_WEATHER, builder.build());
                }, retval::add);
            }

            //TODO:wind shear (C)  (in own method?)

            //TODO:sea state (C) (in own method)

            //TODO:runway state (C) (in own method)

            //CAVOK:
            obsProps.set(ObservationRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, obsRecord.get().isCloudAndVisibilityOK());
            if (obsRecord.get().isCloudAndVisibilityOK()) {
                // visibility, rvr,  present weather, cloud must not be given with CAVOK
                if (obsRecord.get().getVisibility() != null && obsRecord.get().getVisibility().getAerodromeHorizontalVisibility() != null) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Visibility is not empty with CAVOK");
                }

                if (!obsRecord.get().getRvr().isEmpty()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Runway Visual Range is not empty with CAVOK");
                }

                if (!obsRecord.get().getPresentWeather().isEmpty()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Present weather is not empty with CAVOK");
                }

                if (obsRecord.get().getCloud() != null && !obsRecord.get().getCloud().isNil()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Cloud is not empty with CAVOK");
                }

            } else {
                //TODO:visibility (M)  (in own method)

                //TODO:RVR (C)  (in own method)

                for (AerodromePresentWeatherType weather:obsRecord.get().getPresentWeather()) {
                    withWeatherBuilderFor(weather, hints, (builder) -> {
                        obsProps.addToList(ObservationRecordProperties.Name.PRESENT_WEATHER, builder.build());
                    }, retval::add);
                }

                //TODO:Cloud (M)  (in own method)

            }
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation result record in METAR Observation");
        }
        return retval;
    }

    private static IssueList collectTrendProperties(final OMObservationType trend, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {
        IssueList retval = collectCommonObsMetadata(trend, refCtx, properties, "METAR Trend", hints);
        Optional<MeteorologicalAerodromeTrendForecastRecordType> trendRecord = getAerodromeTrendRecordResult(trend, refCtx);
        if (trendRecord.isPresent()) {
            TrendForecastRecordProperties trendProps = new TrendForecastRecordProperties(trendRecord.get());

        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation result record in METAR Trend");
        }
        return retval;
    }

    private static void withSurfaceWindBuilderFor(final AerodromeSurfaceWindPropertyType windProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<ObservedSurfaceWindImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        Optional<AerodromeSurfaceWindType> wind = resolveProperty(windProp, AerodromeSurfaceWindType.class, refCtx);
        if (wind.isPresent()) {
            ObservedSurfaceWindImpl.Builder windBuilder = new ObservedSurfaceWindImpl.Builder();
            windBuilder.setMeanWindDirection(asNumericMeasure(wind.get().getMeanWindDirection()));
            if (wind.get().isVariableWindDirection()) {
                windBuilder.setVariableDirection(true);
            } else {
                windBuilder.setVariableDirection(false);
            }
            if (wind.get().getMeanWindSpeed() != null) {
                windBuilder.setMeanWindSpeed(asNumericMeasure(wind.get().getMeanWindSpeed()).get());
            } else {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean wind speed missing from METAR surface wind observation");
            }
            Optional<AviationCodeListUser.RelationalOperator> speedOperator = asRelationalOperator(wind.get().getMeanWindSpeedOperator());
            speedOperator.ifPresent(windBuilder::setMeanWindSpeedOperator);

            Optional<NumericMeasure> gustSpeed = asNumericMeasure(wind.get().getWindGustSpeed());
            gustSpeed.ifPresent(windBuilder::setWindGust);

            Optional<AviationCodeListUser.RelationalOperator> gustOperator = asRelationalOperator(wind.get().getWindGustSpeedOperator());
            gustOperator.ifPresent(windBuilder::setWindGustOperator);

            if (wind.get().getExtremeClockwiseWindDirection() != null) {
                windBuilder.setExtremeClockwiseWindDirection(asNumericMeasure(wind.get().getExtremeClockwiseWindDirection()).get());
            }

            if (wind.get().getExtremeCounterClockwiseWindDirection() != null) {
                windBuilder.setExtremeCounterClockwiseWindDirection(asNumericMeasure(wind.get().getExtremeCounterClockwiseWindDirection()).get());
            }

            resultHandler.accept(windBuilder);
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeSurfaceWindType value within " + "AerodromeSurfaceWindPropertyType or by reference");
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }
}
