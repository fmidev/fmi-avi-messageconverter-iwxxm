package fi.fmi.avi.converter.iwxxm.v2_1.taf;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aero.aixm511.AirportHeliportType;
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
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import icao.iwxxm21.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm21.AerodromeAirTemperatureForecastType;
import icao.iwxxm21.AerodromeCloudForecastPropertyType;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AerodromeForecastChangeIndicatorType;
import icao.iwxxm21.AerodromeForecastWeatherType;
import icao.iwxxm21.AerodromeSurfaceWindForecastPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindForecastType;
import icao.iwxxm21.CloudLayerPropertyType;
import icao.iwxxm21.LengthWithNilReasonType;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;
import icao.iwxxm21.TAFReportStatusType;
import icao.iwxxm21.TAFType;

/**
 * Carries out detailed validation and property value collecting for IWXXM TAF messages.
 */
public class TAFIWXXMScanner extends AbstractIWXXM21Scanner {

    private static final Logger LOG = LoggerFactory.getLogger(TAFIWXXMScanner.class);

    public static List<ConversionIssue> collectTAFProperties(final TAFType input, final ReferredObjectRetrievalContext refCtx, final TAFProperties properties,
            final ConversionHints hints) {
        final IssueList retval = new IssueList();

        final TAFReportStatusType status = input.getStatus();
        if (status != null) {
            properties.set(TAFProperties.Name.STATUS, AviationCodeListUser.TAFStatus.valueOf(status.name()));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Status is missing"));
        }

        ZonedDateTime issueDateTime = null;
        //Issue time is time instant
        if (input.getIssueTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
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
            final Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(input.getValidTime(), refCtx);
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

        final Optional<OMObservationType> baseFct = resolveProperty(input.getBaseForecast(), OMObservationType.class, refCtx);
        if (baseFct.isPresent()) {
            final OMObservationProperties baseFctProps = new OMObservationProperties();
            retval.addAll(collectBaseForecast(baseFct.get(), issueDateTime, tafValidityTime, refCtx, baseFctProps, hints));
            properties.set(TAFProperties.Name.BASE_FORECAST, baseFctProps);
        }

        if (!input.getChangeForecast().isEmpty()) {
            for (final OMObservationPropertyType obsProp : input.getChangeForecast()) {
                final Optional<OMObservationType> changeFct = resolveProperty(obsProp, OMObservationType.class, refCtx);
                if (changeFct.isPresent()) {
                    final OMObservationProperties chFctProps = new OMObservationProperties();
                    retval.addAll(collectChangeForecast(changeFct.get(), issueDateTime, tafValidityTime, refCtx, chFctProps, hints));
                    properties.addToList(TAFProperties.Name.CHANGE_FORECAST, chFctProps);
                }
            }
        }

        if (TAFReportStatusType.CORRECTION == status || TAFReportStatusType.CANCELLATION == status || TAFReportStatusType.AMENDMENT == status) {
            if (input.getPreviousReportValidPeriod() != null && input.getPreviousReportAerodrome() != null) {
                final Optional<AirportHeliportType> airport = resolveProperty(input.getPreviousReportAerodrome(), AirportHeliportType.class, refCtx);
                airport.ifPresent(p -> {
                    final Optional<Aerodrome> drome = buildAerodrome(p, retval, refCtx);
                    drome.ifPresent(d -> properties.set(TAFProperties.Name.PREV_REPORT_AERODROME, d));
                });

                final Optional<PartialOrCompleteTimePeriod> validTime = getCompleteTimePeriod(input.getPreviousReportValidPeriod(), refCtx);
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
        final GenericReportProperties meta = new GenericReportProperties();
        retval.addAll(AbstractIWXXM21Scanner.collectReportMetadata(input, meta, hints));
        properties.set(TAFProperties.Name.REPORT_METADATA, meta);
        return retval;
    }

    private static List<ConversionIssue> collectBaseForecast(final OMObservationType baseFct, final ZonedDateTime issueTime,
            final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final ConversionHints hints) {

        final IssueList retval = collectBaseFctObsMetadata(baseFct, issueTime, tafValidityTime, refCtx, properties, hints);
        final Optional<MeteorologicalAerodromeForecastRecordType> baseRecord = AbstractIWXXM21Scanner.getAerodromeForecastRecordResult(baseFct, refCtx);
        if (baseRecord.isPresent()) {
            final TAFForecastRecordProperties baseProps = new TAFForecastRecordProperties();

            retval.addAll(collectCommonForecastRecordProperties(baseRecord.get(), refCtx, baseProps, "base forecast", hints));

            //check no changeIndicator
            if (baseRecord.get().getChangeIndicator() != null) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Base forecast record may not contain a change indicator"));
            }

            //air temperatures
            for (final AerodromeAirTemperatureForecastPropertyType prop : baseRecord.get().getTemperature()) {
                withTAFAirTempForecastBuilderFor(prop, refCtx, builder -> baseProps.addToList(TAFForecastRecordProperties.Name.TEMPERATURE, builder.build()),
                        retval::addAll);
            }

            //prevailing visibility & operator is mandatory
            final Optional<NumericMeasure> visibility = asNumericMeasure(baseRecord.get().getPrevailingVisibility());
            if (visibility.isPresent()) {
                baseProps.set(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, visibility.get());
            } else if (!visibility.isPresent() && !baseRecord.get().isCloudAndVisibilityOK()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing visibility in base forecast"));
            }

            final Optional<AviationCodeListUser.RelationalOperator> visibilityOperator = asRelationalOperator(
                    baseRecord.get().getPrevailingVisibilityOperator());
            visibilityOperator.ifPresent(
                    relationalOperator -> baseProps.set(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, relationalOperator));

            //surface wind is mandatory
            final AerodromeSurfaceWindForecastPropertyType windProp = baseRecord.get().getSurfaceWind();
            if (windProp != null) {
                withTAFSurfaceWindBuilderFor(windProp, refCtx, builder -> baseProps.set(TAFForecastRecordProperties.Name.SURFACE_WIND, builder.build()),
                        retval::add);
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind missing in base forecast"));
            }
            properties.set(OMObservationProperties.Name.RESULT, baseProps);
        }

        return retval;
    }

    private static List<ConversionIssue> collectChangeForecast(final OMObservationType changeFct, final ZonedDateTime issueTime,
            final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final ConversionHints hints) {

        final IssueList retval = collectChangeFctObsMetadata(changeFct, issueTime, tafValidityTime, refCtx, properties, "change forecast " + changeFct.getId(),
                hints);
        final Optional<MeteorologicalAerodromeForecastRecordType> changeRecord = AbstractIWXXM21Scanner.getAerodromeForecastRecordResult(changeFct, refCtx);
        if (changeRecord.isPresent()) {
            final TAFForecastRecordProperties changeProps = new TAFForecastRecordProperties();
            retval.addAll(collectCommonForecastRecordProperties(changeRecord.get(), refCtx, changeProps, "change forecast " + changeFct.getId(), hints));

            //changeIndicator
            final Optional<AerodromeForecastChangeIndicatorType> changeIndicator = Optional.ofNullable(changeRecord.get().getChangeIndicator());
            if (changeIndicator.isPresent()) {
                changeProps.set(TAFForecastRecordProperties.Name.CHANGE_INDICATOR,
                        AviationCodeListUser.TAFChangeIndicator.valueOf(changeIndicator.get().name()));
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Change forecast record must contain a change indicator in " + changeFct.getId()));
            }

            //check no air temperatures
            if (!changeRecord.get().getTemperature().isEmpty()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                        "Temperature forecast cannot be included in TAF change forecast in " + changeFct.getId()));
            }

            //prevailing visibility & operator if given
            final Optional<NumericMeasure> visibility = asNumericMeasure(changeRecord.get().getPrevailingVisibility());
            if (visibility.isPresent()) {
                changeProps.set(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, visibility.get());
                if (changeRecord.get().getPrevailingVisibilityOperator() != null) {
                    changeProps.set(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR,
                            AviationCodeListUser.RelationalOperator.valueOf(changeRecord.get().getPrevailingVisibilityOperator().name()));
                }
            }

            //surface wind if given
            final AerodromeSurfaceWindForecastPropertyType windProp = changeRecord.get().getSurfaceWind();
            if (windProp != null) {
                withTAFSurfaceWindBuilderFor(windProp, refCtx, builder -> changeProps.set(TAFForecastRecordProperties.Name.SURFACE_WIND, builder.build()),
                        retval::add);
            }

            //Check for existence of all the mandatory properties in the From case:
            final Optional<AviationCodeListUser.TAFChangeIndicator> changeIndicator2 = changeProps.get(TAFForecastRecordProperties.Name.CHANGE_INDICATOR,
                    AviationCodeListUser.TAFChangeIndicator.class);
            if (changeIndicator2.isPresent() && AviationCodeListUser.TAFChangeIndicator.FROM == changeIndicator2.get()) {
                if (!changeProps.get(TAFForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class).isPresent()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Surface wind is missing in the From type change forecast");
                }

                if (!changeProps.get(TAFForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class).isPresent()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Visibility is missing in the From type change forecast");
                }

                if (!changeProps.get(TAFForecastRecordProperties.Name.CLOUD, CloudForecast.class).isPresent()) {
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cloud is missing in the From type change forecast");
                }
            }
            properties.set(OMObservationProperties.Name.RESULT, changeProps);
        }
        return retval;
    }

