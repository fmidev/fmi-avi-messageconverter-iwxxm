package fi.fmi.avi.converter.iwxxm.taf;

import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

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
import org.xml.sax.SAXException;

import aero.aixm511.AirportHeliportType;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationCodeListUser.TAFStatus;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import fi.fmi.avi.model.taf.TAFReference;
import fi.fmi.avi.model.taf.TAFSurfaceWind;
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
public abstract class AbstractTAFIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<TAF, T> {

    protected abstract T render(final TAFType taf, final ConversionHints hints) throws ConversionException;

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
    public ConversionResult<T> convertMessage(TAF input, ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        if (!input.allAerodromeReferencesContainPosition()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "All aerodrome references must contain a reference point location before converting to IWXXM"));
            return result;
        }

        TAFType taf = create(TAFType.class);
        taf.setId("taf-" + UUID.randomUUID().toString());

        final String issueTimeId = "time-" + UUID.randomUUID().toString();
        final String validTimeId = "time-" + UUID.randomUUID().toString();
        final String foiId = "foi-" + UUID.randomUUID().toString();
        final String processId = "process-" + UUID.randomUUID().toString();
        final String aerodromeId = "ad-" + UUID.randomUUID().toString();

        AviationCodeListUser.TAFStatus status = input.getStatus();
        taf.setStatus(TAFReportStatusType.valueOf(status.name()));

        if (input.getIssueTime().getCompleteTime().isPresent()) {
            taf.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> {
                TimeInstantType ti = create(TimeInstantType.class);
                TimePositionType tp = create(TimePositionType.class);
                tp.getValue().add(input.getIssueTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                ti.setTimePosition(tp);
                ti.setId(issueTimeId);
                prop.setTimeInstant(ti);
            }));
        }

        if (AviationCodeListUser.TAFStatus.MISSING != status) {
            if (input.getValidityTime().isPresent()) {
                Optional<PartialOrCompleteTimeInstant> start = input.getValidityTime().get().getStartTime();
                Optional<PartialOrCompleteTimeInstant> end = input.getValidityTime().get().getEndTime();
                if (!start.isPresent() || !end.isPresent()) {
                    result.addIssue(new ConversionIssue(Type.MISSING_DATA,"Validity time for TAF is missing start or end"));
                    return result;
                }
                if (!start.get().getCompleteTime().isPresent() || !end.get().getCompleteTime().isPresent()) {
                    result.addIssue(new ConversionIssue(Type.MISSING_DATA,"Validity time for TAF is not a fully qualified time period"));
                    return result;
                }
                taf.setValidTime(create(TimePeriodPropertyType.class, (prop) -> {
                    TimePeriodType tp = create(TimePeriodType.class);
                    tp.setId(validTimeId);
                    TimePositionType beginPos = create(TimePositionType.class);
                    beginPos.getValue().add(start.get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    TimePositionType endPos = create(TimePositionType.class);
                    endPos.getValue().add(end.get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    tp.setBeginPosition(beginPos);
                    tp.setEndPosition(endPos);
                    prop.setTimePeriod(tp);

                }));
            }
            this.updateChangeForecast(input, taf, issueTimeId, validTimeId, foiId, processId, result);
        }
        this.updateBaseForecast(input, taf, issueTimeId, validTimeId, foiId, processId, aerodromeId, result);

        if (AviationCodeListUser.TAFStatus.CORRECTION == status || AviationCodeListUser.TAFStatus.CANCELLATION == status || AviationCodeListUser.TAFStatus.AMENDMENT == status) {
            this.updatePreviousReportReferences(input, taf, aerodromeId, result);
        } else {
            //TAF: previousReportValidPeriod must not be present unless this cancels, corrects or amends a previous report
            if (input.getReferredReport().isPresent()) {
                result.addIssue(new ConversionIssue(Type.LOGICAL, "TAF contains reference to the previous report even if its type is " + "not amendment, cancellation or correction"));
            }
        }
        try {
            result.setStatus(Status.SUCCESS);
            this.updateMessageMetadata(input, result, taf);
            ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(result);
            validateDocument(taf, TAFType.class, hints, eventHandler);
            if (eventHandler.errorsFound()) {
                result.setStatus(Status.FAIL);
            } else {
                result.setConvertedMessage(this.render(taf, hints));
            }
        } catch (ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message to String", e));
        }
        return result;
    }

    protected void updateBaseForecast(final TAF source, final TAFType target, final String issueTimeId, final String validTimeId, final String foiId,
            final String processId, final String aerodromeId, final ConversionResult<?> result) {

        Optional<TAFBaseForecast> baseForecastInput = source.getBaseForecast();
        if (baseForecastInput.isPresent()) {
            final OMObservationType baseFct = create(OMObservationType.class);
            baseFct.setId("bfct-" + UUID.randomUUID().toString());

            baseFct.setType(create(ReferenceType.class, (ref) -> {
                ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_TYPE);
                ref.setTitle("Aerodrome Base Forecast");

            }));

            baseFct.setPhenomenonTime(create(TimeObjectPropertyType.class, (prop) -> {
                if (AviationCodeListUser.TAFStatus.MISSING == source.getStatus()) {
                    prop.setHref("#" + issueTimeId);
                    prop.setTitle("issueTime of the TAF missing");
                } else {
                    prop.setHref("#" + validTimeId);
                    prop.setTitle("Valid time period of the TAF");
                }
            }));

            baseFct.setResultTime(create(TimeInstantPropertyType.class, (prop) -> {
                prop.setHref("#" + issueTimeId);
                prop.setTitle("issueTime of the TAF");
            }));

            if (AviationCodeListUser.TAFStatus.MISSING != source.getStatus()) {
                baseFct.setValidTime(create(TimePeriodPropertyType.class, (prop) -> {
                    prop.setHref("#" + validTimeId);
                    prop.setTitle("Valid time period of the TAF");
                }));
            }

            baseFct.setProcedure(create(OMProcessPropertyType.class, (prop) -> {
                prop.setAny(createAndWrap(ProcessType.class, (process) -> {
                    process.setId(processId);
                    process.setDescription(create(StringOrRefType.class, (descr) -> {
                        descr.setValue(AviationCodeListUser.TAF_PROCEDURE_DESCRIPTION);
                    }));
                }));
            }));

            baseFct.setObservedProperty(create(ReferenceType.class, (ref) -> {
                ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES);
                ref.setTitle("TAF forecast properties");
            }));

            Aerodrome ad = source.getAerodrome();
            this.updateSamplingFeature(ad, baseFct, foiId, aerodromeId, result);

            this.updateForecastResult(source, baseForecastInput.get(), baseFct, result);

            target.setBaseForecast(create(OMObservationPropertyType.class, (prop) -> {
                prop.setOMObservation(baseFct);
            }));
        } else {
            if (TAFStatus.CANCELLATION != source.getStatus()) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Base forecast missing for non-cancellation TAF"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateChangeForecast(final TAF source, final TAFType target, final String issueTimeId, final String validTimeId, final String foid,
            final String processId, final ConversionResult<?> result) {

        Optional<List<TAFChangeForecast>> fcts = source.getChangeForecasts();
        if (!source.getValidityTime().isPresent() || !source.getValidityTime().get().isComplete()) {
            result.addIssue(new ConversionIssue(Type.SYNTAX, "TAF validity time is not complete"));
            return;
        }
        ZonedDateTime tafValidityStart = source.getValidityTime().get().getStartTime().get().getCompleteTime().get();
        ZonedDateTime tafValidityEnd = source.getValidityTime().get().getEndTime().get().getCompleteTime().get();
        if (fcts.isPresent()) {
            for (TAFChangeForecast fctInput : fcts.get()) {
                final OMObservationType changeFct = create(OMObservationType.class);
                changeFct.setId("chfct-" + UUID.randomUUID().toString());
                changeFct.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_TYPE);
                    ref.setTitle("Aerodrome Forecast");
                }));
                Optional<PartialOrCompleteTimeInstant> start = fctInput.getPeriodOfChange().getStartTime();
                Optional<PartialOrCompleteTimeInstant> end = fctInput.getPeriodOfChange().getEndTime();
                if (start.isPresent() && start.get().getCompleteTime().isPresent()
                        && end.isPresent() && end.get().getCompleteTime().isPresent()) {
                    ZonedDateTime startTime = start.get().getCompleteTime().get();
                    ZonedDateTime endTime = end.get().getCompleteTime().get();
                    if (startTime.isBefore(tafValidityStart)) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL,
                                "Change group start time '" + startTime.toString() + "'" + " is before TAF validity start time "
                                        + tafValidityStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                    }
                    if (endTime.isAfter(tafValidityEnd)) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL,
                                "Change group end time '" + endTime.toString() + "' is " + " after TAF validity end time "
                                        + tafValidityEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                    }

                    changeFct.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                        JAXBElement<?> wrapped = createAndWrap(TimePeriodType.class, (period) -> {
                            period.setId("time-" + UUID.randomUUID().toString());
                            period.setBeginPosition(create(TimePositionType.class, (tPos) -> {
                                tPos.getValue().add(startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                            }));
                            period.setEndPosition(create(TimePositionType.class, (tPos) -> {
                                tPos.getValue().add(endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                            }));
                        });
                        toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                    }));

                    changeFct.setResultTime(create(TimeInstantPropertyType.class, (tiProp) -> {
                        tiProp.setHref("#" + issueTimeId);
                        tiProp.setTitle("Issue time of the TAF");
                    }));

                    changeFct.setValidTime(create(TimePeriodPropertyType.class, (tpProp) -> {
                        tpProp.setHref("#" + validTimeId);
                        tpProp.setTitle("Valid time period of the TAF");
                    }));

                    changeFct.setProcedure(create(OMProcessPropertyType.class, (procProp) -> {
                        procProp.setHref("#" + processId);
                        procProp.setTitle("WMO 49-2 TAF");
                    }));

                    changeFct.setObservedProperty(create(ReferenceType.class, (ref) -> {
                        ref.setHref(AviationCodeListUser.MET_AERODROME_FORECAST_PROPERTIES);
                        ref.setTitle("TAF forecast properties");
                    }));

                    changeFct.setFeatureOfInterest(create(FeaturePropertyType.class, (foiProp) -> {
                        foiProp.setHref("#" + foid);
                        foiProp.setTitle("Same aerodrome as in baseForecast");
                    }));

                    this.updateForecastResult(source, fctInput, changeFct, result);

                    target.getChangeForecast().add(create(OMObservationPropertyType.class, (prop) -> {
                        prop.setOMObservation(changeFct);
                    }));

                } else {
                    result.addIssue(
                            new ConversionIssue(Type.MISSING_DATA, "Missing full validity start and/or end times in change forecast for '" + fctInput.getPeriodOfChange() + "'"));
                }

            }
        }
    }
    private void updateForecastResult(final TAF taf, final TAFForecast source, final OMObservationType target, final ConversionResult<?> result) {
        if (source == null) {
            return;
        }

        if (TAFStatus.MISSING == taf.getStatus()) {
            if (source instanceof TAFBaseForecast) {
                target.setResult(null);
            } else {
                throw new IllegalArgumentException(
                        "Can only add NIL result for type TAFBaseForecast. Tried to " + "add NIL result for type " + source.getClass().getCanonicalName());
            }
        } else {
            MeteorologicalAerodromeForecastRecordType fctRecord = create(MeteorologicalAerodromeForecastRecordType.class);
            fctRecord.setId("rec-" + UUID.randomUUID().toString());
            fctRecord.setCloudAndVisibilityOK(source.isCeilingAndVisibilityOk());
            if (!source.isCeilingAndVisibilityOk()) {
                Optional<NumericMeasure> measure = source.getPrevailingVisibility();
                if (measure.isPresent()) {
                    fctRecord.setPrevailingVisibility(asMeasure(measure.get(), LengthType.class));
                }
                if (source.getPrevailingVisibilityOperator().isPresent()) {
                    fctRecord.setPrevailingVisibilityOperator(RelationalOperatorType.valueOf(source.getPrevailingVisibilityOperator().get().name()));
                }
                if (source.getForecastWeather().isPresent()) {
                    for (Weather weather : source.getForecastWeather().get()) {
                        fctRecord.getWeather().add(create(AerodromeForecastWeatherType.class, (w) -> {
                            w.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + weather.getCode());
                            if (weather.getDescription().isPresent()) {
                                w.setTitle(weather.getDescription().get());
                            }
                        }));
                    }
                } else if (source.isNoSignificantWeather()) {
                    //The expected end of occurrence of weather phenomena shall be indicated by a nil "forecastWeather"
                    // with a nil reason of "http://codes.wmo.int/common/nil/nothingOfOperationalSignificance"
                    fctRecord.getWeather().add(create(AerodromeForecastWeatherType.class, (w) -> {
                        w.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                    }));
                }
                Optional<CloudForecast> cFct = source.getCloud();
                if (cFct.isPresent()) {
                    AerodromeCloudForecastPropertyType cloudProp = create(AerodromeCloudForecastPropertyType.class);
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
                fctRecord.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, (prop) -> {
                    prop.setAerodromeSurfaceWindForecast(wind);
                }));
            }

            if (source instanceof TAFBaseForecast) {
                TAFBaseForecast baseFct = (TAFBaseForecast) source;
                if (baseFct.getTemperatures().isPresent()) {
                    for (TAFAirTemperatureForecast airTemp: baseFct.getTemperatures().get()) {
                        AerodromeAirTemperatureForecastType tempFct = create(AerodromeAirTemperatureForecastType.class);
                        this.setAirTemperatureForecast(airTemp, tempFct, result);
                        fctRecord.getTemperature().add(create(AerodromeAirTemperatureForecastPropertyType.class, (prop) -> {
                            prop.setAerodromeAirTemperatureForecast(tempFct);
                        }));
                    }
                }
            } else if (source instanceof TAFChangeForecast) {
                TAFChangeForecast changeFct = (TAFChangeForecast) source;
                fctRecord.setChangeIndicator(AerodromeForecastChangeIndicatorType.valueOf(changeFct.getChangeIndicator().name()));
            } else {
                throw new IllegalArgumentException("Unknown TAF forecast type " + source.getClass().getCanonicalName());
            }

            target.setResult(create(MeteorologicalAerodromeForecastRecordPropertyType.class, (prop) -> {
                prop.setMeteorologicalAerodromeForecastRecord(fctRecord);
            }));

        }
    }

    private void updateForecastSurfaceWind(final TAFSurfaceWind source, final AerodromeSurfaceWindForecastType target, final ConversionResult<?> result) {
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
                target.setMinimumAirTemperatureTime(create(TimeInstantPropertyType.class, (prop) -> {
                    prop.setTimeInstant(create(TimeInstantType.class, (time) -> {
                        time.setId("time-" + UUID.randomUUID().toString());
                        time.setTimePosition(create(TimePositionType.class, (tPos) -> {
                            tPos.getValue().add(source.getMinTemperatureTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }));
                    }));
                }));
            }

            measure = source.getMaxTemperature();
            if (!source.getMaxTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Time of the base forecast  maximum temperature is not complete"));
            } else {
                target.setMaximumAirTemperature(asMeasure(measure));
                target.setMaximumAirTemperatureTime(create(TimeInstantPropertyType.class, (prop) -> {
                    prop.setTimeInstant(create(TimeInstantType.class, (time) -> {
                        time.setId("time-" + UUID.randomUUID().toString());
                        time.setTimePosition(create(TimePositionType.class, (tPos) -> {
                            tPos.getValue().add(source.getMaxTemperatureTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }));
                    }));
                }));
            }

        }
    }

    protected void updatePreviousReportReferences(final TAF source, final TAFType target, String aerodromeId, final ConversionResult<?> result) {
        if (TAFReportStatusType.CANCELLATION == target.getStatus() || TAFReportStatusType.CORRECTION == target.getStatus()
                || TAFReportStatusType.AMENDMENT == target.getStatus()) {
            Optional<TAFReference> prevReport = source.getReferredReport();
            if (prevReport.isPresent()) {
                target.setPreviousReportAerodrome(create(AirportHeliportPropertyType.class, (prop) -> {
                    if (source.getAerodrome().equals(prevReport.get().getAerodrome())) {
                        prop.setHref("#" + aerodromeId);
                        prop.setTitle("Same aerodrome as the in the base forecast");
                    } else {
                        prop.setAirportHeliport(create(AirportHeliportType.class, (aerodrome) -> {
                            String aId = "ad-" + UUID.randomUUID().toString();
                            this.setAerodromeData(aerodrome, prevReport.get().getAerodrome(), aId);
                        }));
                    }
                }));

                PartialOrCompleteTimePeriod validity = prevReport.get().getValidityTime();
                if (!validity.isComplete()) {
                    result.addIssue(new ConversionIssue(Type.SYNTAX, "Previous report TAF validity time is not complete"));
                    return;
                }
                Optional<PartialOrCompleteTimeInstant> from = validity.getStartTime();
                Optional<PartialOrCompleteTimeInstant> to = validity.getEndTime();
                if (from.isPresent() && from.get().getCompleteTime().isPresent() && to.isPresent() && to.get().getCompleteTime().isPresent()) {
                    target.setPreviousReportValidPeriod(create(TimePeriodPropertyType.class, (prop) -> {
                        prop.setTimePeriod(create(TimePeriodType.class, (period) -> {
                            period.setId("time-" + UUID.randomUUID().toString());
                            period.setBeginPosition(create(TimePositionType.class, (tPos) -> {
                                tPos.getValue().add(from.get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                            }));
                            period.setEndPosition(create(TimePositionType.class, (tPos) -> {
                                tPos.getValue().add(to.get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                            }));
                        }));
                    }));
                } else {
                    result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing full validity time start and/or end of the referred (previous) report"));
                }
            } else {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing the referred (previous) report for report of type " + target.getStatus()));
            }
        }
    }

    protected void updateMessageMetadata(final TAF source, final ConversionResult<?> results, final TAFType target) throws ConversionException {
        try {
            DatatypeFactory f = DatatypeFactory.newInstance();
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
                if (source.getTranslatedBulletinID().isPresent()) {
                    target.setTranslatedBulletinID(source.getTranslatedBulletinID().get());
                }
                if (source.getTranslatedBulletinReceptionTime().isPresent()) {
                    target.setTranslatedBulletinReceptionTime(f.newXMLGregorianCalendar(source.getTranslatedBulletinReceptionTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                if (source.getTranslationCentreDesignator().isPresent()) {
                    target.setTranslationCentreDesignator(source.getTranslationCentreDesignator().get());
                }
                if (source.getTranslationCentreName().isPresent()) {
                    target.setTranslationCentreName(source.getTranslationCentreName().get());
                }
                if (source.getTranslationTime().isPresent()) {
                    target.setTranslationTime(f.newXMLGregorianCalendar(source.getTranslationTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                if (results.getStatus() != Status.SUCCESS && source.getTranslatedTAC().isPresent()) {
                    target.setTranslationFailedTAC(source.getTranslatedTAC().get());
                }
            }
        } catch (DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
        
    }
    
    @Override
    protected Source getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(this.getClass().getResourceAsStream("TAFCleanup.xsl"));
            return new DOMSource(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException("Unexpected problem in reading the cleanup XSL sheet", e);
        }

    }
    
}
