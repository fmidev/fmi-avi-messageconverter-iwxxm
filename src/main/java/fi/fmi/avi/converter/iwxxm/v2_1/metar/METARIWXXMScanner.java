package fi.fmi.avi.converter.iwxxm.v2_1.metar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import aero.aixm511.RunwayDirectionTimeSlicePropertyType;
import aero.aixm511.RunwayDirectionTimeSliceType;
import aero.aixm511.RunwayDirectionType;
import aero.aixm511.TextDesignatorType;
import aero.aixm511.ValBearingType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Scanner;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Serializer;
import fi.fmi.avi.converter.iwxxm.v2_1.OMObservationProperties;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.RunwayDirection;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.RunwayDirectionImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.metar.ObservedCloudLayer;
import fi.fmi.avi.model.metar.immutable.HorizontalVisibilityImpl;
import fi.fmi.avi.model.metar.immutable.ObservedCloudLayerImpl;
import fi.fmi.avi.model.metar.immutable.ObservedCloudsImpl;
import fi.fmi.avi.model.metar.immutable.ObservedSurfaceWindImpl;
import fi.fmi.avi.model.metar.immutable.RunwayStateImpl;
import fi.fmi.avi.model.metar.immutable.RunwayVisualRangeImpl;
import fi.fmi.avi.model.metar.immutable.SeaStateImpl;
import fi.fmi.avi.model.metar.immutable.WindShearImpl;
import icao.iwxxm21.AerodromeCloudForecastPropertyType;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AerodromeForecastWeatherType;
import icao.iwxxm21.AerodromeHorizontalVisibilityPropertyType;
import icao.iwxxm21.AerodromeHorizontalVisibilityType;
import icao.iwxxm21.AerodromeObservedCloudsType;
import icao.iwxxm21.AerodromePresentWeatherType;
import icao.iwxxm21.AerodromeRecentWeatherType;
import icao.iwxxm21.AerodromeRunwayStatePropertyType;
import icao.iwxxm21.AerodromeRunwayStateType;
import icao.iwxxm21.AerodromeRunwayVisualRangePropertyType;
import icao.iwxxm21.AerodromeRunwayVisualRangeType;
import icao.iwxxm21.AerodromeSeaStatePropertyType;
import icao.iwxxm21.AerodromeSeaStateType;
import icao.iwxxm21.AerodromeSurfaceWindPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindTrendForecastPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindTrendForecastType;
import icao.iwxxm21.AerodromeSurfaceWindType;
import icao.iwxxm21.AerodromeWindShearPropertyType;
import icao.iwxxm21.AerodromeWindShearType;
import icao.iwxxm21.CloudLayerPropertyType;
import icao.iwxxm21.CloudLayerType;
import icao.iwxxm21.DistanceWithNilReasonType;
import icao.iwxxm21.LengthWithNilReasonType;
import icao.iwxxm21.MeteorologicalAerodromeObservationRecordType;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;
import icao.iwxxm21.MeteorologicalAerodromeReportStatusType;
import icao.iwxxm21.MeteorologicalAerodromeTrendForecastRecordType;
import icao.iwxxm21.RunwayDirectionPropertyType;
import icao.iwxxm21.SPECIType;

/**
 * Created by rinne on 25/07/2018.
 */
public class METARIWXXMScanner extends AbstractIWXXM21Scanner {