    private static IssueList collectBaseFctObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime,
            final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final ConversionHints hints) {
        final IssueList retval = collectAndCheckCommonObsMetadata(fct, issueTime, tafValidityTime, refCtx, properties, "base forecast", hints);

        //phenomenonTime
        if (fct.getPhenomenonTime() != null) {
            final Optional<PartialOrCompleteTimePeriod> phenomenonTime = AbstractIWXXM21Serializer.getCompleteTimePeriod(fct.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                if (!phenomenonTime.get().equals(tafValidityTime)) {
                    retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                            "Phenomenon time of the base forecast is not equal to the valid time " + "of the entire TAF"));
                }
            } else {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not resolve base forecast phenomenon time"));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No phenomenon time in base forecast"));
        }
        return retval;
    }

    private static IssueList collectChangeFctObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime,
            final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final String contextPath, final ConversionHints hints) {
        final IssueList retval = collectAndCheckCommonObsMetadata(fct, issueTime, tafValidityTime, refCtx, properties, contextPath, hints);

        //phenomenonTime
        if (fct.getPhenomenonTime() != null) {
            final Optional<PartialOrCompleteTimePeriod> phenomenonTime = AbstractIWXXM21Serializer.getCompleteTimePeriod(fct.getPhenomenonTime(), refCtx);
            if (phenomenonTime.isPresent()) {
                properties.set(OMObservationProperties.Name.PHENOMENON_TIME, phenomenonTime.get());
                if (tafValidityTime.getStartTime().isPresent() && tafValidityTime.getStartTime().get().getCompleteTime().isPresent()) {
                    if (phenomenonTime.get().getStartTime().isPresent() && phenomenonTime.get().getStartTime().get().getCompleteTime().isPresent()) {
                        if (phenomenonTime.get()
                                .getStartTime()
                                .get()
                                .getCompleteTime()
                                .get()
                                .isBefore(tafValidityTime.getStartTime().get().getCompleteTime().get())) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                    "Change forecast start time " + phenomenonTime.get().getStartTime().get().getCompleteTime().get()
                                            + " is before the TAF validity time start " + tafValidityTime.getStartTime().get().getCompleteTime().get());
                        }
                    }
                }
                if (tafValidityTime.getEndTime().isPresent() && tafValidityTime.getEndTime().get().getCompleteTime().isPresent()) {
                    if (phenomenonTime.get().getEndTime().isPresent() && phenomenonTime.get().getEndTime().get().getCompleteTime().isPresent()) {
                        if (phenomenonTime.get()
                                .getEndTime()
                                .get()
                                .getCompleteTime()
                                .get()
                                .isAfter(tafValidityTime.getEndTime().get().getCompleteTime().get())) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL,
                                    "Change forecast end time " + phenomenonTime.get().getEndTime().get().getCompleteTime().get()
                                            + " is after the TAF validity time end " + tafValidityTime.getEndTime().get().getCompleteTime().get());
                        }
                    }
                }
            } else {
                retval.add(
                        new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Phenomenon time of the change forecast cannot be resolved in " + contextPath));
            }
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No phenomenon time in change forecast in " + contextPath));
        }
        return retval;
    }

    private static IssueList collectAndCheckCommonObsMetadata(final OMObservationType fct, final ZonedDateTime issueTime,
            final PartialOrCompleteTimePeriod tafValidityTime, final ReferredObjectRetrievalContext refCtx, final OMObservationProperties properties,
            final String contextPath, final ConversionHints hints) {
        final IssueList retval = AbstractIWXXM21Scanner.collectCommonObsMetadata(fct, refCtx, properties, contextPath, hints);

        final Optional<String> type = properties.get(OMObservationProperties.Name.TYPE, String.class);
        if (type.isPresent() && !AviationCodeListUser.MET_AERODROME_FORECAST_TYPE.equals(type.get())) {
            retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                    "Observation type for IWXXM 2.1 aerodrome forecast Observation is invalid " + "in " + contextPath + ", expected '"
                            + AviationCodeListUser.MET_AERODROME_FORECAST_TYPE + "'"));
        }

        final Optional<PartialOrCompleteTimeInstant> resultTime = properties.get(OMObservationProperties.Name.RESULT_TIME, PartialOrCompleteTimeInstant.class);
        if (issueTime != null && resultTime.isPresent() && resultTime.get().getCompleteTime().isPresent() && !resultTime.get()
                .getCompleteTime()
                .get()
                .equals(issueTime)) {
            retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                    "Result time in forecast is not equal to the TAF message issue time " + "in " + contextPath));
        }

        final Optional<PartialOrCompleteTimePeriod> validTime = properties.get(OMObservationProperties.Name.VALID_TIME, PartialOrCompleteTimePeriod.class);
        if (validTime.isPresent() && !validTime.get().equals(tafValidityTime)) {
            retval.add(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                    "Valid time of the base forecast is not equal to the valid time of " + "the entire TAF in " + contextPath));
        }
        //Note: Checking against the exact String match of the process description is probably too strict
        return retval;
    }

    private static IssueList collectCommonForecastRecordProperties(final MeteorologicalAerodromeForecastRecordType record,
            final ReferredObjectRetrievalContext refCtx, final TAFForecastRecordProperties properties, final String contextPath, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        //cavok
        if (record.isCloudAndVisibilityOK()) {
            properties.set(TAFForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.TRUE);
        }

        //weather
        withEachNillableChild(record, record.getWeather(), AerodromeForecastWeatherType.class,
                new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "weather"), refCtx, value -> AbstractIWXXM21Scanner.withWeatherBuilderFor(value, hints,
                        builder -> properties.addToList(TAFForecastRecordProperties.Name.WEATHER, builder.build()), retval::add), nilReasons -> {
                    if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)) {
                        properties.set(TAFForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.TRUE);
                        if (record.getWeather().size() != 1) {
                            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL, "'No significant weather' (NSW) was reported with other "
                                    + "weather phenomena, NSW must be the only weather property if given");
                        }
                    }
                });

        //cloud
        withNillableChild(record, record.getCloud(), AerodromeCloudForecastPropertyType.class, new QName(IWXXMNamespaceContext.getDefaultURI("iwxxm"), "cloud"),
                refCtx, value -> {
                    final CloudForecastImpl.Builder cloudBuilder = CloudForecastImpl.builder();
                    final Optional<AerodromeCloudForecastType> cloudForecast = resolveProperty(value, AerodromeCloudForecastType.class, refCtx);
                    if (cloudForecast.isPresent()) {
                        if (record.isCloudAndVisibilityOK()) {
                            retval.add(
                                    new ConversionIssue(ConversionIssue.Type.LOGICAL, "Forecast features CAVOK but cloud forecast exists in " + contextPath));
                        }
                        final JAXBElement<LengthWithNilReasonType> vv = cloudForecast.get().getVerticalVisibility();
                        if (vv != null) {
                            if (vv.getValue() != null) {
                                cloudBuilder.setVerticalVisibility(asNumericMeasure(vv.getValue()));
                            } else if (vv.isNil()) {
                                cloudBuilder.setVerticalVisibilityMissing(true);
                            }
                        } else {
                            final List<CloudLayer> layers = new ArrayList<>();
                            for (final CloudLayerPropertyType layerProp : cloudForecast.get().getLayer()) {
                                AbstractIWXXM21Scanner.withCloudLayerBuilderFor(layerProp, refCtx, layerBuilder -> layers.add(layerBuilder.build()),
                                        retval::add, contextPath + " / cloud forecast");
                            }
                            if (!layers.isEmpty()) {
                                cloudBuilder.setLayers(layers);
                            } else {
                                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                        "No vertical visibility or cloud layers in " + contextPath + " / cloud forecast"));
                            }
                        }
                    }

                    properties.set(TAFForecastRecordProperties.Name.CLOUD, cloudBuilder.build());
                }, nilReasons -> {
                    if (nilReasons.contains(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)) {
                        properties.set(TAFForecastRecordProperties.Name.CLOUD, CloudForecastImpl.builder()//
                                .setNoSignificantCloud(true)//
                                .build());
                    }
                });

        return retval;
    }
    //Helpers

    private static void withTAFSurfaceWindBuilderFor(final AerodromeSurfaceWindForecastPropertyType windProp, final ReferredObjectRetrievalContext refCtx,
            final Consumer<SurfaceWindImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        final AerodromeSurfaceWindForecastType windFct = resolveProperty(windProp, AerodromeSurfaceWindForecastType.class, refCtx).orElse(null);
        if (windFct == null) {
            issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Could not find AerodromeSurfaceWindForecastType value within " + "AerodromeSurfaceWindForecastTypePropertyType or by reference");
        } else {
            final SurfaceWindImpl.Builder windBuilder = SurfaceWindImpl.builder();
            windBuilder.setMeanWindDirection(asNumericMeasure(windFct.getMeanWindDirection()));
            windBuilder.setVariableDirection(windFct.isVariableWindDirection());
            if (windFct.getMeanWindSpeed() != null) {
                asNumericMeasure(windFct.getMeanWindSpeed()).ifPresent(windBuilder::setMeanWindSpeed);
            } else {
                issue = new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean wind speed missing from TAF surface wind forecast");
            }
            windBuilder.setMeanWindSpeedOperator(asRelationalOperator(windFct.getMeanWindSpeedOperator()));
            windBuilder.setWindGust(asNumericMeasure(windFct.getWindGustSpeed()));
            windBuilder.setWindGustOperator(asRelationalOperator(windFct.getWindGustSpeedOperator()));
            resultHandler.accept(windBuilder);
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    private static void withTAFAirTempForecastBuilderFor(final AerodromeAirTemperatureForecastPropertyType prop, final ReferredObjectRetrievalContext refCtx,
            final Consumer<TAFAirTemperatureForecastImpl.Builder> resultHandler, final Consumer<List<ConversionIssue>> issueHandler) {
        final AerodromeAirTemperatureForecastType tempFct = resolveProperty(prop, AerodromeAirTemperatureForecastType.class, refCtx).orElse(null);
        if (tempFct == null) {
            return;
        }
        final List<ConversionIssue> issues = new ArrayList<>();
        final TAFAirTemperatureForecastImpl.Builder builder = TAFAirTemperatureForecastImpl.builder();
        if (tempFct.getMaximumAirTemperature() == null) {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Maximum temperature value missing from air temperature forecast"));
        } else {
            asNumericMeasure(tempFct.getMaximumAirTemperature()).ifPresent(builder::setMaxTemperature);
        }
        if (tempFct.getMaximumAirTemperatureTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> maxTempTime = getCompleteTimeInstant(tempFct.getMaximumAirTemperatureTime(), refCtx);
            if (maxTempTime.isPresent()) {
                builder.setMaxTemperatureTime(maxTempTime.get());
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Invalid maximum temperature time in air temperature " + "forecast"));
            }
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Maximum temperature time missing from air temperature " + "forecast"));
        }
        if (tempFct.getMinimumAirTemperature() != null) {
            asNumericMeasure(tempFct.getMinimumAirTemperature()).ifPresent(builder::setMinTemperature);
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Minimum temperature value missing from air temperature forecast"));
        }
        if (tempFct.getMinimumAirTemperatureTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> minTempTime = getCompleteTimeInstant(tempFct.getMinimumAirTemperatureTime(), refCtx);
            if (minTempTime.isPresent()) {
                builder.setMinTemperatureTime(minTempTime.get());
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Invalid minimum temperature time in air temperature " + "forecast"));
            }
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Minimum temperature time missing from air temperature " + "forecast"));
        }
        resultHandler.accept(builder);
        if (!issues.isEmpty()) {
            issueHandler.accept(issues);
        }
    }
}
