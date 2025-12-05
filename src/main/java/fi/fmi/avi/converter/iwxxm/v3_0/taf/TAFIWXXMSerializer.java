package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import aero.aixm511.AirportHeliportType;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.taf.*;
import icao.iwxxm30.*;
import icao.iwxxm30.ObjectFactory;
import net.opengis.gml32.*;
import org.w3c.dom.Document;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE;

public abstract class TAFIWXXMSerializer<T> extends AbstractIWXXM30Serializer<TAF, T> {

    protected abstract T render(TAFType taf, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(T output, XMLSchemaInfo schemaInfo, ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final TAF input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        if (!checkCompleteTimeReferences(input, result)) {
            return result;
        }

        final String validTimeId = getUUID();
        final String aerodromeId = getUUID();

        final TAFType taf = create(TAFType.class);
        taf.setId(getUUID());

        taf.setReportStatus(ReportStatusType.valueOf(input.getReportStatus().name()));

        final Optional<PartialOrCompleteTimeInstant> issueTime = input.getIssueTime();
        if (issueTime.isPresent()) {
            taf.setIssueTime(create(TimeInstantPropertyType.class, prop ->
                    prop.setTimeInstant(createTimeInstant(issueTime.get()))));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time for TAF is missing"));
            return result;
        }

        if (input.getValidityTime().isPresent()) {
            final PartialOrCompleteTimeInstant start = input.getValidityTime().get().getStartTime().orElse(null);
            final PartialOrCompleteTimeInstant end = input.getValidityTime().get().getEndTime().orElse(null);
            if (start == null || end == null) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is missing start or end"));
                return result;
            }
            if (!start.getCompleteTime().isPresent() || !end.getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is not a fully qualified time period"));
                return result;
            }
            taf.setValidPeriod(create(TimePeriodPropertyType.class, prop -> createTimePeriodPropertyType(prop, start, end, validTimeId)));
        }

        taf.setAerodrome(create(AirportHeliportPropertyType.class, prop -> prop.setAirportHeliport(create(AirportHeliportType.class, type -> {
            setAerodromeData(type, input.getAerodrome(), aerodromeId, "uuid.", "uuid.");
            prop.setAirportHeliport(type);
        }))));

        this.updateBaseForecast(input, taf, validTimeId, result);

        if (input.getChangeForecasts().map(changeForecasts -> !changeForecasts.isEmpty()).orElse(false)) {
            this.updateChangeForecast(input, taf, result);
        }
        if (input.isCancelMessage()) {
            taf.setIsCancelReport(true);
            input.getReferredReportValidPeriod().ifPresent(period -> updateCancellationReportValidPeriod(period, taf));
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
        target.setCancelledReportValidPeriod(create(TimePeriodPropertyType.class, prop -> prop.setTimePeriod(create(TimePeriodType.class, type -> {
            type.setId("uuid." + UUID.randomUUID());
            if (cancelTime.getStartTime().isPresent()) {
                type.setBeginPosition(create(TimePositionType.class, begin -> startToIWXXMDateTime(cancelTime).ifPresent(time -> begin.getValue().add(time))));
            }
            if (cancelTime.getEndTime().isPresent()) {
                type.setEndPosition(create(TimePositionType.class, end -> endToIWXXMDateTime(cancelTime).ifPresent(time -> end.getValue().add(time))));
            }
        }))));
    }

    protected void updateChangeForecast(final TAF source, final TAFType target, final ConversionResult<?> result) {
        final ZonedDateTime tafValidityStart = source.getValidityTime()//
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null);
        final ZonedDateTime tafValidityEnd = source.getValidityTime()//
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null);
        if (tafValidityStart == null || tafValidityEnd == null) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "TAF validity time is missing complete start and end time"));
            return;
        }

        for (final TAFChangeForecast fctInput : source.getChangeForecasts().orElse(Collections.emptyList())) {
            final MeteorologicalAerodromeForecastType changeFct = create(MeteorologicalAerodromeForecastType.class);
            changeFct.setId(getUUID());
            changeFct.setChangeIndicator(AerodromeForecastChangeIndicatorType.valueOf(fctInput.getChangeIndicator().name()));
            final ZonedDateTime startTime = fctInput.getPeriodOfChange().getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
            final ZonedDateTime endTime = fctInput.getPeriodOfChange().getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
            if (startTime != null && endTime != null) {
                if (startTime.isBefore(tafValidityStart)) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                            "Change group start time '" + startTime + "'" + " is before TAF validity start time " + toIWXXMDateTime(
                                    tafValidityStart)));
                }
                if (endTime.isAfter(tafValidityEnd)) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                            "Change group end time '" + endTime + "' is " + " after TAF validity end time " + toIWXXMDateTime(tafValidityEnd)));
                }

                changeFct.setPhenomenonTime(create(TimePeriodPropertyType.class, toProp -> {
                    final TimePeriodType wrapped = create(TimePeriodType.class, period -> {
                        period.setId(getUUID());
                        period.setBeginPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(toIWXXMDateTime(startTime))));
                        period.setEndPosition(create(TimePositionType.class, tPos -> tPos.getValue().add(toIWXXMDateTime(endTime))));
                    });
                    toProp.setTimePeriod(wrapped);
                }));

                this.updateForecastResult(fctInput, changeFct, result);

                target.getChangeForecast()
                        .add(create(MeteorologicalAerodromeForecastPropertyType.class, prop -> prop.setMeteorologicalAerodromeForecast(changeFct)));
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Missing full validity start and/or end times in change forecast for '" + fctInput.getPeriodOfChange() + "'"));
            }

        }
    }

    private void updateForecastResult(final TAFForecast source, final MeteorologicalAerodromeForecastType target, final ConversionResult<?> result) {
        if (source == null) {
            return;
        }

        target.setId(getUUID());
        target.setCloudAndVisibilityOK(source.isCeilingAndVisibilityOk());
        if (!source.isCeilingAndVisibilityOk()) {
            final Optional<NumericMeasure> measure = source.getPrevailingVisibility();
            measure.ifPresent(numericMeasure -> target.setPrevailingVisibility(asMeasure(numericMeasure, LengthType.class)));
            source.getPrevailingVisibilityOperator()//
                    .map(operator -> RelationalOperatorType.valueOf(operator.name()))//
                    .ifPresent(target::setPrevailingVisibilityOperator);
            if (source.getForecastWeather().isPresent()) {
                for (final Weather weather : source.getForecastWeather().get()) {
                    target.getWeather()
                            .add(create(AerodromeForecastWeatherType.class,
                                    w -> w.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + weather.getCode())));
                }
            } else if (source.isNoSignificantWeather()) {
                //The expected end of occurrence of weather phenomena shall be indicated by a nil "forecastWeather"
                // with a nil reason of "http://codes.wmo.int/common/nil/nothingOfOperationalSignificance"
                target.getWeather()
                        .add(create(AerodromeForecastWeatherType.class,
                                w -> w.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)));
            }
            source.getCloud().ifPresent(cloudForecast -> {
                final AerodromeCloudForecastPropertyType cloudProp = create(AerodromeCloudForecastPropertyType.class);
                if (cloudForecast.isNoSignificantCloud() && !source.isCeilingAndVisibilityOk()) {
                    //NOTE: iwxxm:cloud is not nillable, so cannot set xsi:nil="true"
                    cloudProp.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                } else {
                    final AerodromeCloudForecastType acFct = create(AerodromeCloudForecastType.class);
                    this.updateForecastClouds(cloudForecast, acFct, result);
                    cloudProp.setAerodromeCloudForecast(acFct);
                }
                target.setCloud(cloudProp);
            });
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
        source.getSurfaceWind().ifPresent(sourceWind -> {
            final AerodromeSurfaceWindForecastType wind = create(AerodromeSurfaceWindForecastType.class);
            this.updateForecastSurfaceWind(sourceWind, wind, result);
            target.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, prop -> prop.setAerodromeSurfaceWindForecast(wind)));
        });
    }

    private void setAirTemperatureForecast(final TAFAirTemperatureForecast source, final AerodromeAirTemperatureForecastType target,
                                           final ConversionResult<?> result) {
        if (source != null) {
            NumericMeasure measure = source.getMinTemperature();
            if (!source.getMinTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Time of the base forecast  minimum temperature is not complete"));
            } else {
                target.setMinimumAirTemperature(asMeasure(measure, MeasureType.class));
                target.setMinimumAirTemperatureTime(create(TimeInstantPropertyType.class, prop -> prop.setTimeInstant(create(TimeInstantType.class, time -> {
                    time.setId(getUUID());
                    time.setTimePosition(create(TimePositionType.class, tPos -> source.getMinTemperatureTime().getCompleteTime()//
                            .map(AbstractIWXXMSerializer::toIWXXMDateTime)//
                            .ifPresent(tempTime -> tPos.getValue().add(tempTime))));
                }))));
            }

            measure = source.getMaxTemperature();
            if (!source.getMaxTemperatureTime().getCompleteTime().isPresent()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Time of the base forecast  maximum temperature is not complete"));
            } else {
                target.setMaximumAirTemperature(asMeasure(measure, MeasureType.class));
                target.setMaximumAirTemperatureTime(create(TimeInstantPropertyType.class, prop -> prop.setTimeInstant(create(TimeInstantType.class, time -> {
                    time.setId(getUUID());
                    time.setTimePosition(create(TimePositionType.class, tPos -> source.getMaxTemperatureTime().getCompleteTime()//
                            .map(AbstractIWXXMSerializer::toIWXXMDateTime)//
                            .ifPresent(tempTime -> tPos.getValue().add(tempTime))));
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
        if (source == null) {
            return;
        }
        target.setId(getUUID());
        source.getVerticalVisibility()//
                .map(measure -> new ObjectFactory().createAerodromeCloudTypeVerticalVisibility(asMeasure(measure, LengthWithNilReasonType.class)))//
                .ifPresent(target::setVerticalVisibility);
        for (final CloudLayer layer : source.getLayers().orElse(Collections.emptyList())) {
            target.getLayer()
                    .add(create(CloudLayerPropertyType.class,
                            l -> l.setCloudLayer(create(CloudLayerType.class, cl -> this.setForecastCloudLayerData(cl, layer)))));
        }
    }

    protected void setForecastCloudLayerData(final CloudLayerType target, final CloudLayer source) {
        if (source == null) {
            return;
        }
        source.getBase()//
                .map(sourceBase -> asMeasure(sourceBase, DistanceWithNilReasonType.class))//
                .ifPresent(target::setBase);
        source.getAmount()//
                .map(amount -> create(CloudAmountReportedAtAerodromeType.class,
                        amt -> amt.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + amount.getCode())))//
                .ifPresent(target::setAmount);

        source.getCloudType().ifPresent(cloudType -> {
            final ObjectFactory of = new ObjectFactory();
            final SigConvectiveCloudTypeType sigConvectiveCloudTypeType = of.createSigConvectiveCloudTypeType();
            sigConvectiveCloudTypeType.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + cloudType.getCode());
            target.setCloudType(of.createCloudLayerTypeCloudType(sigConvectiveCloudTypeType));
            target.setCloudType(of.createCloudLayerTypeCloudType(sigConvectiveCloudTypeType));
        });
    }

    protected void updateBaseForecast(final TAF source, final TAFType target, final String validTimeId, final ConversionResult<?> result) {
        final TAFBaseForecast baseForecastInput = source.getBaseForecast().orElse(null);
        if (baseForecastInput == null) {
            if (!source.isCancelMessage()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Base forecast missing for non-cancellation TAF"));
            }
            return;
        }
        final MeteorologicalAerodromeForecastType baseFct = create(MeteorologicalAerodromeForecastType.class);
        baseFct.setId(getUUID());

        baseFct.setPhenomenonTime(create(TimePeriodPropertyType.class, prop -> prop.setHref("#" + validTimeId)));

        baseForecastInput.getPrevailingVisibility()//
                .map(prevailingVisibility -> asMeasure(prevailingVisibility, DistanceWithNilReasonType.class))//
                .ifPresent(baseFct::setPrevailingVisibility);

        if (baseForecastInput.getSurfaceWind().isPresent()) {
            baseFct.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, prop -> {
                final AerodromeSurfaceWindForecastType surfaceWindType = create(AerodromeSurfaceWindForecastType.class);
                final SurfaceWind surfaceWind = baseForecastInput.getSurfaceWind().get();

                surfaceWindType.setVariableWindDirection(surfaceWind.isVariableDirection());
                surfaceWind.getMeanWindDirection()//
                        .map(direction -> asMeasure(direction, AngleWithNilReasonType.class))//
                        .ifPresent(surfaceWindType::setMeanWindDirection);
                surfaceWindType.setMeanWindSpeed(asMeasure(surfaceWind.getMeanWindSpeed(), VelocityWithNilReasonType.class));
                surfaceWind.getMeanWindSpeedOperator()//
                        .map(operator -> RelationalOperatorType.valueOf(operator.name()))//
                        .ifPresent(surfaceWindType::setMeanWindSpeedOperator);
                surfaceWind.getWindGust()//
                        .map(value -> asMeasure(value, VelocityWithNilReasonType.class))//
                        .ifPresent(surfaceWindType::setWindGustSpeed);
                surfaceWind.getWindGustOperator()//
                        .map(operator -> RelationalOperatorType.valueOf(operator.name()))//
                        .ifPresent(surfaceWindType::setWindGustSpeedOperator);

                prop.setAerodromeSurfaceWindForecast(surfaceWindType);
            }));
        }
        for (final TAFAirTemperatureForecast airTemp : baseForecastInput.getTemperatures().orElse(Collections.emptyList())) {
            final AerodromeAirTemperatureForecastType tempFct = create(AerodromeAirTemperatureForecastType.class);
            this.setAirTemperatureForecast(airTemp, tempFct, result);
            baseFct.getTemperature().add(create(AerodromeAirTemperatureForecastPropertyType.class, prop -> prop.setAerodromeAirTemperatureForecast(tempFct)));
        }

        this.updateForecastResult(baseForecastInput, baseFct, result);
        target.setBaseForecast(create(MeteorologicalAerodromeForecastPropertyType.class, prop -> prop.setMeteorologicalAerodromeForecast(baseFct)));
    }

    protected void updateMessageMetadata(final TAF source, final ConversionResult<?> results, final TAFType target) throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();
            if (source.getPermissibleUsage().isPresent()) {
                source.getPermissibleUsage()//
                        .map(value -> PermissibleUsageType.valueOf(value.name()))//
                        .ifPresent(target::setPermissibleUsage);
                source.getPermissibleUsageReason()//
                        .map(reason -> PermissibleUsageReasonType.valueOf(reason.name()))//
                        .ifPresent(target::setPermissibleUsageReason);
                source.getPermissibleUsageSupplementary().ifPresent(target::setPermissibleUsageSupplementary);
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
                if (results.getStatus() != ConversionResult.Status.SUCCESS) {
                    source.getTranslatedTAC().ifPresent(target::setTranslationFailedTAC);
                }
            }
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = TAFIWXXMSerializer.class.getResourceAsStream("TAF30CleanUp.xsl");
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
