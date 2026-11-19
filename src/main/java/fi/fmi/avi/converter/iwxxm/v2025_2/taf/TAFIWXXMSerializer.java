package fi.fmi.avi.converter.iwxxm.v2025_2.taf;

import aero.aixm511full.AirportHeliportType;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.taf.TAFSerializationSupport;
import fi.fmi.avi.converter.iwxxm.v2025_2.AbstractIWXXM20252Serializer;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.taf.*;
import icao.iwxxm2025_2.*;
import net.opengis.gml32.*;
import org.w3c.dom.Document;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE;

public abstract class TAFIWXXMSerializer<T> extends AbstractIWXXM20252Serializer<TAF, T> {

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

        final Optional<TimeInstantPropertyType> issueTime = TAFSerializationSupport.issueTime(input, result);
        if (!issueTime.isPresent()) {
            return result;
        }
        taf.setIssueTime(issueTime.get());

        final Optional<TimePeriodPropertyType> validPeriod = TAFSerializationSupport.validPeriod(input, validTimeId, result);
        if (input.getValidityTime().isPresent() && !validPeriod.isPresent()) {
            return result;
        }
        validPeriod.ifPresent(taf::setValidPeriod);

        taf.setAerodrome(create(AirportHeliportPropertyType.class, property -> {
            final AirportHeliportType aerodrome = create(AirportHeliportType.class);
            setAerodromeData(aerodrome, input.getAerodrome(), aerodromeId, UUID_PREFIX, UUID_PREFIX);
            property.setAirportHeliport(aerodrome);
        }));

        this.updateBaseForecast(input, taf, validTimeId, result);
        if (input.getChangeForecasts().map(changeForecasts -> !changeForecasts.isEmpty()).orElse(false)) {
            this.updateChangeForecast(input, taf, result);
        }
        if (input.isCancelMessage()) {
            taf.setIsCancelReport(true);
            input.getReferredReportValidPeriod().ifPresent(period -> taf.setCancelledReportValidPeriod(TAFSerializationSupport.cancellationPeriod(period)));
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

    protected void updateChangeForecast(final TAF source, final TAFType target, final ConversionResult<?> result) {
        TAFSerializationSupport.forEachChangeForecast(source, result, (forecast, period) -> {
            final MeteorologicalAerodromeForecastType change = create(MeteorologicalAerodromeForecastType.class);
            change.setChangeIndicator(AerodromeForecastChangeIndicatorType.valueOf(forecast.getChangeIndicator().name()));
            change.setPhenomenonTime(period);
            this.updateForecastResult(forecast, change, result);
            target.getChangeForecast().add(create(MeteorologicalAerodromeForecastPropertyType.class,
                    property -> property.setMeteorologicalAerodromeForecast(change)));
        });
    }

    private void updateForecastResult(final TAFForecast source, final MeteorologicalAerodromeForecastType target, final ConversionResult<?> result) {
        if (source == null) {
            return;
        }

        target.setId(getUUID());
        target.setCloudAndVisibilityOK(source.isCeilingAndVisibilityOk());
        if (TAFSerializationSupport.nonCavokConditions(source, result)) {
            final Optional<NumericMeasure> measure = source.getPrevailingVisibility();
            measure.ifPresent(numericMeasure -> target.setPrevailingVisibility(asMeasure(numericMeasure, LengthType.class)));
            source.getPrevailingVisibilityOperator()//
                    .map(operator -> RelationalOperatorType.valueOf(operator.name()))//
                    .ifPresent(target::setPrevailingVisibilityOperator);
            TAFSerializationSupport.writeForecastWeather(source, AerodromeForecastWeatherType.class, target.getWeather());
            source.getCloud().ifPresent(cloudForecast -> {
                final AerodromeCloudForecastPropertyType cloudProp = create(AerodromeCloudForecastPropertyType.class);
                if (cloudForecast.isNoSignificantCloud()) {
                    //NOTE: iwxxm:cloud is not nillable, so cannot set xsi:nil="true"
                    cloudProp.getNilReason().add(CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                } else {
                    final AerodromeCloudForecastType acFct = create(AerodromeCloudForecastType.class);
                    this.updateForecastClouds(cloudForecast, acFct);
                    cloudProp.setAerodromeCloudForecast(acFct);
                }
                target.setCloud(cloudProp);
            });
        }
        source.getSurfaceWind().ifPresent(sourceWind -> {
            final AerodromeSurfaceWindForecastType wind = create(AerodromeSurfaceWindForecastType.class);
            this.updateForecastSurfaceWind(sourceWind, wind);
            target.setSurfaceWind(create(AerodromeSurfaceWindForecastPropertyType.class, prop -> prop.setAerodromeSurfaceWindForecast(wind)));
        });
    }

    private void setAirTemperatureForecast(final TAFAirTemperatureForecast source, final AerodromeAirTemperatureForecastType target,
            final ConversionResult<?> result) {
        if (source != null) {
            TAFSerializationSupport.temperatureTime(source.getMinTemperatureTime(), "minimum", result).ifPresent(time -> {
                target.setMinimumAirTemperature(asMeasure(source.getMinTemperature(), MeasureType.class));
                target.setMinimumAirTemperatureTime(time);
            });
            TAFSerializationSupport.temperatureTime(source.getMaxTemperatureTime(), "maximum", result).ifPresent(time -> {
                target.setMaximumAirTemperature(asMeasure(source.getMaxTemperature(), MeasureType.class));
                target.setMaximumAirTemperatureTime(time);
            });
        }
    }

    private void updateForecastSurfaceWind(final SurfaceWind source, final AerodromeSurfaceWindForecastType target) {
        if (source != null) {
            target.setMeanWindSpeed(asMeasure(source.getMeanWindSpeed(), SpeedType.class));
            source.getMeanWindDirection().ifPresent(m -> target.setMeanWindDirection(asMeasure(m, AngleType.class)));
            source.getMeanWindSpeedOperator().map(operator -> RelationalOperatorType.valueOf(operator.name()))
                    .ifPresent(target::setMeanWindSpeedOperator);
            source.getWindGust().ifPresent(m -> target.setWindGustSpeed(asMeasure(m, SpeedType.class)));
            source.getWindGustOperator().map(operator -> RelationalOperatorType.valueOf(operator.name()))
                    .ifPresent(target::setWindGustSpeedOperator);
            target.setVariableWindDirection(source.isVariableDirection());
        }
    }

    protected void updateForecastClouds(final CloudForecast source, final AerodromeCloudForecastType target) {
        if (source == null) {
            return;
        }
        target.setId(getUUID());
        source.getVerticalVisibility()//
                .map(measure -> asMeasure(measure, LengthWithNilReasonType.class))//
                .ifPresent(target::setVerticalVisibility);
        if (source.isVerticalVisibilityMissing()) {
            final LengthWithNilReasonType visibility = create(LengthWithNilReasonType.class);
            visibility.setUom("[ft_i]");
            visibility.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN);
            target.setVerticalVisibility(visibility);
        }
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
        TAFSerializationSupport.cloudAmountReference(source, CloudAmountReportedAtAerodromeType.class).ifPresent(target::setAmount);
        TAFSerializationSupport.cloudTypeReference(source, SigConvectiveCloudTypeType.class).ifPresent(target::setCloudType);
    }

    protected void updateBaseForecast(final TAF source, final TAFType target, final String validTimeId, final ConversionResult<?> result) {
        final TAFBaseForecast baseForecastInput = source.getBaseForecast().orElse(null);
        if (baseForecastInput == null) {
            if (!source.isCancelMessage()) {
                target.setBaseForecast(create(MeteorologicalAerodromeForecastPropertyType.class,
                        property -> property.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING)));
            }
            return;
        }
        final MeteorologicalAerodromeForecastType baseFct = create(MeteorologicalAerodromeForecastType.class);
        baseFct.setPhenomenonTime(create(TimePeriodPropertyType.class, prop -> prop.setHref("#" + validTimeId)));

        for (final TAFAirTemperatureForecast airTemp : baseForecastInput.getTemperatures().orElse(Collections.emptyList())) {
            final AerodromeAirTemperatureForecastType tempFct = create(AerodromeAirTemperatureForecastType.class);
            this.setAirTemperatureForecast(airTemp, tempFct, result);
            baseFct.getTemperature().add(create(AerodromeAirTemperatureForecastPropertyType.class, prop -> prop.setAerodromeAirTemperatureForecast(tempFct)));
        }

        this.updateForecastResult(baseForecastInput, baseFct, result);
        target.setBaseForecast(create(MeteorologicalAerodromeForecastPropertyType.class, prop -> prop.setMeteorologicalAerodromeForecast(baseFct)));
    }

