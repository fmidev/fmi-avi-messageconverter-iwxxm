package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.gml32.AngleType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;

import org.w3c.dom.Document;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.CodeICAOType;
import aero.aixm511.CodeVerticalDatumType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.TextNameType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.ElevatedPoint;
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
import icao.iwxxm30.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm30.AerodromeAirTemperatureForecastType;
import icao.iwxxm30.AerodromeCloudForecastPropertyType;
import icao.iwxxm30.AerodromeCloudForecastType;
import icao.iwxxm30.AerodromeForecastChangeIndicatorType;
import icao.iwxxm30.AerodromeForecastWeatherType;
import icao.iwxxm30.AerodromeSurfaceWindForecastPropertyType;
import icao.iwxxm30.AerodromeSurfaceWindForecastType;
import icao.iwxxm30.AirportHeliportPropertyType;
import icao.iwxxm30.AngleWithNilReasonType;
import icao.iwxxm30.CloudAmountReportedAtAerodromeType;
import icao.iwxxm30.CloudLayerPropertyType;
import icao.iwxxm30.CloudLayerType;
import icao.iwxxm30.DistanceWithNilReasonType;
import icao.iwxxm30.LengthWithNilReasonType;
import icao.iwxxm30.MeteorologicalAerodromeForecastPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastType;
import icao.iwxxm30.ObjectFactory;
import icao.iwxxm30.PermissibleUsageReasonType;
import icao.iwxxm30.PermissibleUsageType;
import icao.iwxxm30.RelationalOperatorType;
import icao.iwxxm30.ReportStatusType;
import icao.iwxxm30.SigConvectiveCloudTypeType;
import icao.iwxxm30.TAFType;
import icao.iwxxm30.VelocityWithNilReasonType;

public abstract class TAFIWXXMSerializer<T> extends AbstractIWXXM30Serializer<TAF, T> {

    protected abstract T render(TAFType taf, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(T output, XMLSchemaInfo schemaInfo, ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final TAF input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        if (!checkCompleteTimeReferences(input, result)) {
            return result;
        }
//TODO: Check
        //checkAerodromeReferencePositions(input, result);

        final String issueTimeId = "uuid." + UUID.randomUUID().toString();
        final String validTimeId = "uuid." + UUID.randomUUID().toString();
        final String aerodromeId = "uuid." + UUID.randomUUID().toString();

        TAFType taf = create(TAFType.class);
        taf.setId("uuid." + UUID.randomUUID().toString());

        final AviationWeatherMessage.ReportStatus status = input.getReportStatus().get();
        taf.setReportStatus(ReportStatusType.valueOf(status.name()));

        if (input.getIssueTime().get().getCompleteTime().isPresent()) {
            taf.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> {
                createTimeInstantProperty(input, prop, issueTimeId);
            }));
        }

        if (input.getValidityTime().isPresent()) {
            final Optional<PartialOrCompleteTimeInstant> start = input.getValidityTime().get().getStartTime();
            final Optional<PartialOrCompleteTimeInstant> end = input.getValidityTime().get().getEndTime();
            if (!start.isPresent() || !end.isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is missing start or end"));
                return result;
            }
            if (!start.get().getCompleteTime().isPresent() || !end.get().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is not a fully qualified time period"));
                return result;
            }
            taf.setValidPeriod(create(TimePeriodPropertyType.class, (prop) -> {
                createTimePeriodPropertyType(prop, start.get(), end.get(), validTimeId);
            }));
        }

        this.updateAerodrome(input.getAerodrome(), taf, aerodromeId);

        this.updateBaseForecast(input, taf, validTimeId, result);

        if (input.getChangeForecasts().isPresent() && input.getChangeForecasts().get().size() > 0) {
            this.updateChangeForecast(input, taf, result);
        }

        if (input.isCancelMessage() && input.getReferredReportValidPeriod().isPresent()) {
            taf.setIsCancelReport(true);
            updateCancellationReportValidPeriod(input.getReferredReportValidPeriod().get(), taf);
        }

        try {
            this.updateMessageMetadata(input, result, taf);
            final T rendered = this.render(taf, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));

