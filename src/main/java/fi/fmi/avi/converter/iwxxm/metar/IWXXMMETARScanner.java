package fi.fmi.avi.converter.iwxxm.metar;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import fi.fmi.avi.model.metar.immutable.HorizontalVisibilityImpl;
import fi.fmi.avi.model.metar.immutable.ObservedSurfaceWindImpl;
import icao.iwxxm21.AerodromeHorizontalVisibilityPropertyType;
import icao.iwxxm21.AerodromeHorizontalVisibilityType;
import icao.iwxxm21.AerodromeSurfaceWindPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindType;
import icao.iwxxm21.MeteorologicalAerodromeObservationRecordType;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;
import icao.iwxxm21.MeteorologicalAerodromeReportStatusType;
import icao.iwxxm21.MeteorologicalAerodromeTrendForecastRecordType;
import icao.iwxxm21.SPECIType;

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

            //TODO:air temp (M)

            //TODO:dewpoint temp (M)

            //TODO:QNH (M)

            //TODO:recent weather (C)

            //TODO:wind shear (C)  (in own method?)

            //TODO:sea state (C) (in own method)

            //TODO:runway state (C) (in own method)

            //CAVOK:
            obsProps.set(ObservationRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, obsRecord.get().isCloudAndVisibilityOK());
            if (obsRecord.get().isCloudAndVisibilityOK()) {
                // visibility, rvr,  present weather, cloud must not be given with CAVOK
                if (obsRecord.get().getVisibility() != null && obsRecord.get().getVisibility().getAerodromeHorizontalVisibility() != null) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Visbility is not empty with CAVOK");
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

                //TODO:Present weather (C)

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
            windBuilder.setVariableDirection(wind.get().isVariableWindDirection());
            if (wind.get().getMeanWindSpeed() != null) {
                windBuilder.setMeanWindSpeed(asNumericMeasure(wind.get().getMeanWindSpeed()).get());
            } else {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean wind speed missing from METAR surface wind observation");
            }
            windBuilder.setMeanWindSpeedOperator(asRelationalOperator(wind.get().getMeanWindSpeedOperator()));
            windBuilder.setWindGust(asNumericMeasure(wind.get().getWindGustSpeed()));
            windBuilder.setWindGustOperator(asRelationalOperator(wind.get().getWindGustSpeedOperator()));
            windBuilder.setExtremeClockwiseWindDirection(asNumericMeasure(wind.get().getExtremeClockwiseWindDirection()));
            windBuilder.setExtremeCounterClockwiseWindDirection(asNumericMeasure(wind.get().getExtremeCounterClockwiseWindDirection()));
            resultHandler.accept(windBuilder);
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeSurfaceWindType value within " + "AerodromeSurfaceWindPropertyType or by reference");
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withVisibilityBuilderFor(final AerodromeHorizontalVisibilityPropertyType visProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<HorizontalVisibilityImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        Optional<AerodromeHorizontalVisibilityType> visibility = resolveProperty(visProp, AerodromeHorizontalVisibilityType.class, refCtx);
        if (visibility.isPresent()) {
            HorizontalVisibilityImpl.Builder visBuilder = new HorizontalVisibilityImpl.Builder();
            if (visibility.get().getPrevailingVisibility() != null) {
                visBuilder.setPrevailingVisibility(asNumericMeasure(visibility.get().getPrevailingVisibility()).get());
            } else {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Prevailing visibility missing from METAR horizontal visibility observation");
            }

        }
    }
}