    public static List<ConversionIssue> collectMETARProperties(final MeteorologicalAerodromeObservationReportType input,
            final ReferredObjectRetrievalContext refCtx, final METARProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        final MeteorologicalAerodromeReportStatusType status = input.getStatus();
        if (status != null) {
            properties.set(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.valueOf(status.name()));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is not given"));
        }

        properties.set(METARProperties.Name.SPECI, input instanceof SPECIType);

        // report metadata
        final GenericReportProperties meta = new GenericReportProperties();
        retval.addAll(AbstractIWXXM21Scanner.collectReportMetadata(input, meta, hints));
        properties.set(METARProperties.Name.REPORT_METADATA, meta);

        if (MeteorologicalAerodromeReportStatusType.MISSING == status) {
            boolean missingFound = false;
            if (input.getObservation() != null && input.getObservation().getOMObservation() != null) {
                final Object result = input.getObservation().getOMObservation().getResult();
                if (result instanceof Node) {
                    final Node resultNode = (Node) result;
                    final NamedNodeMap attrs = resultNode.getAttributes();
                    final Node nilReasonAttr = attrs.getNamedItem("nilReason");
                    if (nilReasonAttr != null) {
                        if ("missing".equals(nilReasonAttr.getNodeValue())) {
                            missingFound = true;
                        }
                    }
                }
            }

            if (!missingFound) {
                retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "No nilReason 'missing' was found in result for " + "a missing METAR");
            }

            if (!input.getTrendForecast().isEmpty()) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Missing METARs may not contain Trend forecasts");
            }
        }

        properties.set(METARProperties.Name.AUTOMATED, input.isAutomatedStation());

        // observation
        final Optional<OMObservationType> observation = resolveProperty(input.getObservation(), OMObservationType.class, refCtx);
        if (observation.isPresent()) {
            final OMObservationProperties obsProps = new OMObservationProperties();
            retval.addAll(collectObservationProperties(observation.get(), status, refCtx, properties, obsProps, hints));
            properties.set(METARProperties.Name.OBSERVATION, obsProps);
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No Observation in METAR");
        }

        // trends
        withEachNillableChild(input, input.getTrendForecast(), OMObservationPropertyType.class,
                new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "trendForecast"), refCtx, value -> {
                    final Optional<OMObservationType> trend = resolveProperty(value, OMObservationType.class, refCtx);
                    if (trend.isPresent()) {
                        final OMObservationProperties trendProps = new OMObservationProperties();
                        retval.addAll(collectTrendProperties(trend.get(), refCtx, trendProps, hints));
                        properties.addToList(METARProperties.Name.TREND_FORECAST, trendProps);
                    } else {
                        retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No Observation in METAR");
                    }
                }, nilReasons -> {
                    if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NO_SIGNIFICANT_CHANGE)) {
                        properties.set(METARProperties.Name.TREND_NO_SIGNIFICANT_CHANGES, Boolean.TRUE);
                        if (input.getTrendForecast().size() != 1) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                    "'No significant changes' (NOSIG) reported with other trend " + "forecasts, NOSIG must be the only trend if given");
                        }
                    }
                });

        return retval;
    }

    private static IssueList collectObservationProperties(final OMObservationType obs, final MeteorologicalAerodromeReportStatusType metarStatus,
            final ReferredObjectRetrievalContext refCtx, final METARProperties metarProps, final OMObservationProperties properties,
            final ConversionHints hints) {
        final IssueList retval = AbstractIWXXM21Scanner.collectCommonObsMetadata(obs, refCtx, properties, "METAR Observation", hints);
        //check type & observedProperty:
        final Optional<String> type = properties.get(OMObservationProperties.Name.TYPE, String.class);
        if (type.isPresent()) {
            if (!AviationCodeListUser.MET_AERODROME_OBSERVATION_TYPE.equals(type.get())) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Invalid observation type '" + type.get() + "', expected '" + AviationCodeListUser.MET_AERODROME_OBSERVATION_TYPE + "'");
            }
        }
        final Optional<String> observedProperty = properties.get(OMObservationProperties.Name.OBSERVED_PROPERTY, String.class);
        if (observedProperty.isPresent()) {
            if (!AviationCodeListUser.MET_AERODROME_OBSERVATION_PROPERTIES.equals(observedProperty.get())) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Invalid observed property '" + observedProperty.get() + "', expected '" + AviationCodeListUser.MET_AERODROME_OBSERVATION_PROPERTIES
                                + "'");
            }
        }
        if (obs.getPhenomenonTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> phenomenonTime = AbstractIWXXM21Serializer.getCompleteTimeInstant(obs.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                final Optional<PartialOrCompleteTimeInstant> resultTime = properties.get(OMObservationProperties.Name.RESULT_TIME,
                        PartialOrCompleteTimeInstant.class);
                if (resultTime.isPresent()) {
                    if (!phenomenonTime.equals(resultTime)) {
                        retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                                "resultTime '" + resultTime.get() + " is not equal to " + "phenomenonTime for METAR observation");
                    }
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve METAR observation phenomenonTime property"));
            }
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "phenomenonTime missing for METAR observation");
        }
        final Aerodrome aerodrome = properties.get(OMObservationProperties.Name.AERODROME, Aerodrome.class).orElse(null);

        final Optional<MeteorologicalAerodromeObservationRecordType> obsRecord = AbstractIWXXM21Scanner.getAerodromeObservationRecordResult(obs, refCtx);
        if (obsRecord.isPresent()) {
            final ObservationRecordProperties obsProps = new ObservationRecordProperties();

            //Surface wind (M)
            final AerodromeSurfaceWindPropertyType windProp = obsRecord.get().getSurfaceWind();
            if (windProp != null) {
                withSurfaceWindBuilderFor(windProp, refCtx, builder -> obsProps.set(ObservationRecordProperties.Name.SURFACE_WIND, builder.build()),
                        retval::add);
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Surface wind is missing in non-missing METAR Observation");
            }

            // Air temperature (M)
            final Optional<NumericMeasure> airTemp = asNumericMeasure(obsRecord.get().getAirTemperature());
            if (airTemp.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.AIR_TEMPERATURE, airTemp.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Air temperature is missing in non-missing METAR Observation");
            }

            //Dew point temperature (M)
            final Optional<NumericMeasure> dewpointTemp = asNumericMeasure(obsRecord.get().getDewpointTemperature());
            if (dewpointTemp.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.DEWPOINT_TEMPERATURE, dewpointTemp.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Dew point temperature is missing in non-missing METAR " + "Observation");
            }

            //QNH (M)
            final Optional<NumericMeasure> qnh = asNumericMeasure(obsRecord.get().getQnh());
            if (qnh.isPresent()) {
                obsProps.set(ObservationRecordProperties.Name.QNH, qnh.get());
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "QNH is missing in non-missing METAR Observation");
            }

            //Recent weather (C)
            for (final AerodromeRecentWeatherType weather : obsRecord.get().getRecentWeather()) {
                AbstractIWXXM21Scanner.withWeatherBuilderFor(weather, hints,
                        builder -> obsProps.addToList(ObservationRecordProperties.Name.RECENT_WEATHER, builder.build()), retval::add);
            }

            //wind shear (C)
            final AerodromeWindShearPropertyType wsProp = obsRecord.get().getWindShear();
            if (wsProp != null) {
                withWindShearBuilderFor(wsProp, aerodrome, refCtx, wsBuilder -> obsProps.set(ObservationRecordProperties.Name.WIND_SHEAR, wsBuilder.build()),
                        retval::add);
            }

            //sea state (C)
            final AerodromeSeaStatePropertyType ssProp = obsRecord.get().getSeaState();
            if (ssProp != null) {
                withSeaStateBuilderFor(ssProp, refCtx, ssBuilder -> obsProps.set(ObservationRecordProperties.Name.SEA_STATE, ssBuilder.build()), retval::add);
            }

            // runway state (C)
            boolean snoCloPresent = false;
            boolean nonSnoCloRWSPresent = false;
            for (final AerodromeRunwayStatePropertyType rvsProp : obsRecord.get().getRunwayState()) {
                final Optional<AerodromeRunwayStateType> rvs = resolveProperty(rvsProp, AerodromeRunwayStateType.class, refCtx);
                if (rvs.isPresent()) {
                    if (rvs.get().isSnowClosure() != null && rvs.get().isSnowClosure()) {
                        snoCloPresent = true;
                    } else {
                        nonSnoCloRWSPresent = true;
                        withRunwayStateBuilderFor(rvsProp, aerodrome, refCtx,
                                rvsBuilder -> obsProps.addToList(ObservationRecordProperties.Name.RUNWAY_STATE, rvsBuilder.build()), retval::add);
                    }
                }
            }
            if (snoCloPresent) {
                if (nonSnoCloRWSPresent) {
                    retval.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Additional runway state information given in addition to "
                            + "snow closure (applying for the entire aerodrome), this info is discarded");
                }
                metarProps.set(METARProperties.Name.SNOW_CLOSURE, Boolean.TRUE);
            }

            //CAVOK:
            obsProps.set(ObservationRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, obsRecord.get().isCloudAndVisibilityOK());
            if (obsRecord.get().isCloudAndVisibilityOK()) {
                // visibility, RVR,  present weather, cloud not allowed with CAVOK
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
                //Horizontal visibility (M)
                final AerodromeHorizontalVisibilityPropertyType visProp = obsRecord.get().getVisibility();
                if (visProp != null) {
                    withVisibilityBuilderFor(visProp, refCtx, builder -> obsProps.set(ObservationRecordProperties.Name.VISIBILITY, builder.build()),
                            retval::add);
                } else {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Horizontal visibility missing in METAR Observation");
                }

                //RVR (C)
                for (final AerodromeRunwayVisualRangePropertyType rvrProp : obsRecord.get().getRvr()) {
                    withRunwayVisualRangeBuilderFor(rvrProp, aerodrome, refCtx,
                            rvrBuilder -> obsProps.addToList(ObservationRecordProperties.Name.RUNWAY_VISUAL_RANGE, rvrBuilder.build()), retval::add);
                }

                // Present weather (C)
                for (final AerodromePresentWeatherType weather : obsRecord.get().getPresentWeather()) {
                    AbstractIWXXM21Scanner.withWeatherBuilderFor(weather, hints,
                            builder -> obsProps.addToList(ObservationRecordProperties.Name.PRESENT_WEATHER, builder.build()), retval::add);
                }

                // Cloud (M)
                final JAXBElement<MeteorologicalAerodromeObservationRecordType.Cloud> cloudElement = obsRecord.get().getCloud();
                if (cloudElement != null) {
                    withObservedCloudBuilderFor(cloudElement, refCtx,
                            cloudBuilder -> obsProps.set(ObservationRecordProperties.Name.CLOUD, cloudBuilder.build()), retval::add);
                }
            }
            properties.set(OMObservationProperties.Name.RESULT, obsProps);
        } else if (MeteorologicalAerodromeReportStatusType.MISSING != metarStatus) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation result record in METAR Observation");
        }
        return retval;
    }

    private static IssueList collectTrendProperties(final OMObservationType trend, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {
        final IssueList retval = AbstractIWXXM21Scanner.collectCommonObsMetadata(trend, refCtx, properties, "METAR Trend", hints);
        //check type & observedProperty:
        final Optional<String> type = properties.get(OMObservationProperties.Name.TYPE, String.class);
        if (type.isPresent()) {
            if (!AviationCodeListUser.TREND_FORECAST_OBSERVATION_TYPE.equals(type.get())) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Invalid observation type '" + type.get() + "', expected '" + AviationCodeListUser.TREND_FORECAST_OBSERVATION_TYPE + "'");
            }
        }
        final Optional<String> observedProperty = properties.get(OMObservationProperties.Name.OBSERVED_PROPERTY, String.class);
        if (observedProperty.isPresent()) {
            if (!AviationCodeListUser.TREND_FORECAST_PROPERTIES.equals(observedProperty.get())) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Invalid observed property '" + observedProperty.get() + "', expected '" + AviationCodeListUser.TREND_FORECAST_PROPERTIES + "'");
            }
        }
        //phenomenonTime (C)
        if (trend.getPhenomenonTime() != null) {
            final Optional<PartialOrCompleteTimePeriod> phenomenonTime = AbstractIWXXM21Serializer.getCompleteTimePeriod(trend.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                final Optional<PartialOrCompleteTimeInstant> start = phenomenonTime.get().getStartTime();
                final Optional<PartialOrCompleteTimeInstant> end = phenomenonTime.get().getEndTime();
                if (start.isPresent() && end.isPresent() && start.get().equals(end.get())) {
                    // zero-length period, silently convert to an instant:
                    properties.set(OMObservationProperties.Name.PHENOMENON_TIME, start.get());
                } else {
                    properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                }
            } else {
                final Optional<PartialOrCompleteTimeInstant> phenomenonTimeInstant = AbstractIWXXM21Serializer.getCompleteTimeInstant(trend.getPhenomenonTime(),
                        refCtx);
                phenomenonTimeInstant.ifPresent(time -> properties.set(OMObservationProperties.Name.PHENOMENON_TIME, time));
            }

        }
        final MeteorologicalAerodromeTrendForecastRecordType trendRecord = AbstractIWXXM21Scanner.getAerodromeTrendRecordResult(trend, refCtx).orElse(null);
        if (trendRecord == null) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation result record in METAR Trend");
        } else {
            final TrendForecastRecordProperties trendProps = new TrendForecastRecordProperties();
            //change indicator (M)
            if (trendRecord.getChangeIndicator() != null) {
                trendProps.set(TrendForecastRecordProperties.Name.CHANGE_INDICATOR,
                        AviationCodeListUser.TrendForecastChangeIndicator.valueOf(trendRecord.getChangeIndicator().name()));
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Change indicator missing in METAR Trend");
            }

            //wind (C)
            if (trendRecord.getSurfaceWind() != null) {
                withForecastSurfaceWindBuilderFor(trendRecord.getSurfaceWind(), refCtx,
                        windBuilder -> trendProps.set(TrendForecastRecordProperties.Name.SURFACE_WIND, windBuilder.build()), retval::add);
            }

            //CAVOK (C)
            trendProps.set(TrendForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, trendRecord.isCloudAndVisibilityOK());
            if (trendRecord.isCloudAndVisibilityOK()) {
                // no visibility, weather, cloud allowed with CAVOK:
                if (trendRecord.getPrevailingVisibility() != null) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Visibility is not empty with CAVOKin trend");
                }

                if (!trendRecord.getForecastWeather().isEmpty()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Forecast weather is not empty with CAVOK trend");
                }

                if (trendRecord.getCloud() != null && !trendRecord.getCloud().isNil()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Cloud is not empty with CAVOK trend");
                }
            } else {
                //prevailing visibility (C)
                asNumericMeasure(trendRecord.getPrevailingVisibility())//
                        .ifPresent(measure -> trendProps.set(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY, measure));
                asRelationalOperator(trendRecord.getPrevailingVisibilityOperator())//
                        .ifPresent(operator -> trendProps.set(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, operator));

                //forecast weather (C) (incl. NSW)
                withEachNillableChild(trendRecord, trendRecord.getForecastWeather(), AerodromeForecastWeatherType.class,
                        new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm2"), "forecastWeather"), refCtx,
                        value -> AbstractIWXXM21Scanner.withWeatherBuilderFor(value, hints,
                                builder -> trendProps.addToList(TrendForecastRecordProperties.Name.FORECAST_WEATHER, builder.build()), retval::add),
                        nilReasons -> {
                            if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)) {
                                trendProps.set(TrendForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.TRUE);
                                if (trendRecord.getForecastWeather().size() != 1) {
                                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                            "'No significant weather' (NSW) was reported with other "
                                                    + "weather phenomena, NSW must be the only forecastWeather property if given");
                                }
                            }
                        });

                //cloud (C) (incl. NSC)
                final JAXBElement<AerodromeCloudForecastPropertyType> cloudElem = trendRecord.getCloud();
                withTrendCloudBuilderFor(cloudElem, refCtx, cloudBuilder -> trendProps.set(TrendForecastRecordProperties.Name.CLOUD, cloudBuilder.build()),
                        retval::add);
            }
            properties.set(OMObservationProperties.Name.RESULT, trendProps);
        }
        return retval;
    }

    private static void withSurfaceWindBuilderFor(final AerodromeSurfaceWindPropertyType windProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<ObservedSurfaceWindImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        final AerodromeSurfaceWindType wind = resolveProperty(windProp, AerodromeSurfaceWindType.class, refCtx).orElse(null);
        if (wind == null) {
            issue = new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeSurfaceWindType value within AerodromeSurfaceWindPropertyType or by reference");
        } else {
            final ObservedSurfaceWindImpl.Builder windBuilder = ObservedSurfaceWindImpl.builder();
            windBuilder.setMeanWindDirection(asNumericMeasure(wind.getMeanWindDirection()));
            windBuilder.setVariableDirection(wind.isVariableWindDirection());
            if (wind.getMeanWindSpeed() == null) {
                issue = new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Mean wind speed missing from METAR surface wind observation");
            }
            asNumericMeasure(wind.getMeanWindSpeed()).ifPresent(windBuilder::setMeanWindSpeed);
            windBuilder.setMeanWindSpeedOperator(asRelationalOperator(wind.getMeanWindSpeedOperator()));
            windBuilder.setWindGust(asNumericMeasure(wind.getWindGustSpeed()));
            windBuilder.setWindGustOperator(asRelationalOperator(wind.getWindGustSpeedOperator()));
            windBuilder.setExtremeClockwiseWindDirection(asNumericMeasure(wind.getExtremeClockwiseWindDirection()));
            windBuilder.setExtremeCounterClockwiseWindDirection(asNumericMeasure(wind.getExtremeCounterClockwiseWindDirection()));
            if (issue == null) {
                resultHandler.accept(windBuilder);
            }
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withForecastSurfaceWindBuilderFor(final AerodromeSurfaceWindTrendForecastPropertyType windProp,
            final ReferredObjectRetrievalContext refCtx, final Consumer<SurfaceWindImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        final AerodromeSurfaceWindTrendForecastType wind = resolveProperty(windProp, AerodromeSurfaceWindTrendForecastType.class, refCtx).orElse(null);
        if (wind == null) {
            issue = new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeSurfaceWindTrendForecastType value within AerodromeSurfaceWindTrendForecastPropertyType or by reference");
        } else {
            final SurfaceWindImpl.Builder windBuilder = SurfaceWindImpl.builder();
            asNumericMeasure(wind.getMeanWindDirection()).ifPresent(windBuilder::setMeanWindDirection);
            asNumericMeasure(wind.getMeanWindSpeed()).ifPresent(windBuilder::setMeanWindSpeed);
            if (wind.getMeanWindSpeed() == null) {
                issue = new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Mean wind speed missing from METAR trend surface wind forecast");
            }
            windBuilder.setMeanWindSpeedOperator(asRelationalOperator(wind.getMeanWindSpeedOperator()));
            windBuilder.setWindGust(asNumericMeasure(wind.getWindGustSpeed()));
            windBuilder.setWindGustOperator(asRelationalOperator(wind.getWindGustSpeedOperator()));

            if (issue == null) {
                resultHandler.accept(windBuilder);
            }
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withVisibilityBuilderFor(final AerodromeHorizontalVisibilityPropertyType visProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<HorizontalVisibilityImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        final AerodromeHorizontalVisibilityType visibility = resolveProperty(visProp, AerodromeHorizontalVisibilityType.class, refCtx).orElse(null);
        if (visibility == null) {
            issue = new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeHorizontalVisibilityType value within AerodromeHorizontalVisibilityPropertyType or by reference");
        } else {
            final HorizontalVisibilityImpl.Builder visBuilder = HorizontalVisibilityImpl.builder();
            if (visibility.getPrevailingVisibility() == null) {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Prevailing visibility missing from METAR horizontal visibility observation");
            }
            asNumericMeasure(visibility.getPrevailingVisibility()).ifPresent(visBuilder::setPrevailingVisibility);
            visBuilder.setPrevailingVisibilityOperator(asRelationalOperator(visibility.getPrevailingVisibilityOperator()));
            visBuilder.setMinimumVisibility(asNumericMeasure(visibility.getMinimumVisibility()));
            if (issue == null) {
                resultHandler.accept(visBuilder);
            }
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withWindShearBuilderFor(final AerodromeWindShearPropertyType shearProp, final Aerodrome aerodrome,
            final ReferredObjectRetrievalContext refCtx, final Consumer<WindShearImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        final Optional<AerodromeWindShearType> windShear = resolveProperty(shearProp, AerodromeWindShearType.class, refCtx);
        if (windShear.isPresent()) {
            final WindShearImpl.Builder wsBuilder = WindShearImpl.builder();
            if (windShear.get().isAllRunways() != null && windShear.get().isAllRunways()) {
                wsBuilder.setAppliedToAllRunways(windShear.get().isAllRunways());
                if (!windShear.get().getRunway().isEmpty()) {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                            "WindShear has flag 'allRunways' but also contains a " + "non-empty list of runway elements, ignoring the individual runways");
                }
            } else {
                final List<RunwayDirection> directions = new ArrayList<>();
                for (final RunwayDirectionPropertyType rwdProp : windShear.get().getRunway()) {
                    withRunwayDirectionBuilderFor(rwdProp, aerodrome, refCtx, rwdBuilder -> directions.add(rwdBuilder.build()), issues::add);
                }
                wsBuilder.setRunwayDirections(directions);
            }
            resultHandler.accept(wsBuilder);
        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not resolve AerodromeWindShearType from within " + "AerodromeWindShearPropertyType");
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withSeaStateBuilderFor(final AerodromeSeaStatePropertyType seaStateProperty, final ReferredObjectRetrievalContext refCtx,
            final Consumer<SeaStateImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        final AerodromeSeaStateType seaState = resolveProperty(seaStateProperty, AerodromeSeaStateType.class, refCtx).orElse(null);
        if (seaState == null) {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not resolve AerodromeSeaStateType from within " + "AerodromeSeaStatePropertyType");
        } else {
            final SeaStateImpl.Builder ssBuilder = SeaStateImpl.builder();
            //Either temp AND (state OR sig wave height)
            if (seaState.getSeaSurfaceTemperature() == null) {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Sea surface temperature missing in SeaState");
            }
            asNumericMeasure(seaState.getSeaSurfaceTemperature()).ifPresent(ssBuilder::setSeaSurfaceTemperature);
            if (seaState.getSeaState() != null && seaState.getSeaState().getHref() != null) {
                if (seaState.getSeaState().getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SEA_SURFACE_STATE)) {
                    final String code = seaState.getSeaState().getHref().substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SEA_SURFACE_STATE.length());
                    ssBuilder.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.fromInt(Integer.parseInt(code)));
                } else {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Invalid codelist value used for sea surface state, should start " + "with "
                                    + AviationCodeListUser.CODELIST_VALUE_PREFIX_SEA_SURFACE_STATE);
                }
                if (seaState.getSignificantWaveHeight() != null) {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Significant wave height not allowed with sea surface state in " + "SeaState");
                }
            } else if (seaState.getSignificantWaveHeight() != null) {
                ssBuilder.setSignificantWaveHeight(asNumericMeasure(seaState.getSignificantWaveHeight()));
            } else {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "One of sea surface state or significant wave height must be " + "given in SeaState");
            }
            resultHandler.accept(ssBuilder);
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withRunwayStateBuilderFor(final AerodromeRunwayStatePropertyType rwsProp, final Aerodrome aerodrome,
            final ReferredObjectRetrievalContext refCtx, final Consumer<RunwayStateImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        final Optional<AerodromeRunwayStateType> runwayState = resolveProperty(rwsProp, AerodromeRunwayStateType.class, refCtx);
        if (runwayState.isPresent()) {
            if (runwayState.get().isSnowClosure() != null && runwayState.get().isSnowClosure()) {
                throw new IllegalArgumentException("Must not give runway state with snow closure as input, no need to parse as RunwayState object. SNOCLO "
                        + "applies to the whole aerodrome, not an individual runway");
            }
            final RunwayStateImpl.Builder rwsBuilder = RunwayStateImpl.builder();
            if (runwayState.get().isAllRunways() != null && runwayState.get().isAllRunways()) {
                rwsBuilder.setAppliedToAllRunways(true);
                if (runwayState.get().getRunway() != null) {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                            "Runway should not be given if 'allRunways' is true in " + "RunwayState");
                }
            } else {
                final RunwayDirectionPropertyType rwdProp = runwayState.get().getRunway();
                if (rwdProp != null) {
                    withRunwayDirectionBuilderFor(rwdProp, aerodrome, refCtx, rwdBuilder -> rwsBuilder.setRunwayDirection(rwdBuilder.build()), issues::add);
                } else {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "Runway designator must be given in RunwayState unless " + "'allRunways' flag is given");
                }
            }
            if (runwayState.get().isCleared() != null && runwayState.get().isCleared()) {
                rwsBuilder.setCleared(true);
                if (runwayState.get().getDepositType() != null || runwayState.get().getContamination() != null
                        || runwayState.get().getEstimatedSurfaceFrictionOrBrakingAction() != null || runwayState.get().getDepthOfDeposit() != null) {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                            "No deposit, contamination, surface friction / braking " + "action or depth of deposit allowed with cleared flag");
                }
            } else {
                //deposit
                if (runwayState.get().getDepositType() != null && runwayState.get().getDepositType().getHref() != null) {
                    if (runwayState.get().getDepositType().getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_DEPOSITS)) {
                        final String code = runwayState.get()
                                .getDepositType()
                                .getHref()
                                .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_DEPOSITS.length());
                        rwsBuilder.setDeposit(AviationCodeListUser.RunwayDeposit.fromInt(Integer.parseInt(code)));
                    } else {
                        issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Unknown codelist reference for runway state " + "deposit '" + runwayState.get().getDepositType().getHref() + "'");
                    }
                }

                //contamination
                if (runwayState.get().getContamination() != null && runwayState.get().getContamination().getHref() != null) {
                    if (runwayState.get().getContamination().getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_CONTAMINATION)) {
                        final String code = runwayState.get()
                                .getContamination()
                                .getHref()
                                .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_CONTAMINATION.length());
                        rwsBuilder.setContamination(AviationCodeListUser.RunwayContamination.fromInt(Integer.parseInt(code)));
                    } else {
                        issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Unknown codelist reference for runway state " + "contamination '" + runwayState.get().getContamination().getHref() + "'");
                    }
                }

                //depth of deposit
                if (runwayState.get().getDepthOfDeposit() != null) {
                    final JAXBElement<DistanceWithNilReasonType> dod = runwayState.get().getDepthOfDeposit();
                    if (dod.isNil()) {
                        if (dod.getValue() != null && !dod.getValue().getNilReason().isEmpty()) {
                            if (dod.getValue().getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)) {
                                rwsBuilder.setDepthInsignificant(true);
                            } else if (dod.getValue().getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                                rwsBuilder.setDepthNotMeasurable(true);
                            } else if (dod.getValue().getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN)) {
                                rwsBuilder.setRunwayNotOperational(true);
                            } else {
                                issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                        "Unknown nilReason for runway state depth of deposit '" + dod.getValue().getNilReason()
                                                + "', treating as notMeasurable");
                                rwsBuilder.setDepthNotMeasurable(true);
                            }
                        } else {
                            issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "No nilReason for nil runway state depth of deposit, treating as notMeasurable");
                            rwsBuilder.setDepthNotMeasurable(true);
                        }
                    } else if (dod.getValue() != null) {
                        rwsBuilder.setDepthOfDeposit(NumericMeasureImpl.of(dod.getValue().getValue(), dod.getValue().getUom()));
                    }
                }

                //friction/braking action
                if (runwayState.get().getEstimatedSurfaceFrictionOrBrakingAction() != null
                        && runwayState.get().getEstimatedSurfaceFrictionOrBrakingAction().getHref() != null) {
                    if (runwayState.get()
                            .getEstimatedSurfaceFrictionOrBrakingAction()
                            .getHref()
                            .startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_SURFACE_FRICTION_OR_BRAKING_ACTION)) {
                        final String code = runwayState.get()
                                .getEstimatedSurfaceFrictionOrBrakingAction()
                                .getHref()
                                .substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_RUNWAY_SURFACE_FRICTION_OR_BRAKING_ACTION.length());
                        final AviationCodeListUser.BrakingAction ba = AviationCodeListUser.BrakingAction.fromInt(Integer.parseInt(code));
                        if (ba == null) {
                            //not one of the BA codes, must be surface friction
                            final int intValue = Integer.parseInt(code);
                            if (intValue == 127) {
                                rwsBuilder.setRunwayNotOperational(true);
                            } else if (intValue == 99) {
                                rwsBuilder.setEstimatedSurfaceFrictionUnreliable(true);
                            } else if (intValue >= 0 && intValue <= 98) {
                                final double friction = Integer.parseInt(code) / 100.0;
                                rwsBuilder.setEstimatedSurfaceFriction(friction);
                            } else {
                                issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                        "Unknown estimated surface friction / braking " + "action code '" + intValue + "'");
                            }
                        } else {
                            rwsBuilder.setBrakingAction(ba);
                        }
                    } else {
                        issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Invalid codelist reference for " + "estimated surface friction / braking action '" + runwayState.get()
                                        .getEstimatedSurfaceFrictionOrBrakingAction()
                                        .getHref() + "'");
                    }
                }

            }

            resultHandler.accept(rwsBuilder);
        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Could not resolve AerodromeRunwayStateType from within " + "AerodromeRunwayStatePropertyType");
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withRunwayVisualRangeBuilderFor(final AerodromeRunwayVisualRangePropertyType rvrProp, final Aerodrome aerodrome,
            final ReferredObjectRetrievalContext refCtx, final Consumer<RunwayVisualRangeImpl.Builder> resultHandler,
            final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        final Optional<AerodromeRunwayVisualRangeType> rvr = resolveProperty(rvrProp, AerodromeRunwayVisualRangeType.class, refCtx);
        if (rvr.isPresent()) {
            final RunwayVisualRangeImpl.Builder rvrBuilder = RunwayVisualRangeImpl.builder();
            final RunwayDirectionPropertyType rwdProp = rvr.get().getRunway();
            if (rwdProp != null) {
                withRunwayDirectionBuilderFor(rwdProp, aerodrome, refCtx, rwdBuilder -> {
                    rvrBuilder.setRunwayDirection(rwdBuilder.build());
                    rvrBuilder.setMeanRVR(asNumericMeasure(rvr.get().getMeanRVR()));
                    rvrBuilder.setMeanRVROperator(asRelationalOperator(rvr.get().getMeanRVROperator()));
                    if (rvr.get().getPastTendency() != null) {
                        rvrBuilder.setPastTendency(AviationCodeListUser.VisualRangeTendency.valueOf(rvr.get().getPastTendency().name()));
                    }
                    //Note: varying RVR is not supported in IWXXM 2.1:
                    rvrBuilder.setVaryingRVRMaximum(Optional.empty());
                    rvrBuilder.setVaryingRVRMaximumOperator(Optional.empty());
                    rvrBuilder.setVaryingRVRMinimum(Optional.empty());
                    rvrBuilder.setVaryingRVRMinimumOperator(Optional.empty());
                    resultHandler.accept(rvrBuilder);
                }, issues::add);
            } else {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No runway property in RunwayVisualRange");
            }
        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Unable to resolve AerodromeRunwayVisualRange from " + "AerodromeRunwayVisualRangeProperty");
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withObservedCloudBuilderFor(final JAXBElement<MeteorologicalAerodromeObservationRecordType.Cloud> cloudElement,
            final ReferredObjectRetrievalContext refCtx, final Consumer<ObservedCloudsImpl.Builder> resultHandler,
            final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        if (cloudElement != null && cloudElement.getValue() != null) {
            final MeteorologicalAerodromeObservationRecordType.Cloud cloud = cloudElement.getValue();
            if (cloud != null) {
                final ObservedCloudsImpl.Builder cloudBuilder = ObservedCloudsImpl.builder();
                final AerodromeObservedCloudsType obsClouds = cloud.getAerodromeObservedClouds();
                if (cloud.getNilReason().isEmpty()) {
                    if (obsClouds != null) {
                        if (obsClouds.getVerticalVisibility() != null) {
                            final JAXBElement<LengthWithNilReasonType> vv = obsClouds.getVerticalVisibility();
                            if (vv != null) {
                                if (vv.getValue() != null) {
                                    final LengthWithNilReasonType length = vv.getValue();
                                    if (length.getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                                        cloudBuilder.setVerticalVisibilityUnobservableByAutoSystem(true);
                                    } else {
                                        cloudBuilder.setVerticalVisibility(asNumericMeasure(vv.getValue()));
                                    }
                                } else {
                                    issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                            "No value for vertical visibility element, strange");
                                }
                            }
                        } else if (!obsClouds.getLayer().isEmpty()) {
                            final List<ObservedCloudLayer> layers = new ArrayList<>();
                            withEachNillableChild(obsClouds, obsClouds.getLayer(), AerodromeObservedCloudsType.Layer.class,
                                    new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "layer"), refCtx,
                                    value -> withObservedCloudLayerBuilderFor(value, refCtx, layerBuilder -> layers.add(layerBuilder.build()), issues::add,
                                            "observed cloud"), nilReasons -> {
                                        final ObservedCloudLayerImpl.Builder layerBuilder = ObservedCloudLayerImpl.builder();
                                        if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                                            layerBuilder.setHeightUnobservableByAutoSystem(true);
                                            layerBuilder.setAmountUnobservableByAutoSystem(true);
                                        } else if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_DETECTED_BY_AUTO_SYSTEM)) {
                                            layerBuilder.setHeightNotDetectedByAutoSystem(true);
                                            layerBuilder.setAmountNotDetectedByAutoSystem(true);
                                        }
                                        layers.add(layerBuilder.build());
                                    });
                            cloudBuilder.setLayers(layers);
                        } else {
                            issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                    "ObservedClouds contains neither vertical visibility nor any cloud layers");
                        }
                    } else {
                        issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                "Cloud property contains neither a nilReason nor an " + "AerodromeObservedClouds child");
                    }
                } else {
                    cloudBuilder.setNoCloudsDetectedByAutoSystem(
                            cloud.getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_DETECTED_BY_AUTO_SYSTEM));
                    cloudBuilder.setNoSignificantCloud(
                            cloud.getNilReason().contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE));
                }
                resultHandler.accept(cloudBuilder);
            }
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withTrendCloudBuilderFor(final JAXBElement<AerodromeCloudForecastPropertyType> cloudElement,
            final ReferredObjectRetrievalContext refCtx, final Consumer<CloudForecastImpl.Builder> resultHandler,
            final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        if (cloudElement != null && cloudElement.getValue() != null) {
            final AerodromeCloudForecastPropertyType cloudProp = cloudElement.getValue();
            if (cloudProp != null) {
                final CloudForecastImpl.Builder cloudBuilder = CloudForecastImpl.builder();
                if (cloudProp.getNilReason().isEmpty()) {
                    final Optional<AerodromeCloudForecastType> cloudFct = resolveProperty(cloudProp, AerodromeCloudForecastType.class, refCtx);
                    if (cloudFct.isPresent()) {
                        if (cloudFct.get().getVerticalVisibility() != null) {
                            final JAXBElement<LengthWithNilReasonType> vv = cloudFct.get().getVerticalVisibility();
                            if (vv != null) {
                                if (vv.getValue() != null) {
                                    final LengthWithNilReasonType length = vv.getValue();
                                    if (length.getNilReason().isEmpty()) {
                                        cloudBuilder.setVerticalVisibility(asNumericMeasure(vv.getValue()));
                                    } else {
                                        issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                                "Vertical visibility given with " + "nilReason in Trend cloud forecast, ignoring");
                                    }
                                } else {
                                    issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                            "No value for vertical visibility element, strange");
                                }
                            }
                        } else if (!cloudFct.get().getLayer().isEmpty()) {
                            final List<CloudLayer> layers = new ArrayList<>();
                            final List<AerodromeCloudForecastType.Layer> inputLayers = cloudFct.get().getLayer();
                            for (final CloudLayerPropertyType layerProp : inputLayers) {
                                if (layerProp != null) {
                                    AbstractIWXXM21Scanner.withCloudLayerBuilderFor(layerProp, refCtx, layerBuilder -> layers.add(layerBuilder.build()),
                                            issues::add, "trend cloud");
                                } else {
                                    issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "Nil cloud layer in trend forecast");
                                }
                            }
                            cloudBuilder.setLayers(layers);
                        } else {
                            issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                    "Trend cloud forecast contains neither vertical visibility nor any cloud layers");
                        }
                    } else {
                        issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                "Cloud forecast property contains neither a nilReason nor an " + "AerodromeCloudForecastType child");
                    }
                } else {
                    cloudBuilder.setNoSignificantCloud(cloudProp.getNilReason()
                            .stream()
                            .anyMatch(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE::equals));
                }
                resultHandler.accept(cloudBuilder);
            }
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    private static void withRunwayDirectionBuilderFor(final RunwayDirectionPropertyType rwdProp, final Aerodrome aerodrome,
            final ReferredObjectRetrievalContext refCtx, final Consumer<RunwayDirectionImpl.Builder> resultHandler,
            final Consumer<ConversionIssue> issueHandler) {
        final IssueList issues = new IssueList();
        final Optional<RunwayDirectionType> rwd = resolveProperty(rwdProp, RunwayDirectionType.class, refCtx);
        if (rwd.isPresent()) {
            final List<RunwayDirectionTimeSlicePropertyType> slicePropList = rwd.get().getTimeSlice();
            if (slicePropList == null || slicePropList.isEmpty()) {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No timeSlices in RunwayDirection");
            } else {
                Optional<RunwayDirectionTimeSliceType> slice = Optional.empty();
                if (slicePropList.size() > 1) {
                    issues.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                            "More than one timeSlice within RunwayDirection, choosing the first SNAPSHOT");
                }
                for (final RunwayDirectionTimeSlicePropertyType sliceProp : slicePropList) {
                    slice = resolveProperty(sliceProp, RunwayDirectionTimeSliceType.class, refCtx);
                    if (slice.isPresent() && "SNAPSHOT".equals(slice.get().getInterpretation())) {
                        break;
                    }
                }
                if (slice.isPresent()) {
                    final RunwayDirectionImpl.Builder rwdBuilder = RunwayDirectionImpl.builder();
                    final TextDesignatorType designator = slice.get().getDesignator();
                    final ValBearingType trueBearing = slice.get().getTrueBearing();
                    rwdBuilder.setNullableAssociatedAirportHeliport(aerodrome);
                    if (designator != null && designator.getValue() != null) {
                        rwdBuilder.setDesignator(designator.getValue());
                    }
                    if (trueBearing != null && trueBearing.getValue() != null) {
                        rwdBuilder.setTrueBearing(trueBearing.getValue().doubleValue());
                    }
                    resultHandler.accept(rwdBuilder);
                } else {
                    issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Unable to find RunwayDirectionTimeSliceType");
                }
            }
        } else {
            final String message;
            if (rwdProp.getHref() != null) {
                message = "Unable to resolve the RunwayDirectionType within the runway element, id:" + rwdProp.getHref().substring(1);
            } else {
                message = "Unable to resolve the RunwayDirectionType within the runway element";
            }
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, message);
        }
        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

    protected static void withObservedCloudLayerBuilderFor(final AerodromeObservedCloudsType.Layer layerProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<ObservedCloudLayerImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler, final String contextPath) {
        final IssueList issues = new IssueList();
        final ObservedCloudLayerImpl.Builder layerBuilder = ObservedCloudLayerImpl.builder();
        final Optional<CloudLayerType> layer = resolveProperty(layerProp, CloudLayerType.class, refCtx);
        if (layer.isPresent()) {
            AbstractIWXXM21Scanner.withCloudBase(layer.get(), refCtx, layerBuilder::setBase, nilReasons -> {
                if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                    layerBuilder.setHeightUnobservableByAutoSystem(true);
                }
                if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_DETECTED_BY_AUTO_SYSTEM)) {
                    layerBuilder.setHeightNotDetectedByAutoSystem(true);
                }
            }, issues::add, contextPath);

            AbstractIWXXM21Scanner.withCloudAmount(layer.get(), refCtx, layerBuilder::setAmount, nilReasons -> {
                if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                    layerBuilder.setAmountUnobservableByAutoSystem(true);
                }
                if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_DETECTED_BY_AUTO_SYSTEM)) {
                    layerBuilder.setAmountNotDetectedByAutoSystem(true);
                }
            }, issues::add, contextPath);

            AbstractIWXXM21Scanner.withCloudType(layer.get(), refCtx, layerBuilder::setCloudType, nilReasons -> {
                if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOT_OBSERVABLE)) {
                    layerBuilder.setCloudTypeUnobservableByAutoSystem(true);
                }
            }, issues::add, contextPath);

            resultHandler.accept(layerBuilder);
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve cloud layer in " + contextPath));
        }

        for (final ConversionIssue issue : issues) {
            issueHandler.accept(issue);
        }
    }

}