            result.setConvertedMessage(rendered);

        } catch (final Exception e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    private void updateCancellationReportValidPeriod(final PartialOrCompleteTimePeriod cancelTime, final TAFType target) {
        target.setCancelledReportValidPeriod(create(TimePeriodPropertyType.class, prop -> {
            prop.setTimePeriod(create(TimePeriodType.class, type -> {
                type.setId("uuid." + UUID.randomUUID());

                if (cancelTime.getStartTime().isPresent()) {
                    type.setBeginPosition(create(TimePositionType.class, begin -> {
                        begin.getValue().add(cancelTime.getStartTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    }));
                }

                if (cancelTime.getEndTime().isPresent()) {
                    type.setEndPosition(create(TimePositionType.class, begin -> {
                        begin.getValue().add(cancelTime.getEndTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    }));
                }
            }));
        }));
    }

    private void updateAerodrome(final Aerodrome aerodrome, final TAFType target, final String aerodromeId) {
        target.setAerodrome(create(AirportHeliportPropertyType.class, prop -> {
            prop.setAirportHeliport(create(AirportHeliportType.class, type -> {
                type.setId(aerodromeId);
                AirportHeliportTimeSliceType timeSlice = create(AirportHeliportTimeSliceType.class);
                timeSlice.setId("uuid." + UUID.randomUUID());

                timeSlice.setInterpretation("SNAPSHOT");
                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));

                timeSlice.setDesignator(create(CodeAirportHeliportDesignatorType.class, (code) -> code.setValue(aerodrome.getDesignator())));

                if (aerodrome.getName().isPresent()) {
                    TextNameType name = new TextNameType();
                    name.setValue(aerodrome.getName().get());
                    timeSlice.setPortName(name);
                }

                if (aerodrome.getLocationIndicatorICAO().isPresent()) {
                    CodeICAOType code = new CodeICAOType();
                    code.setValue(aerodrome.getLocationIndicatorICAO().get());
                    timeSlice.setLocationIndicatorICAO(code);
                }

                if (aerodrome.getReferencePoint().isPresent()) {
                    timeSlice.setARP(create(ElevatedPointPropertyType.class, elevatedPointProp -> {
                        ElevatedPoint sourcePoint = aerodrome.getReferencePoint().get();
                        ElevatedPointType targetPoint = create(ElevatedPointType.class);
                        targetPoint.setId("uuid." + UUID.randomUUID());

                        targetPoint.setElevation(create(ValDistanceVerticalType.class, verticalType -> {
                            verticalType.setUom(sourcePoint.getElevationUom().get());
                            verticalType.setValue(sourcePoint.getElevationValue().get().toString());
                        }));

                        targetPoint.setPos(create(DirectPositionType.class, pos -> {
                            pos.getValue().addAll(sourcePoint.getCoordinates());
                        }));

                        if (sourcePoint.getVerticalDatum().isPresent()) {
                            targetPoint.setVerticalDatum(
                                    create(CodeVerticalDatumType.class, verticalCode -> verticalCode.setValue(sourcePoint.getVerticalDatum().get())));
                        }

                        targetPoint.setSrsDimension(BigInteger.valueOf(2));
                        targetPoint.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);

                        targetPoint.getAxisLabels().addAll(Arrays.asList("Lat", "Long"));
                        elevatedPointProp.setElevatedPoint(targetPoint);
                    }));
                }
                type.getTimeSlice().add(create(AirportHeliportTimeSlicePropertyType.class, sliceProp -> sliceProp.setAirportHeliportTimeSlice(timeSlice)));
                prop.setAirportHeliport(type);
            }));
        }));
    }

    protected void updateChangeForecast(final TAF source, final TAFType target, final ConversionResult<?> result) {

        final Optional<List<TAFChangeForecast>> fcts = source.getChangeForecasts();
        if (!source.getValidityTime().isPresent() || !source.getValidityTime().get().isComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "TAF validity time is not complete"));
            return;
        }
        final ZonedDateTime tafValidityStart = source.getValidityTime().get().getStartTime().get().getCompleteTime().get();
        final ZonedDateTime tafValidityEnd = source.getValidityTime().get().getEndTime().get().getCompleteTime().get();
        if (fcts.isPresent()) {
            for (final TAFChangeForecast fctInput : fcts.get()) {
                final MeteorologicalAerodromeForecastType changeFct = create(MeteorologicalAerodromeForecastType.class);
                changeFct.setId("uuid." + UUID.randomUUID().toString());
                final Optional<PartialOrCompleteTimeInstant> start = fctInput.getPeriodOfChange().getStartTime();
                final Optional<PartialOrCompleteTimeInstant> end = fctInput.getPeriodOfChange().getEndTime();
                if (start.isPresent() && start.get().getCompleteTime().isPresent() && end.isPresent() && end.get().getCompleteTime().isPresent()) {
                    final ZonedDateTime startTime = start.get().getCompleteTime().get();
                    final ZonedDateTime endTime = end.get().getCompleteTime().get();
                    if (startTime.isBefore(tafValidityStart)) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                                "Change group start time '" + startTime.toString() + "'" + " is before TAF validity start time " + tafValidityStart.format(
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                    }
                    if (endTime.isAfter(tafValidityEnd)) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                                "Change group end time '" + endTime.toString() + "' is " + " after TAF validity end time " + tafValidityEnd.format(
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                    }

                    changeFct.setPhenomenonTime(create(TimePeriodPropertyType.class, (toProp) -> {
                        final TimePeriodType wrapped = create(TimePeriodType.class, (period) -> {
                            period.setId("uuid." + UUID.randomUUID().toString());
                            period.setBeginPosition(
                                    create(TimePositionType.class, (tPos) -> tPos.getValue().add(startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
                            period.setEndPosition(
                                    create(TimePositionType.class, (tPos) -> tPos.getValue().add(endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
                        });
                        toProp.setTimePeriod(wrapped);
                    }));

                    this.updateForecastResult(fctInput, changeFct, result);

                    target.getChangeForecast()
                            .add(create(MeteorologicalAerodromeForecastPropertyType.class, (prop) -> prop.setMeteorologicalAerodromeForecast(changeFct)));

                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                            "Missing full validity start and/or end times in change forecast for '" + fctInput.getPeriodOfChange() + "'"));
                }

            }
        }
    }

    private void updateForecastResult(final TAFForecast source, final MeteorologicalAerodromeForecastType target, final ConversionResult<?> result) {
        if (source == null) {
            return;
        }

        target.setId("uuid." + UUID.randomUUID().toString());
        target.setCloudAndVisibilityOK(source.isCeilingAndVisibilityOk());
        if (!source.isCeilingAndVisibilityOk()) {
            final Optional<NumericMeasure> measure = source.getPrevailingVisibility();
            measure.ifPresent(numericMeasure -> target.setPrevailingVisibility(asMeasure(numericMeasure, LengthType.class)));
            if (source.getPrevailingVisibilityOperator().isPresent()) {
                target.setPrevailingVisibilityOperator(RelationalOperatorType.valueOf(source.getPrevailingVisibilityOperator().get().name()));
            }
            if (source.getForecastWeather().isPresent()) {
                for (final Weather weather : source.getForecastWeather().get()) {
                    target.getWeather().add(create(AerodromeForecastWeatherType.class, (w) -> {
                        w.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + weather.getCode());
                        if (weather.getDescription().isPresent()) {
                            w.setTitle(weather.getDescription().get());
                        }
                    }));
                }
            } else if (source.isNoSignificantWeather()) {
                //The expected end of occurrence of weather phenomena shall be indicated by a nil "forecastWeather"
                // with a nil reason of "http://codes.wmo.int/common/nil/nothingOfOperationalSignificance"
                target.getWeather()
                        .add(create(AerodromeForecastWeatherType.class,
                                (w) -> w.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)));
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
                target.setCloud(cloudProp);
            }
        } else {
            //When CAVOK conditions apply, the appropriate Record type shall have "cloudAndVisibilityOK" set to true
            // and visibility, runway visual range, weather, and cloud information shall be missing
            if (source.getPrevailingVisibility().isPresent() || source.getPrevailingVisibilityOperator().isPresent()) {
                result.addIssue(
                        new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Visibility included with CAVOK, ignoring"));
            }
            if (source.getForecastWeather().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Weather included with CAVOK, ignoring"));
            }
            if (source.getCloud().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Cloud included with CAVOK, ignoring"));
            }
        }
        if (source.getSurfaceWind().isPresent()) {
            final AerodromeSurfaceWindForecastType wind = create(AerodromeSurfaceWindForecastType.class);
            this.updateForecastSurfaceWind(source.getSurfaceWind().get(), wind, result);
            target.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, (prop) -> prop.setAerodromeSurfaceWindForecast(wind)));
        }

        if (source instanceof TAFBaseForecast) {
            final TAFBaseForecast baseFct = (TAFBaseForecast) source;
            if (baseFct.getTemperatures().isPresent()) {
                for (final TAFAirTemperatureForecast airTemp : baseFct.getTemperatures().get()) {
                    final AerodromeAirTemperatureForecastType tempFct = create(AerodromeAirTemperatureForecastType.class);
                    this.setAirTemperatureForecast(airTemp, tempFct, result);
                    target.getTemperature()
                            .add(create(AerodromeAirTemperatureForecastPropertyType.class, (prop) -> prop.setAerodromeAirTemperatureForecast(tempFct)));
                }
            }
        } else if (source instanceof TAFChangeForecast) {
            final TAFChangeForecast changeFct = (TAFChangeForecast) source;
            target.setChangeIndicator(AerodromeForecastChangeIndicatorType.valueOf(changeFct.getChangeIndicator().name()));
        } else {
            throw new IllegalArgumentException("Unknown TAF forecast type " + source.getClass().getCanonicalName());
        }
    }

    private void setAirTemperatureForecast(final TAFAirTemperatureForecast source, final AerodromeAirTemperatureForecastType target,
            final ConversionResult<?> result) {
        if (source != null) {
            NumericMeasure measure = source.getMinTemperature();
            if (!source.getMinTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Time of the base forecast  minimum temperature is not complete"));
            } else {
                target.setMinimumAirTemperature(asMeasure(measure));
                target.setMinimumAirTemperatureTime(
                        create(TimeInstantPropertyType.class, (prop) -> prop.setTimeInstant(create(TimeInstantType.class, (time) -> {
                            time.setId("uuid." + UUID.randomUUID().toString());
                            time.setTimePosition(create(TimePositionType.class, (tPos) -> tPos.getValue()
                                    .add(source.getMinTemperatureTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
                        }))));
            }

            measure = source.getMaxTemperature();
            if (!source.getMaxTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Time of the base forecast  maximum temperature is not complete"));
            } else {
                target.setMaximumAirTemperature(asMeasure(measure));
                target.setMaximumAirTemperatureTime(
                        create(TimeInstantPropertyType.class, (prop) -> prop.setTimeInstant(create(TimeInstantType.class, (time) -> {
                            time.setId("uuid." + UUID.randomUUID().toString());
                            time.setTimePosition(create(TimePositionType.class, (tPos) -> tPos.getValue()
                                    .add(source.getMaxTemperatureTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
                        }))));
            }

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

    protected void updateForecastClouds(final CloudForecast source, final AerodromeCloudForecastType target, final ConversionResult<?> result) {
        if (source != null) {
            target.setId("uuid." + UUID.randomUUID().toString());
            final Optional<NumericMeasure> measure = source.getVerticalVisibility();
            if (measure.isPresent()) {
                final LengthWithNilReasonType vvValue = create(LengthWithNilReasonType.class, (vv) -> {
                    vv.setValue(measure.get().getValue());
                    vv.setUom(measure.get().getUom());
                });
                target.setVerticalVisibility(new ObjectFactory().createAerodromeCloudTypeVerticalVisibility(vvValue));
            }
            if (source.getLayers().isPresent()) {
                for (final CloudLayer layer : source.getLayers().get()) {
                    target.getLayer()
                            .add(create(CloudLayerPropertyType.class,
                                    (l) -> l.setCloudLayer(create(CloudLayerType.class, (cl) -> this.setForecastCloudLayerData(cl, layer)))));
                }
            }
        }
    }

    protected void setForecastCloudLayerData(final CloudLayerType target, final CloudLayer source) {
        if (source != null) {
            if (source.getBase().isPresent()) {
                target.setBase(create(DistanceWithNilReasonType.class, (base) -> {
                    base.setUom(source.getBase().get().getUom());
                    base.setValue(source.getBase().get().getValue());
                }));
            }

            final Optional<AviationCodeListUser.CloudAmount> amount = source.getAmount();
            amount.ifPresent(cloudAmount -> target.setAmount(create(CloudAmountReportedAtAerodromeType.class, (amt) -> {
                amt.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + cloudAmount.getCode());
            })));

            final Optional<AviationCodeListUser.CloudType> type = source.getCloudType();
            if (type.isPresent()) {
                ObjectFactory of = new ObjectFactory();
                SigConvectiveCloudTypeType sigConvectiveCloudTypeType = of.createSigConvectiveCloudTypeType();
                sigConvectiveCloudTypeType.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + type.get().getCode());
                target.setCloudType(of.createCloudLayerTypeCloudType(sigConvectiveCloudTypeType));
                target.setCloudType(of.createCloudLayerTypeCloudType(sigConvectiveCloudTypeType));
            }
        }
    }

    protected void updateBaseForecast(final TAF source, final TAFType target, final String validTimeId, final ConversionResult<?> result) {

        final Optional<TAFBaseForecast> baseForecastInput = source.getBaseForecast();
        if (baseForecastInput.isPresent()) {
            final MeteorologicalAerodromeForecastType baseFct = create(MeteorologicalAerodromeForecastType.class);
            baseFct.setId("uuid." + UUID.randomUUID().toString());

            baseFct.setPhenomenonTime(create(TimePeriodPropertyType.class, (prop) -> {
                prop.setHref("#" + validTimeId);
                prop.setTitle("Valid time period of the TAF");
            }));

            baseFct.setPrevailingVisibility(create(DistanceWithNilReasonType.class, (prop) -> {
                NumericMeasure prevailingVisibility = baseForecastInput.get().getPrevailingVisibility().get();
                prop.setUom(prevailingVisibility.getUom());
                prop.setValue(prevailingVisibility.getValue());
            }));

            if (baseForecastInput.get().getSurfaceWind().isPresent()) {
                baseFct.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, prop -> {
                    AerodromeSurfaceWindForecastType surfaceWindType = create(AerodromeSurfaceWindForecastType.class);
                    SurfaceWind surfaceWind = baseForecastInput.get().getSurfaceWind().get();

                    surfaceWindType.setVariableWindDirection(surfaceWind.isVariableDirection());

                    if (surfaceWind.getMeanWindDirection().isPresent()) {
                        surfaceWindType.setMeanWindDirection(create(AngleWithNilReasonType.class, (direction) -> {
                            direction.setUom(surfaceWind.getMeanWindDirection().get().getUom());
                            direction.setValue(surfaceWind.getMeanWindDirection().get().getValue());
                        }));
                    }

                    surfaceWindType.setMeanWindSpeed(create(VelocityWithNilReasonType.class, (speed) -> {
                        speed.setUom(surfaceWind.getMeanWindSpeed().getUom());
                        speed.setValue(surfaceWind.getMeanWindSpeed().getValue());
                    }));

                    if (surfaceWind.getMeanWindSpeedOperator().isPresent()) {
                        surfaceWindType.setMeanWindSpeedOperator(RelationalOperatorType.valueOf(surfaceWind.getMeanWindSpeedOperator().get().name()));
                    }

                    if (surfaceWind.getWindGust().isPresent()) {
                        surfaceWindType.setWindGustSpeed(create(VelocityWithNilReasonType.class, (gustSpeed) -> {
                            gustSpeed.setUom(surfaceWind.getWindGust().get().getUom());
                            gustSpeed.setValue(surfaceWind.getWindGust().get().getValue());
                        }));
                    }

                    if (surfaceWind.getWindGustOperator().isPresent()) {
                        surfaceWindType.setWindGustSpeedOperator(RelationalOperatorType.valueOf(surfaceWind.getWindGustOperator().get().name()));
                    }
                    prop.setAerodromeSurfaceWindForecast(surfaceWindType);
                }));
            }

            this.updateForecastResult(baseForecastInput.get(), baseFct, result);
            target.setBaseForecast(create(MeteorologicalAerodromeForecastPropertyType.class, (prop) -> prop.setMeteorologicalAerodromeForecast(baseFct)));
        } else {
            if (AviationWeatherMessage.ReportStatus.AMENDMENT.equals(source.getReportStatus()) || AviationWeatherMessage.ReportStatus.CORRECTION.equals(
                    source.getReportStatus())) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Base forecast missing for non-cancellation TAF"));
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
                if (source.getTranslatedBulletinID().isPresent()) {
                    target.setTranslatedBulletinID(source.getTranslatedBulletinID().get());
                }
                if (source.getTranslatedBulletinReceptionTime().isPresent()) {
                    target.setTranslatedBulletinReceptionTime(
                            f.newXMLGregorianCalendar(source.getTranslatedBulletinReceptionTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
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
                if (results.getStatus() != ConversionResult.Status.SUCCESS && source.getTranslatedTAC().isPresent()) {
                    target.setTranslationFailedTAC(source.getTranslatedTAC().get());
                }
            }
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = this.getClass().getResourceAsStream("TAF30CleanUp.xsl");
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
            return TAFIWXXMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
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
}
