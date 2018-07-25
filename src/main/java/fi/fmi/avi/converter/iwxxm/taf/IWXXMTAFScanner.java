package fi.fmi.avi.converter.iwxxm.taf;

import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.ReferenceType;
import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import org.w3c.dom.Node;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFSurfaceWindImpl;
import icao.iwxxm21.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm21.AerodromeAirTemperatureForecastType;
import icao.iwxxm21.AerodromeCloudForecastPropertyType;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AerodromeForecastChangeIndicatorType;
import icao.iwxxm21.AerodromeForecastWeatherType;
import icao.iwxxm21.AerodromeSurfaceWindForecastPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindForecastType;
import icao.iwxxm21.CloudAmountReportedAtAerodromeType;
import icao.iwxxm21.CloudLayerPropertyType;
import icao.iwxxm21.CloudLayerType;
import icao.iwxxm21.DistanceWithNilReasonType;
import icao.iwxxm21.LengthWithNilReasonType;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.SigConvectiveCloudTypeType;
import icao.iwxxm21.TAFReportStatusType;
import icao.iwxxm21.TAFType;
import wmo.metce2013.ProcessType;

/**
 * Carries out detailed validation and property value collecting for IWXXM TAF messages.
 */
public class IWXXMTAFScanner extends AbstractIWXXMScanner {