    protected void updateMessageMetadata(final TAF source, final ConversionResult<?> result, final TAFType target) throws ConversionException {
        TAFSerializationSupport.writeMetadata(source, result, new TAFSerializationSupport.MetadataWriter() {
            @Override
            public void setPermissibleUsage(final AviationCodeListUser.PermissibleUsage usage,
                    final AviationCodeListUser.PermissibleUsageReason reason, final String supplementary) {
                target.setPermissibleUsage(PermissibleUsageType.valueOf(usage.name()));
                Optional.ofNullable(reason).map(value -> PermissibleUsageReasonType.valueOf(value.name())).ifPresent(target::setPermissibleUsageReason);
                Optional.ofNullable(supplementary).ifPresent(target::setPermissibleUsageSupplementary);
            }

            @Override
            public void setTranslatedBulletin(final String id, final XMLGregorianCalendar receptionTime) {
                Optional.ofNullable(id).ifPresent(target::setTranslatedBulletinID);
                Optional.ofNullable(receptionTime).ifPresent(target::setTranslatedBulletinReceptionTime);
            }

            @Override
            public void setTranslationCentre(final String designator, final String name) {
                Optional.ofNullable(designator).ifPresent(target::setTranslationCentreDesignator);
                Optional.ofNullable(name).ifPresent(target::setTranslationCentreName);
            }

            @Override
            public void setTranslationTime(final XMLGregorianCalendar time) {
                target.setTranslationTime(time);
            }

            @Override
            public void setTranslationFailedTac(final String tac) {
                target.setTranslationFailedTAC(tac);
            }
        });
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = TAFIWXXMSerializer.class.getResourceAsStream("TAF20252CleanUp.xsl");
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
