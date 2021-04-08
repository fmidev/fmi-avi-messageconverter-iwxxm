package fi.fmi.avi.converter.iwxxm.v2_1.taf;

import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.AngleType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.StringOrRefType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.OMProcessPropertyType;
import net.opengis.om20.TimeObjectPropertyType;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Serializer;
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
import fi.fmi.avi.model.taf.TAFForecast;
import icao.iwxxm21.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm21.AerodromeAirTemperatureForecastType;
import icao.iwxxm21.AerodromeCloudForecastPropertyType;
import icao.iwxxm21.AerodromeCloudForecastType;
import icao.iwxxm21.AerodromeForecastChangeIndicatorType;
import icao.iwxxm21.AerodromeForecastWeatherType;
import icao.iwxxm21.AerodromeSurfaceWindForecastPropertyType;
import icao.iwxxm21.AerodromeSurfaceWindForecastType;
import icao.iwxxm21.AirportHeliportPropertyType;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordPropertyType;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;
import icao.iwxxm21.PermissibleUsageReasonType;
import icao.iwxxm21.PermissibleUsageType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.TAFReportStatusType;
import icao.iwxxm21.TAFType;
import wmo.metce2013.ProcessType;

/**
 * Common functionality for conversions related to producing IWXXM TAF messages.
 *
 * @param <T>
 *         the type of the
 */
public abstract class TAFIWXXMSerializer<T> extends AbstractIWXXM21Serializer<TAF, T> {

    protected abstract T render(TAFType taf, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

    /**
     * Converts a TAF object into another format.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the conversion result.
     */
    @Override
    public ConversionResult<T> convertMessage(final TAF input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        if (!input.allAerodromeReferencesContainPosition()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.INFO, ConversionIssue.Type.MISSING_DATA,
                    "At least one of the Aerodrome references does not contain reference point location"));
        }

        final TAFType taf = create(TAFType.class);
        taf.setId("taf-" + UUID.randomUUID().toString());

        final String issueTimeId = "time-" + UUID.randomUUID().toString();
        final String validTimeId = "time-" + UUID.randomUUID().toString();
        final String foiId = "foi-" + UUID.randomUUID().toString();
        final String processId = "process-" + UUID.randomUUID().toString();
        final String aerodromeId = "ad-" + UUID.randomUUID().toString();

        final AviationWeatherMessage.ReportStatus status = input.getReportStatus();
        taf.setStatus(TAFReportStatusType.valueOf(status.name()));

        input.getIssueTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .ifPresent(issueTime -> taf.setIssueTime(create(TimeInstantPropertyType.class, prop -> {
                    final TimeInstantType ti = create(TimeInstantType.class);
                    final TimePositionType tp = create(TimePositionType.class);
                    tp.getValue().add(toIWXXMDateTime(issueTime));
                    ti.setTimePosition(tp);
                    ti.setId(issueTimeId);
                    prop.setTimeInstant(ti);
                })));

        if (!input.isMissingMessage()) {
            if (input.getValidityTime().isPresent()) {
                final String validityStart = input.getValidityTime().<String> flatMap(AbstractIWXXMSerializer::startToIWXXMDateTime).orElse(null);
                final String validityEnd = input.getValidityTime().<String> flatMap(AbstractIWXXMSerializer::endToIWXXMDateTime).orElse(null);
                if (validityStart == null || validityEnd == null) {
                    result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Validity time for TAF is missing complete start or end"));
                    return result;
                }
                taf.setValidTime(create(TimePeriodPropertyType.class, prop -> {
                    final TimePeriodType tp = create(TimePeriodType.class);
                    tp.setId(validTimeId);
                    final TimePositionType beginPos = create(TimePositionType.class);
                    beginPos.getValue().add(validityStart);
                    final TimePositionType endPos = create(TimePositionType.class);
                    endPos.getValue().add(validityEnd);
                    tp.setBeginPosition(beginPos);
                    tp.setEndPosition(endPos);
                    prop.setTimePeriod(tp);

                }));
            }
            this.updateChangeForecast(input, taf, issueTimeId, validTimeId, foiId, processId, result);
        }
        this.updateBaseForecast(input, taf, issueTimeId, validTimeId, foiId, processId, aerodromeId, result);