    public static List<ConversionIssue> collectTAFProperties(final TAFType input, final ReferredObjectRetrievalContext refCtx, final TAFProperties properties,
            final ConversionHints hints) {
        IssueList retval = new IssueList();

        TAFReportStatusType status = input.getStatus();
        if (status != null) {
            properties.set(TAFProperties.Name.STATUS, AviationCodeListUser.TAFStatus.valueOf(status.name()));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is missing"));
        }

        ZonedDateTime issueDateTime = null;
        //Issue time is time instant
        if (input.getIssueTime() != null) {
            Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
            if (!issueTime.isPresent()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(TAFProperties.Name.ISSUE_TIME, issueTime.get());
                issueDateTime = issueTime.get().getCompleteTime().orElse(null);
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
        }

        PartialOrCompleteTimePeriod tafValidityTime = null;
        //Valid time is time period
        if (input.getValidTime() != null) {
            Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(input.getValidTime(), refCtx);
            if (!validTime.isPresent()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "TAF valid time is not valid"));
            } else {
                tafValidityTime = validTime.get();
                properties.set(TAFProperties.Name.VALID_TIME, tafValidityTime);
                if (!validTime.get().getStartTime().isPresent()) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "TAF valid time start is missing"));
                }
                if (!validTime.get().getEndTime().isPresent()) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "TAF valid time end is missing"));
                }
            }
        } else if (TAFReportStatusType.MISSING != status) {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "TAF valid time is missing"));
        }

        Optional<OMObservationType> baseFct = resolveProperty(input.getBaseForecast(), OMObservationType.class, refCtx);
        if (baseFct.isPresent()) {
            OMObservationProperties baseFctProps = new OMObservationProperties(baseFct.get());
            retval.addAll(collectBaseForecast(baseFct.get(), issueDateTime, tafValidityTime, refCtx, baseFctProps, hints));
            properties.set(TAFProperties.Name.BASE_FORECAST, baseFctProps);
        }

        if (!input.getChangeForecast().isEmpty()) {
            for (OMObservationPropertyType obsProp : input.getChangeForecast()) {
                Optional<OMObservationType> changeFct = resolveProperty(obsProp, OMObservationType.class, refCtx);
                if (changeFct.isPresent()) {
                    OMObservationProperties chFctProps = new OMObservationProperties(changeFct.get());
                    retval.addAll(collectChangeForecast(changeFct.get(), issueDateTime, tafValidityTime, refCtx, chFctProps, hints));
                    properties.addToList(TAFProperties.Name.CHANGE_FORECAST, chFctProps);
                }
            }
        }

        if (TAFReportStatusType.CORRECTION == status || TAFReportStatusType.CANCELLATION == status || TAFReportStatusType.AMENDMENT == status) {
            if (input.getPreviousReportValidPeriod() != null && input.getPreviousReportAerodrome() != null) {
                Optional<AirportHeliportType> airport = resolveProperty(input.getPreviousReportAerodrome(), AirportHeliportType.class, refCtx);
                airport.ifPresent((p) -> {
                    Optional<Aerodrome> drome = buildAerodrome(p, retval, refCtx);
                    drome.ifPresent((d) -> {
                        properties.set(TAFProperties.Name.PREV_REPORT_AERODROME, d);
                    });
                });

                Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(input.getPreviousReportValidPeriod(), refCtx);
                if (!validTime.isPresent()) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "TAF previous report valid time is not valid"));
                } else {
                    properties.set(TAFProperties.Name.PREV_REPORT_VALID_TIME, validTime.get());
                    if (!validTime.get().getStartTime().isPresent()) {
                        retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "TAF previous report valid time start is missing"));
                    }
                    if (!validTime.get().getEndTime().isPresent()) {
                        retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "TAF previous report valid time end is missing"));
                    }
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Previous report info missing for TAF with" + " status " + status));
            }
        } else {
            if (input.getPreviousReportAerodrome() != null || input.getPreviousReportValidPeriod() != null) {
                retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                        "Previous report info was found for TAF with " + "status " + status + ", ignoring"));
            }
        }

        // report metadata
        GenericReportProperties meta = new GenericReportProperties(input);
        retval.addAll(collectReportMetadata(input, meta, hints));
        if (!meta.contains(GenericReportProperties.Name.TRANSLATION_TIME)) {
            if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)) {
                Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
                if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                    meta.set(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.now());
                } else if (value instanceof ZonedDateTime) {
                    meta.set(GenericReportProperties.Name.TRANSLATION_TIME, (ZonedDateTime) value);
                }
            }
        }
        properties.set(TAFProperties.Name.REPORT_METADATA, meta);
        return retval;
    }

    private static List<ConversionIssue> collectBaseForecast(final OMObservationType baseFct, final ZonedDateTime issueTime, final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {

        IssueList retval = collectBaseFctObsMetadata(baseFct, issueTime, tafValidityTime, refCtx, properties, hints);
        Optional<MeteorologicalAerodromeForecastRecordType> baseRecord = getAerodromeForecastRecordResult(baseFct, refCtx);
        if (baseRecord.isPresent()) {
            ForecastRecordProperties baseProps = new ForecastRecordProperties(baseRecord.get());

            retval.addAll(collectCommonForecastRecordProperties(baseRecord.get(), refCtx, baseProps, "base forecast", hints));

            //check no changeIndicator
            if (baseRecord.get().getChangeIndicator() != null) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Base forecast record may not contain a change indicator"));
            }

            //air temperatures
            for (AerodromeAirTemperatureForecastPropertyType prop : baseRecord.get().getTemperature()) {
                withTAFAirTempForecastBuilderFor(prop, refCtx, (builder) -> {
                    baseProps.addToList(ForecastRecordProperties.Name.TEMPERATURE, builder.build());
                }, retval::addAll);
            }

            //prevailing visibility & operator is mandatory
            Optional<NumericMeasure> visibility = asNumericMeasure(baseRecord.get().getPrevailingVisibility());
            if (visibility.isPresent()) {
                baseProps.set(ForecastRecordProperties.Name.PREVAILING_VISIBILITY, visibility.get());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing visibility in base forecast"));
            }

            Optional<AviationCodeListUser.RelationalOperator> visibilityOperator = asRelationalOperator(baseRecord.get().getPrevailingVisibilityOperator());
            if (visibilityOperator.isPresent()) {
                baseProps.set(ForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, visibilityOperator.get());
            }

            //surface wind is mandatory
            AerodromeSurfaceWindForecastPropertyType windProp = baseRecord.get().getSurfaceWind();
            if (windProp != null) {
                withTAFSurfaceWindBuilderFor(windProp, refCtx, (builder) -> {
                    baseProps.set(ForecastRecordProperties.Name.SURFACE_WIND, builder.build());
                }, retval::add);
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind missing in base forecast"));
            }
            properties.set(OMObservationProperties.Name.RESULT, baseProps);
        }

        return retval;
    }

    private static List<ConversionIssue> collectChangeForecast(final OMObservationType changeFct, final ZonedDateTime issueTime, final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {

        IssueList retval = collectChangeFctObsMetadata(changeFct, issueTime, tafValidityTime, refCtx, properties, "change forecast " + changeFct.getId(), hints);
        Optional<MeteorologicalAerodromeForecastRecordType> changeRecord = getAerodromeForecastRecordResult(changeFct, refCtx);
        if (changeRecord.isPresent()) {
            ForecastRecordProperties changeProps = new ForecastRecordProperties(changeRecord.get());
            retval.addAll(collectCommonForecastRecordProperties(changeRecord.get(), refCtx, changeProps, "change forecast " + changeFct.getId(), hints));

            //changeIndicator
            Optional<AerodromeForecastChangeIndicatorType> changeIndicator = Optional.ofNullable(changeRecord.get().getChangeIndicator());
            if (changeIndicator.isPresent()) {
                changeProps.set(ForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.valueOf(changeIndicator.get().name()));
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Change forecast record must contain a change indicator in " + changeFct.getId()));
            }

            //check no air temperatures
            if (!changeRecord.get().getTemperature().isEmpty()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Temperature forecast cannot be included in TAF change forecast in " + changeFct.getId()));
            }

            //prevailing visibility & operator if given
            Optional<NumericMeasure> visibility = asNumericMeasure(changeRecord.get().getPrevailingVisibility());
            if (visibility.isPresent()) {
                changeProps.set(ForecastRecordProperties.Name.PREVAILING_VISIBILITY, visibility.get());
                if (changeRecord.get().getPrevailingVisibilityOperator() != null) {
                    changeProps.set(ForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.valueOf(changeRecord.get().getPrevailingVisibilityOperator().name()));
                }
            }

            //surface wind if given
            AerodromeSurfaceWindForecastPropertyType windProp = changeRecord.get().getSurfaceWind();
            if (windProp != null) {
                withTAFSurfaceWindBuilderFor(windProp, refCtx, (builder) -> {
                    changeProps.set(ForecastRecordProperties.Name.SURFACE_WIND, builder.build());
                }, retval::add);
            }
            properties.set(OMObservationProperties.Name.RESULT, changeProps);
        }
        return retval;
    }

    private static IssueList collectBaseFctObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime, final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx,
            final OMObservationProperties properties, final ConversionHints hints) {
        IssueList retval = collectCommonObsMetadata(fct, issueTime, tafValidityTime, refCtx, properties, "base forecast", hints);

        //phenomenonTime
        if (fct.getPhenomenonTime() != null) {
            Optional<PartialOrCompleteTimePeriod> phenomenonTime = getCompleteTimePeriod(fct.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                if (!phenomenonTime.get().equals(tafValidityTime)) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Phenomenon time of the base forecast is not equal to the valid time " + "of the entire TAF"));
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve base forecast phenomenon time"));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No phenomenon time in base forecast"));
        }
        return retval;
    }

    private static IssueList collectChangeFctObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime, final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final String contextPath, final ConversionHints hints) {
        IssueList retval = collectCommonObsMetadata(fct, issueTime, tafValidityTime, refCtx, properties, contextPath, hints);

        //phenomenonTime
        if (fct.getPhenomenonTime() != null) {
            Optional<PartialOrCompleteTimePeriod> phenomenonTime = getCompleteTimePeriod(fct.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                if (tafValidityTime.getStartTime().isPresent() && tafValidityTime.getStartTime().get().getCompleteTime().isPresent()) {
                    if (phenomenonTime.get().getStartTime().isPresent() && phenomenonTime.get().getStartTime().get().getCompleteTime().isPresent()) {
                        if (phenomenonTime.get().getStartTime().get().getCompleteTime().get().isBefore(tafValidityTime.getStartTime().get().getCompleteTime().get())) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                    "Change forecast start time " + phenomenonTime.get().getStartTime().get().getCompleteTime().get() + " is before the TAF validity time start "
                                            + tafValidityTime.getStartTime().get().getCompleteTime().get());
                        }
                    }
                }
                if (tafValidityTime.getEndTime().isPresent() && tafValidityTime.getEndTime().get().getCompleteTime().isPresent()) {
                    if (phenomenonTime.get().getEndTime().isPresent() && phenomenonTime.get().getEndTime().get().getCompleteTime().isPresent()) {
                        if (phenomenonTime.get().getEndTime().get().getCompleteTime().get().isAfter(tafValidityTime.getEndTime().get().getCompleteTime().get())) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                    "Change forecast end time " + phenomenonTime.get().getEndTime().get().getCompleteTime().get() + " is after the TAF validity time end "
                                            + tafValidityTime.getEndTime().get().getCompleteTime().get());
                        }
                    }
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Phenomenon time of the change forecast cannot be resolved in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No phenomenon time in change forecast in " + contextPath));
        }
        return retval;
    }

    private static IssueList collectCommonObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime, final PartialOrCompleteTimePeriod tafValidityTime,
            final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties, final String contextPath, final ConversionHints hints) {
        IssueList retval = new IssueList();
        //forecast type is correct
        if (fct.getType() != null) {
            ReferenceType type = fct.getType();
            if (AviationCodeListUser.MET_AERODROME_FORECAST_TYPE.equals(type.getHref())) {
                properties.set(OMObservationProperties.Name.TYPE, type.getHref());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                        "Observation type for IWXXM 2.1 aerodrome forecast Observation is invalid " + "in " + contextPath + ", expected '" + AviationCodeListUser.MET_AERODROME_FORECAST_TYPE + "'"));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Observation type for IWXXM 2.1 aerodrome forecast Observation type is missing in " + contextPath));
        }

        //result time
        if (fct.getResultTime() != null) {
            Optional<PartialOrCompleteTimeInstant> resultTime = getCompleteTimeInstant(fct.getResultTime(), refCtx);
            if (resultTime.isPresent() && resultTime.get().getCompleteTime().isPresent()) {
                properties.set(OMObservationProperties.Name.RESULT_TIME, resultTime.get());
                if (issueTime != null && !resultTime.get().getCompleteTime().get().equals(issueTime)) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Result time in forecast is not equal to the TAF message issue time " + "in " + contextPath));
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Result time is not valid or cannot be resolved in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Result time is missing in " + contextPath));
        }

        //validTime
        if (fct.getValidTime() != null) {
            Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(fct.getValidTime(), refCtx);
            if (validTime.isPresent()) {
                properties.set(OMObservationProperties.Name.VALID_TIME, validTime.get());
                if (!validTime.get().equals(tafValidityTime)) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                            "Valid time of the base forecast is not equal to the valid time of " + "the entire TAF in " + contextPath));
                }
            }
        }

        //procedure
        if (fct.getProcedure() != null) {
            Optional<ProcessType> process = resolveProperty(fct.getProcedure(), ProcessType.class, refCtx);

            if (process.isPresent()) {
                //Note: Checking against the exact String match of the description is probably too strict:
                /*
                if (!AviationCodeListUser.TAF_PROCEDURE_DESCRIPTION.equals(process.get().getDescription().getValue())) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Process description is not "));
                }
                */
                properties.set(OMObservationProperties.Name.PROCEDURE, process.get());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve the process property value in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No process given in " + contextPath));
        }

        //observed property
        if (fct.getObservedProperty() != null) {
            ReferenceType obsProp = fct.getObservedProperty();
            if (AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES.equals(obsProp.getHref())) {
                properties.set(OMObservationProperties.Name.OBSERVED_PROPERTY, obsProp.getHref());
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                        "Observed property does not match " + AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES + " in " + contextPath));
            }
        }

        //foi
        if (fct.getFeatureOfInterest() != null) {
            Optional<SFSpatialSamplingFeatureType> sft = resolveProperty(fct.getFeatureOfInterest(), "abstractFeature", SFSpatialSamplingFeatureType.class, refCtx);
            if (sft.isPresent()) {

                List<FeaturePropertyType> sampledFeatures = sft.get().getSampledFeature();
                if (sampledFeatures.isEmpty()) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "No sampled feature (for aerodrome) in TAF"));
                } else if (sampledFeatures.size() != 1) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one sampled feature in TAF"));
                } else {
                    Optional<AirportHeliportType> airport = resolveProperty(sampledFeatures.get(0), "abstractFeature", AirportHeliportType.class, refCtx);
                    airport.ifPresent((p) -> {
                        Optional<Aerodrome> drome = buildAerodrome(p, retval, refCtx);
                        drome.ifPresent((d) -> {
                            properties.set(OMObservationProperties.Name.AERODROME, d);
                        });
                    });
                }

                ShapeType shape = sft.get().getShape();
                if (shape != null) {
                    Optional<PointType> point = resolveProperty(shape, "abstractGeometry", PointType.class, refCtx);
                    if (!point.isPresent()) {
                        retval.add(
                                new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The spatial sampling feature shape is not a Point in " + contextPath));
                    } else {
                        GeoPositionImpl.Builder posBuilder = new GeoPositionImpl.Builder();
                        boolean canBuildPos = true;
                        if (point.get().getPos() != null) {
                            DirectPositionType dp = point.get().getPos();
                            if (dp.getSrsName() != null) {
                                posBuilder.setCoordinateReferenceSystemId(dp.getSrsName());
                            } else {
                                if (point.get().getSrsName() != null) {
                                    posBuilder.setCoordinateReferenceSystemId(point.get().getSrsName());
                                } else {
                                    canBuildPos = false;
                                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                            "No SRS name for sampling point position"));
                                }
                            }
                            if (dp.getValue() != null) {
                                posBuilder.setCoordinates(dp.getValue().toArray(new Double[] {}));
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No value for sampling point position"));
                            }

                            if (canBuildPos) {
                                properties.set(OMObservationProperties.Name.SAMPLING_POINT, posBuilder.build());
                            }

                        } else if (point.get().getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated " + "position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                    }
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No shape in spatial sampling feature in " + contextPath));
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Could not find feature of interest of type SFSpatialSamplingFeature in " + contextPath));
            }
        }
        return retval;
    }

    private static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval, final ReferredObjectRetrievalContext refCtx) {
        AerodromeImpl.Builder aerodromeBuilder = new AerodromeImpl.Builder();
        List<AirportHeliportTimeSlicePropertyType> slices = airport.getTimeSlice();
        if (slices.isEmpty()) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No time slice (for aerodrome) in TAF"));
            return Optional.empty();
        } else if (slices.size() != 1) {
            retval.add(
                    new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one time slice (for aerodrome) in TAF"));
            return Optional.empty();
        } else {
            Optional<AirportHeliportTimeSliceType> slice = resolveProperty(slices.get(0), AirportHeliportTimeSliceType.class, refCtx);
            if (slice.isPresent()) {
                if (slice.get().getDesignator().getNilReason() == null) {
                    aerodromeBuilder.setDesignator(slice.get().getDesignator().getValue());
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No designator for " + "aerodrome"));
                }
                if (slice.get().getLocationIndicatorICAO() != null) {
                    aerodromeBuilder.setLocationIndicatorICAO(slice.get().getLocationIndicatorICAO().getValue());
                }
                if (slice.get().getDesignatorIATA() != null) {
                    aerodromeBuilder.setDesignatorIATA(slice.get().getDesignatorIATA().getValue());
                }
                if (slice.get().getPortName() != null) {
                    aerodromeBuilder.setName(slice.get().getPortName().getValue());
                }

                ValDistanceVerticalType elevation = slice.get().getFieldElevation();
                ElevatedPointPropertyType pointProp = slice.get().getARP();
                if (pointProp != null && pointProp.getNilReason() == null) {
                    Optional<ElevatedPointType> elPoint = resolveProperty(pointProp, ElevatedPointType.class, refCtx);
                    if (elPoint.isPresent()) {
                        GeoPositionImpl.Builder posBuilder = new GeoPositionImpl.Builder();
                        boolean canBuildPos = true;
                        //use ref point elevation as fallback for the aerodrome elevation
                        if (elevation == null && elPoint.get().getElevation() != null) {
                            elevation = elPoint.get().getElevation();
                            if (elevation.getNilReason() == null) {
                                posBuilder.setNullableElevationUom(elevation.getUom());
                                if (elevation.getValue() != null) {
                                    posBuilder.setElevationValue(Double.parseDouble(elevation.getValue()));
                                }
                            }
                        }
                        if (elPoint.get().getPos() != null) {
                            DirectPositionType dp = elPoint.get().getPos();
                            if (dp.getSrsName() != null) {
                                posBuilder.setCoordinateReferenceSystemId(dp.getSrsName());
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No SRS " + "name for ARP elevated point position"));
                            }
                            if (dp.getValue() != null) {
                                posBuilder.setCoordinates(dp.getValue().toArray(new Double[] {}));
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No value " + "for ARP elevated point position"));
                            }

                            if (canBuildPos) {
                                aerodromeBuilder.setReferencePoint(posBuilder.build());
                            }

                        } else if (elPoint.get().getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated " + "position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                    }
                }

                if (elevation != null && elevation.getNilReason() == null) {
                    if ("M".equals(elevation.getUom())) {
                        aerodromeBuilder.setFieldElevationValue(Double.parseDouble(elevation.getValue()));
                    } else {
                        retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                "Field elevation unit " + "is '" + elevation.getUom() + "', 'M' is required"));
                    }
                }
            }
        }
        return Optional.of(aerodromeBuilder.build());
    }

    private static IssueList collectCommonForecastRecordProperties(final MeteorologicalAerodromeForecastRecordType record, final ReferredObjectRetrievalContext refCtx, final ForecastRecordProperties properties, final String contextPath, final ConversionHints hints) {
        IssueList retval = new IssueList();

        //cavok
        if (record.isCloudAndVisibilityOK()) {
            properties.set(ForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.TRUE);
        }

        //weather
        for (AerodromeForecastWeatherType weather : record.getWeather()) {
            boolean nswFound = false;
            for (String nilReason : weather.getNilReason()) {
                if (AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE.equals(nilReason)) {
                    nswFound = true;
                    break;
                }
            }
            if (nswFound) {
                properties.set(ForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.TRUE);
            } else {
                withWeatherBuilderFor(weather, hints, (builder) -> {
                    properties.addToList(ForecastRecordProperties.Name.WEATHER, builder.build());
                }, retval::add);
            }
        }

        //cloud
        AerodromeCloudForecastPropertyType cloudProp = record.getCloud();
        if (cloudProp != null) {
            CloudForecastImpl.Builder cloudBuilder = new CloudForecastImpl.Builder();
            //NSC
            boolean nscFound = false;
            for (String nilReason : cloudProp.getNilReason()) {
                if (AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE.equals(nilReason)) {
                    nscFound = true;
                    break;
                }
            }

            cloudBuilder.setNoSignificantCloud(nscFound);
            if (!nscFound) {
                Optional<AerodromeCloudForecastType> cloud = resolveProperty(cloudProp, AerodromeCloudForecastType.class, refCtx);
                if (cloud.isPresent()) {
                    if (record.isCloudAndVisibilityOK()) {
                        retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Forecast features CAVOK but cloud forecast exists in " + contextPath));
                    }
                    JAXBElement<LengthWithNilReasonType> vv = cloud.get().getVerticalVisibility();
                    if (vv != null && vv.getValue() != null) {
                        cloudBuilder.setVerticalVisibility(asNumericMeasure(vv.getValue()));
                    } else {
                        List<CloudLayer> layers = new ArrayList<>();
                        for (CloudLayerPropertyType layerProp : cloud.get().getLayer()) {
                            Optional<CloudLayerType> layer = resolveProperty(layerProp, CloudLayerType.class, refCtx);
                            if (layer.isPresent()) {
                                CloudAmountReportedAtAerodromeType amount = layer.get().getAmount();
                                DistanceWithNilReasonType base = layer.get().getBase();
                                JAXBElement<SigConvectiveCloudTypeType> type = layer.get().getCloudType();
                                CloudLayerImpl.Builder layerBuilder = new CloudLayerImpl.Builder();

                                if (base.getNilReason().isEmpty()) {
                                    layerBuilder.setBase(asNumericMeasure(base));
                                }
                                if (amount.getHref() != null && amount.getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME)) {
                                    String amountCode = amount.getHref().substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME.length());
                                    try {
                                        layerBuilder.setAmount(AviationCodeListUser.CloudAmount.fromInt(Integer.parseInt(amountCode)));
                                    } catch (NumberFormatException e) {
                                        retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                                "Could not parse code list value '" + amountCode + "' as an integer for code list " + AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME));
                                    }

                                } else {
                                    retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                            "Cloud amount code '" + amount.getHref() + "' does not start with " + AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + " in " + contextPath));
                                }

                                if (type != null && type.getValue() != null) {
                                    if (type.getValue().getHref() != null && type.getValue().getHref().startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE)) {
                                        String typeCode = type.getValue().getHref().substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE.length());
                                        try {
                                            layerBuilder.setCloudType(AviationCodeListUser.CloudType.fromInt(Integer.parseInt(typeCode)));
                                        } catch (NumberFormatException e) {
                                            retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                                    "Could not parse code list value '" + typeCode + "' as an integer for code list " + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE));
                                        }
                                    }
                                }
                                layers.add(layerBuilder.build());
                            } else {
                                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                        "Could not resolve cloud layer " + cloudProp.getHref() + "" + " in " + contextPath));
                            }
                        }
                        if (!layers.isEmpty()) {
                            cloudBuilder.setLayers(layers);
                        } else {
                            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                    "No NSC, vertical visibility or cloud layers in cloud forecast in " + contextPath));
                        }
                    }
                }
            }
            properties.set(ForecastRecordProperties.Name.CLOUD, cloudBuilder.build());
        }

        return retval;
    }

    //Helpers

    private static Optional<MeteorologicalAerodromeForecastRecordType> getAerodromeForecastRecordResult(final OMObservationType fct, final ReferredObjectRetrievalContext refCtx) {
        Object o = fct.getResult();
        if (o instanceof Node) {
            Node resultNode = ((Node) o);
            Node recordNode = resultNode.getFirstChild();
            if (recordNode != null && "MeteorologicalAerodromeForecastRecord".equals(recordNode.getLocalName()) && "http://icao.int/iwxxm/2.1".equals(recordNode.getNamespaceURI())) {
                try {
                    JAXBElement<MeteorologicalAerodromeForecastRecordType> record = refCtx.getJAXBBinder().unmarshal(recordNode, MeteorologicalAerodromeForecastRecordType.class);
                    return Optional.of(record.getValue());
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }

    private static void withTAFSurfaceWindBuilderFor(final AerodromeSurfaceWindForecastPropertyType windProp, final ReferredObjectRetrievalContext refCtx, final Consumer<TAFSurfaceWindImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        Optional<AerodromeSurfaceWindForecastType> windFct = resolveProperty(windProp, AerodromeSurfaceWindForecastType.class, refCtx);
        if (windFct.isPresent()) {
            TAFSurfaceWindImpl.Builder windBuilder = new TAFSurfaceWindImpl.Builder();
            windBuilder.setMeanWindDirection(asNumericMeasure(windFct.get().getMeanWindDirection()));
            if (windFct.get().isVariableWindDirection()) {
                windBuilder.setVariableDirection(true);
            } else {
                windBuilder.setVariableDirection(false);
            }
            if (windFct.get().getMeanWindSpeed() != null) {
                windBuilder.setMeanWindSpeed(asNumericMeasure(windFct.get().getMeanWindSpeed()).get());
            } else {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean wind speed missing from TAF surface wind forecast");
            }
            Optional<AviationCodeListUser.RelationalOperator> speedOperator = asRelationalOperator(windFct.get().getMeanWindSpeedOperator());
            speedOperator.ifPresent(windBuilder::setMeanWindSpeedOperator);

            Optional<NumericMeasure> gustSpeed = asNumericMeasure(windFct.get().getWindGustSpeed());
            gustSpeed.ifPresent(windBuilder::setWindGust);

            Optional<AviationCodeListUser.RelationalOperator> gustOperator = asRelationalOperator(windFct.get().getWindGustSpeedOperator());
            gustOperator.ifPresent(windBuilder::setWindGustOperator);

            resultHandler.accept(windBuilder);
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not find AerodromeSurfaceWindForecastType value within " + "AerodromeSurfaceWindForecastTypePropertyType or by reference");
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withTAFAirTempForecastBuilderFor(final AerodromeAirTemperatureForecastPropertyType prop, final ReferredObjectRetrievalContext refCtx,
            final Consumer<TAFAirTemperatureForecastImpl.Builder> resultHandler, final Consumer<List<ConversionIssue>> issueHandler) {
        List<ConversionIssue> issues = new ArrayList<>();
        Optional<AerodromeAirTemperatureForecastType> tempFct = resolveProperty(prop, AerodromeAirTemperatureForecastType.class, refCtx);
        if (tempFct.isPresent()) {
            TAFAirTemperatureForecastImpl.Builder builder = new TAFAirTemperatureForecastImpl.Builder();
            if (tempFct.get().getMaximumAirTemperature() != null) {
                builder.setMaxTemperature(asNumericMeasure(tempFct.get().getMaximumAirTemperature()).get());
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Maximum temperature value missing from air temperature forecast"));
            }
            if (tempFct.get().getMaximumAirTemperatureTime() != null) {
                Optional<PartialOrCompleteTimeInstant> maxTempTime = getCompleteTimeInstant(tempFct.get().getMaximumAirTemperatureTime(), refCtx);
                if (maxTempTime.isPresent()) {
                    builder.setMaxTemperatureTime(maxTempTime.get());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Invalid maximum temperature time in air temperature " + "forecast"));
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Maximum temperature time missing from air temperature " + "forecast"));
            }
            if (tempFct.get().getMinimumAirTemperature() != null) {
                builder.setMinTemperature(asNumericMeasure(tempFct.get().getMinimumAirTemperature()).get());
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Minimum temperature value missing from air temperature forecast"));
            }
            if (tempFct.get().getMinimumAirTemperatureTime() != null) {
                Optional<PartialOrCompleteTimeInstant> minTempTime = getCompleteTimeInstant(tempFct.get().getMinimumAirTemperatureTime(), refCtx);
                if (minTempTime.isPresent()) {
                    builder.setMinTemperatureTime(minTempTime.get());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Invalid minimum temperature time in air temperature " + "forecast"));
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Minimum temperature time missing from air temperature " + "forecast"));
            }
            resultHandler.accept(builder);
        }
        if (!issues.isEmpty()) {
            issueHandler.accept(issues);
        }
    }

    private static void withWeatherBuilderFor(final AerodromeForecastWeatherType weather, final ConversionHints hints, final Consumer<WeatherImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        String codeListValue = weather.getHref();
        if (codeListValue != null && codeListValue.startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER)) {
            String code = codeListValue.substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER.length());
            String description = weather.getTitle();
            WeatherImpl.Builder wBuilder = new WeatherImpl.Builder();
            boolean codeOk = false;
            if (hints == null || hints.isEmpty() || !hints.containsKey(ConversionHints.KEY_WEATHER_CODES) || ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678
                    .equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                // Only the official codes allowed by default
                if (WEATHER_CODES.containsKey(code)) {
                    wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                    codeOk = true;
                } else {
                    issue = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Illegal weather code " + code + " found with strict WMO 4678 " + "checking");
                }
            } else {
                if (ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    wBuilder.setCode(code);
                    if (description != null) {
                        wBuilder.setDescription(description);
                    } else if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setDescription(WEATHER_CODES.get(code));
                    }
                } else if (ConversionHints.VALUE_WEATHER_CODES_IGNORE_NON_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                        codeOk = true;
                    }
                }
            }
            if (codeOk) {
                resultHandler.accept(wBuilder);
            }
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Weather codelist value does not begin with " + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER);
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static Optional<NumericMeasure> asNumericMeasure(final MeasureType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(new NumericMeasureImpl.Builder().setValue(source.getValue()).setUom(source.getUom()).build());
    }

    private static Optional<AviationCodeListUser.RelationalOperator> asRelationalOperator(final RelationalOperatorType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(AviationCodeListUser.RelationalOperator.valueOf(source.name()));
    }

}