        if (AviationWeatherMessage.ReportStatus.CORRECTION == status || AviationWeatherMessage.ReportStatus.AMENDMENT == status || input.isCancelMessage()) {
            this.updatePreviousReportReferences(input, taf, aerodromeId, result);
        } else {
            //TAF: previousReportValidPeriod must not be present unless this cancels, corrects or amends a previous report
            if (input.getReferredReportValidPeriod().isPresent()) {
                result.addIssue(new ConversionIssue(Type.LOGICAL,
                        "TAF contains reference to the previous report even if its type is not amendment, cancellation or correction"));
            }
        }
        try {
            this.updateMessageMetadata(input, result, taf);
            final T rendered = this.render(taf, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            result.setConvertedMessage(rendered);

        } catch (final ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    protected void updateBaseForecast(final TAF source, final TAFType target, final String issueTimeId, final String validTimeId, final String foiId,
            final String processId, final String aerodromeId, final ConversionResult<?> result) {

        final Optional<TAFBaseForecast> baseForecastInput = source.getBaseForecast();
        if (baseForecastInput.isPresent()) {
            final OMObservationType baseFct = create(OMObservationType.class);
            baseFct.setId("bfct-" + UUID.randomUUID().toString());

            baseFct.setType(create(ReferenceType.class, ref -> {
                ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_TYPE);
                ref.setTitle("Aerodrome Base Forecast");

            }));

            baseFct.setPhenomenonTime(create(TimeObjectPropertyType.class, prop -> {
                if (source.isMissingMessage()) {
                    prop.setHref("#" + issueTimeId);
                    prop.setTitle("issueTime of the TAF missing");
                } else {
                    prop.setHref("#" + validTimeId);
                    prop.setTitle("Valid time period of the TAF");
                }
            }));

            baseFct.setResultTime(create(TimeInstantPropertyType.class, prop -> {
                prop.setHref("#" + issueTimeId);
                prop.setTitle("issueTime of the TAF");
            }));

            if (!source.isMissingMessage()) {
                baseFct.setValidTime(create(TimePeriodPropertyType.class, prop -> {
                    prop.setHref("#" + validTimeId);
                    prop.setTitle("Valid time period of the TAF");
                }));
            }

            baseFct.setProcedure(create(OMProcessPropertyType.class, prop -> prop.setAny(createAndWrap(ProcessType.class, process -> {
                process.setId(processId);
                process.setDescription(create(StringOrRefType.class, descr -> descr.setValue(AviationCodeListUser.TAF_PROCEDURE_DESCRIPTION)));
            }))));

            baseFct.setObservedProperty(create(ReferenceType.class, ref -> {
                ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES);
                ref.setTitle("TAF forecast properties");
            }));

            this.updateSamplingFeature(source.getAerodrome(), baseFct, foiId, aerodromeId, result);

            this.updateForecastResult(source, baseForecastInput.get(), baseFct, result);

            target.setBaseForecast(create(OMObservationPropertyType.class, prop -> prop.setOMObservation(baseFct)));
        } else {
            if (!source.isCancelMessage()) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Base forecast missing for non-cancellation TAF"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateChangeForecast(final TAF source, final TAFType target, final String issueTimeId, final String validTimeId, final String foid,
            final String processId, final ConversionResult<?> result) {

        final Optional<List<TAFChangeForecast>> fcts = source.getChangeForecasts();
        final ZonedDateTime tafValidityStart = source.getValidityTime()//
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null);
        final ZonedDateTime tafValidityEnd = source.getValidityTime()//
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null);
        if (tafValidityStart == null || tafValidityEnd == null) {
            result.addIssue(new ConversionIssue(Type.SYNTAX, "TAF validity time is not complete"));
            return;
        }
        for (final TAFChangeForecast fctInput : fcts.orElse(Collections.emptyList())) {
            final OMObservationType changeFct = create(OMObservationType.class);
            changeFct.setId("chfct-" + UUID.randomUUID().toString());
            changeFct.setType(create(ReferenceType.class, ref -> {
                ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_TYPE);
                ref.setTitle("Aerodrome Forecast");
            }));
            final ZonedDateTime startTime = fctInput.getPeriodOfChange().getStartTime()//
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                    .orElse(null);
            final ZonedDateTime endTime = fctInput.getPeriodOfChange().getEndTime()//
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                    .orElse(null);
            if (startTime == null || endTime == null) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA,
                        "Missing full validity start and/or end times in change forecast for '" + fctInput.getPeriodOfChange() + "'"));
            } else {
                if (startTime.isBefore(tafValidityStart)) {
                    result.addIssue(new ConversionIssue(Type.LOGICAL,
                            "Change group start time '" + startTime + "' is before TAF validity start time " + toIWXXMDateTime(tafValidityStart)));
                }
                if (endTime.isAfter(tafValidityEnd)) {
                    result.addIssue(new ConversionIssue(Type.LOGICAL,
                            "Change group end time '" + endTime + "' is after TAF validity end time " + toIWXXMDateTime(tafValidityEnd)));
                }

                changeFct.setPhenomenonTime(create(TimeObjectPropertyType.class, toProp -> {
                    final JAXBElement<?> wrapped = createAndWrap(TimePeriodType.class, period -> {
                        period.setId("time-" + UUID.randomUUID().toString());
                        period.setBeginPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(toIWXXMDateTime(startTime))));
                        period.setEndPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(toIWXXMDateTime(endTime))));
                    });
                    toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                }));

                changeFct.setResultTime(create(TimeInstantPropertyType.class, tiProp -> {
                    tiProp.setHref("#" + issueTimeId);
                    tiProp.setTitle("Issue time of the TAF");
                }));

                changeFct.setValidTime(create(TimePeriodPropertyType.class, tpProp -> {
                    tpProp.setHref("#" + validTimeId);
                    tpProp.setTitle("Valid time period of the TAF");
                }));

                changeFct.setProcedure(create(OMProcessPropertyType.class, procProp -> {
                    procProp.setHref("#" + processId);
                    procProp.setTitle("WMO 49-2 TAF");
                }));

                changeFct.setObservedProperty(create(ReferenceType.class, ref -> {
                    ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES);
                    ref.setTitle("TAF forecast properties");
                }));

                changeFct.setFeatureOfInterest(create(FeaturePropertyType.class, foiProp -> {
                    foiProp.setHref("#" + foid);
                    foiProp.setTitle("Same aerodrome as in baseForecast");
                }));

                this.updateForecastResult(source, fctInput, changeFct, result);

                target.getChangeForecast().add(create(OMObservationPropertyType.class, prop -> prop.setOMObservation(changeFct)));

            }

        }
    }

    private void updateForecastResult(final TAF taf, final TAFForecast source, final OMObservationType target, final ConversionResult<?> result) {
        if (source == null) {
            return;
        }

        if (taf.isMissingMessage()) {
            if (source instanceof TAFBaseForecast) {
                target.setResult(null);
            } else {
                throw new IllegalArgumentException(
                        "Can only add NIL result for type TAFBaseForecast. Tried to add NIL result for type " + source.getClass().getCanonicalName());
            }
        } else {
            final MeteorologicalAerodromeForecastRecordType fctRecord = create(MeteorologicalAerodromeForecastRecordType.class);
            fctRecord.setId("rec-" + UUID.randomUUID().toString());
            fctRecord.setCloudAndVisibilityOK(source.isCeilingAndVisibilityOk());
            if (!source.isCeilingAndVisibilityOk()) {
                final Optional<NumericMeasure> measure = source.getPrevailingVisibility();
                measure.ifPresent(numericMeasure -> fctRecord.setPrevailingVisibility(asMeasure(numericMeasure, LengthType.class)));
                if (source.getPrevailingVisibilityOperator().isPresent()) {
                    fctRecord.setPrevailingVisibilityOperator(RelationalOperatorType.valueOf(source.getPrevailingVisibilityOperator().get().name()));
                }
                if (source.getForecastWeather().isPresent()) {
                    for (final Weather weather : source.getForecastWeather().get()) {
                        fctRecord.getWeather().add(create(AerodromeForecastWeatherType.class, w -> {
                            w.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + weather.getCode());
                            if (weather.getDescription().isPresent()) {
                                w.setTitle(weather.getDescription().get());
                            }
                        }));
                    }
                } else if (source.isNoSignificantWeather()) {
                    //The expected end of occurrence of weather phenomena shall be indicated by a nil "forecastWeather"
                    // with a nil reason of "http://codes.wmo.int/common/nil/nothingOfOperationalSignificance"
                    fctRecord.getWeather()
                            .add(create(AerodromeForecastWeatherType.class,
                                    w -> w.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)));
                }
                final Optional<CloudForecast> cFct = source.getCloud();
                if (cFct.isPresent()) {
                    final AerodromeCloudForecastPropertyType cloudProp = create(AerodromeCloudForecastPropertyType.class);
                    if (cFct.get().isNoSignificantCloud() && !source.isCeilingAndVisibilityOk()) {
                        //NOTE: iwxxm:cloud is not nillable, so cannot set xsi:nil="true"
                        cloudProp.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                    } else {
                        final AerodromeCloudForecastType acFct = create(AerodromeCloudForecastType.class);
                        this.updateForecastClouds(cFct.get(), acFct, result);
                        cloudProp.setAerodromeCloudForecast(acFct);
                    }
                    fctRecord.setCloud(cloudProp);
                }
            } else {
                //When CAVOK conditions apply, the appropriate Record type shall have "cloudAndVisibilityOK" set to true
                // and visibility, runway visual range, weather, and cloud information shall be missing
                if (source.getPrevailingVisibility().isPresent() || source.getPrevailingVisibilityOperator().isPresent()) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, Type.LOGICAL, "Visibility included with CAVOK, ignoring"));
                }
                if (source.getForecastWeather().isPresent()) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, Type.LOGICAL, "Weather included with CAVOK, ignoring"));
                }
                if (source.getCloud().isPresent()) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, Type.LOGICAL, "Cloud included with CAVOK, ignoring"));
                }
            }
            if (source.getSurfaceWind().isPresent()) {
                final AerodromeSurfaceWindForecastType wind = create(AerodromeSurfaceWindForecastType.class);
                this.updateForecastSurfaceWind(source.getSurfaceWind().get(), wind, result);
                fctRecord.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, prop -> prop.setAerodromeSurfaceWindForecast(wind)));
            }

            if (source instanceof TAFBaseForecast) {
                final TAFBaseForecast baseFct = (TAFBaseForecast) source;
                if (baseFct.getTemperatures().isPresent()) {
                    for (final TAFAirTemperatureForecast airTemp : baseFct.getTemperatures().get()) {
                        final AerodromeAirTemperatureForecastType tempFct = create(AerodromeAirTemperatureForecastType.class);
                        this.setAirTemperatureForecast(airTemp, tempFct, result);
                        fctRecord.getTemperature()
                                .add(create(AerodromeAirTemperatureForecastPropertyType.class, prop -> prop.setAerodromeAirTemperatureForecast(tempFct)));
                    }
                }
            } else if (source instanceof TAFChangeForecast) {
                final TAFChangeForecast changeFct = (TAFChangeForecast) source;
                fctRecord.setChangeIndicator(AerodromeForecastChangeIndicatorType.valueOf(changeFct.getChangeIndicator().name()));
            } else {
                throw new IllegalArgumentException("Unknown TAF forecast type " + source.getClass().getCanonicalName());
            }

            target.setResult(create(MeteorologicalAerodromeForecastRecordPropertyType.class, prop -> prop.setMeteorologicalAerodromeForecastRecord(fctRecord)));

        }
    }

    private void updateForecastSurfaceWind(final SurfaceWind source, final AerodromeSurfaceWindForecastType target, final ConversionResult<?> result) {
        if (source != null) {
            target.setMeanWindSpeed(asMeasure(source.getMeanWindSpeed(), SpeedType.class));
            source.getMeanWindDirection().ifPresent(m -> target.setMeanWindDirection(asMeasure(m, AngleType.class)));
            source.getWindGust().ifPresent(m -> target.setWindGustSpeed(asMeasure(m, SpeedType.class)));
            target.setVariableWindDirection(source.isVariableDirection());
        }
    }

    private void setAirTemperatureForecast(final TAFAirTemperatureForecast source, final AerodromeAirTemperatureForecastType target,
            final ConversionResult<?> result) {
        if (source != null) {
            NumericMeasure measure = source.getMinTemperature();
            if (!source.getMinTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Time of the base forecast  minimum temperature is not complete"));
            } else {
                target.setMinimumAirTemperature(asMeasure(measure));
                target.setMinimumAirTemperatureTime(create(TimeInstantPropertyType.class, prop -> prop.setTimeInstant(create(TimeInstantType.class, time -> {
                    time.setId("time-" + UUID.randomUUID().toString());
                    time.setTimePosition(create(TimePositionType.class, tPos -> toIWXXMDateTime(source.getMinTemperatureTime())//
                            .ifPresent(tempTime -> tPos.getValue().add(tempTime))));
                }))));
            }

            measure = source.getMaxTemperature();
            if (!source.getMaxTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Time of the base forecast  maximum temperature is not complete"));
            } else {
                target.setMaximumAirTemperature(asMeasure(measure));
                target.setMaximumAirTemperatureTime(create(TimeInstantPropertyType.class, prop -> prop.setTimeInstant(create(TimeInstantType.class, time -> {
                    time.setId("time-" + UUID.randomUUID().toString());
                    time.setTimePosition(create(TimePositionType.class, tPos -> toIWXXMDateTime(source.getMaxTemperatureTime())//
                            .ifPresent(tempTime -> tPos.getValue().add(tempTime))));
                }))));
            }

        }
    }

    protected void updatePreviousReportReferences(final TAF source, final TAFType target, final String aerodromeId, final ConversionResult<?> result) {
        if (TAFReportStatusType.CANCELLATION == target.getStatus() || TAFReportStatusType.CORRECTION == target.getStatus()
                || TAFReportStatusType.AMENDMENT == target.getStatus()) {
            final Optional<PartialOrCompleteTimePeriod> referredReportValidPeriod = source.getReferredReportValidPeriod();
            if (referredReportValidPeriod.isPresent()) {
                target.setPreviousReportAerodrome(create(AirportHeliportPropertyType.class, prop -> {
                    if (source.getBaseForecast().isPresent()) {
                        prop.setHref("#" + aerodromeId);
                        prop.setTitle("Same aerodrome as the in the base forecast");
                    }
                }));

                final PartialOrCompleteTimePeriod validity = referredReportValidPeriod.get();
                if (!validity.isComplete()) {
                    result.addIssue(new ConversionIssue(Type.SYNTAX, "Previous report TAF validity time is not complete"));
                    return;
                }
                final String validityStart = startToIWXXMDateTime(validity).orElse(null);
                final String validityEnd = endToIWXXMDateTime(validity).orElse(null);
                if (validityStart == null || validityEnd == null) {
                    result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing full validity time start and/or end of the referred (previous) report"));
                } else {
                    target.setPreviousReportValidPeriod(create(TimePeriodPropertyType.class, prop -> prop.setTimePeriod(create(TimePeriodType.class, period -> {
                        period.setId("time-" + UUID.randomUUID().toString());
                        period.setBeginPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(validityStart)));
                        period.setEndPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(validityEnd)));
                    }))));
                }
            } else {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing the referred (previous) report for report of type " + target.getStatus()));
            }
        }
    }

    protected void updateMessageMetadata(final TAF source, final ConversionResult<?> results, final TAFType target) throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();
            if (source.getPermissibleUsage().isPresent()) {
                target.setPermissibleUsage(PermissibleUsageType.valueOf(source.getPermissibleUsage().get().name()));
                if (source.getPermissibleUsageReason().isPresent()) {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.valueOf(source.getPermissibleUsageReason().get().name()));
                }
                if (source.getPermissibleUsageSupplementary().isPresent()) {
                    target.setPermissibleUsageSupplementary(source.getPermissibleUsageSupplementary().get());
                }
            } else {
                target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
                target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
            }
            if (source.isTranslated()) {
                source.getTranslatedBulletinID().ifPresent(target::setTranslatedBulletinID);
                source.getTranslatedBulletinReceptionTime()//
                        .map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time)))//
                        .ifPresent(target::setTranslatedBulletinReceptionTime);
                source.getTranslationCentreDesignator().ifPresent(target::setTranslationCentreDesignator);
                source.getTranslationCentreName().ifPresent(target::setTranslationCentreName);
                source.getTranslationTime()//
                        .map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time)))//
                        .ifPresent(target::setTranslationTime);
                if (results.getStatus() != Status.SUCCESS) {
                    source.getTranslatedTAC().ifPresent(target::setTranslationFailedTAC);
                }
            }
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }

    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = this.getClass().getResourceAsStream("TAFCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class ToDOM extends TAFIWXXMSerializer<Document> {

        @Override
        protected Document render(final TAFType taf, final ConversionHints hints) throws ConversionException {
            return this.renderXMLDocument(taf, hints);
        }

        @Override
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }

    }

    public static class ToString extends TAFIWXXMSerializer<String> {

        @Override
        protected String render(final TAFType taf, final ConversionHints hints) throws ConversionException {
            final Document result = renderXMLDocument(taf, hints);
            return renderDOMToString(result, hints);
        }

        @Override
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return TAFIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

    public static class ToJAXBObject extends TAFIWXXMSerializer<TAFType> {

        @Override
        protected TAFType render(final TAFType taf, final ConversionHints hints) {
            return taf;
        }

        @Override
        protected IssueList validate(final TAFType output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) {
            return TAFIWXXMSerializer.validateJAXBObjectAgainstSchemaAndSchematron(output, TAFType.class, schemaInfo, hints);
        }

    }

}
