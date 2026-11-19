package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Collects TAF properties and builds model values from the GML types shared by the IWXXM bindings. */
public final class TAFScanningSupport extends AbstractIWXXMScanner {
    private TAFScanningSupport() {
        throw new AssertionError();
    }

    public static void collectIssueTime(final TimeInstantPropertyType input, final TAFProperties properties,
            final List<ConversionIssue> issues, final ReferredObjectRetrievalContext context) {
        if (input != null) {
            final Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input, context);
            if (!issueTime.isPresent()) {
                issues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(TAFProperties.Name.ISSUE_TIME, issueTime.get());
            }
        }
    }

    public static Optional<PartialOrCompleteTimePeriod> validPeriod(final TimePeriodPropertyType period, final ReferredObjectRetrievalContext context) {
        return getCompleteTimePeriod(period, context).filter(PartialOrCompleteTimePeriod::isCompleteStrict);
    }

    public static void collectChangeForecastProperties(final TimePeriodPropertyType period, final String indicator,
            final TAFChangeForecastProperties properties, final List<ConversionIssue> issues, final ReferredObjectRetrievalContext context) {
        final Optional<PartialOrCompleteTimePeriod> changePeriod = validPeriod(period, context);
        if (changePeriod.isPresent()) {
            properties.set(TAFChangeForecastProperties.Name.CHANGE_PERIOD, changePeriod.get());
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Valid period is required in change forecasts"));
        }

        if (indicator != null) {
            properties.set(TAFChangeForecastProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TAFChangeIndicator.valueOf(indicator));
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Change indicator is required in change forecasts"));
        }
    }

    public static void collectVisibility(final MeasureType visibility, final String operator, final TAFForecastProperties properties) {
        asNumericMeasure(visibility).ifPresent(measure -> properties.set(TAFForecastProperties.Name.PREVAILING_VISIBILITY, measure));
        if (operator != null) {
            properties.set(TAFForecastProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.valueOf(operator));
        }
    }

    public static void collectWeather(final List<? extends ReferenceType> weather, final TAFForecastProperties properties,
            final ConversionHints hints, final List<ConversionIssue> issues) {
        if (isNoSignificantWeather(weather)) {
            properties.set(TAFForecastProperties.Name.NO_SIGNIFICANT_WEATHER, true);
        } else if (!weather.isEmpty()) {
            final List<Weather> forecasts = new ArrayList<>();
            for (final ReferenceType value : weather) {
                withWeatherBuilderFor(value, hints, builder -> forecasts.add(builder.build()), issues::add);
            }
            if (!forecasts.isEmpty()) {
                properties.set(TAFForecastProperties.Name.FORECAST_WEATHER, toUnmodifiableList(forecasts));
            }
        }
    }

    private static boolean isNoSignificantWeather(final List<? extends ReferenceType> weather) {
        return weather.size() == 1 && hasNothingOfOperationalSignificance(weather.get(0).getNilReason());
    }

    public static void collectCancellation(final Boolean cancelReport, final TimePeriodPropertyType cancelledPeriod,
            final TAFProperties properties, final List<ConversionIssue> issues, final ReferredObjectRetrievalContext context) {
        if (Boolean.TRUE.equals(cancelReport)) {
            properties.set(TAFProperties.Name.IS_CANCEL_MESSAGE, true);
            final Optional<TimePeriodType> period = resolveProperty(cancelledPeriod, TimePeriodType.class, context);
            if (period.isPresent()) {
                collectCancellationPeriod(period.get(), properties, issues, context);
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cancellation period is missing."));
            }
        }
    }

    private static void collectCancellationPeriod(final TimePeriodType period, final TAFProperties properties,
            final List<ConversionIssue> issues, final ReferredObjectRetrievalContext context) {
        final Optional<ZonedDateTime> start = getStartTime(period, context);
        final Optional<ZonedDateTime> end = getEndTime(period, context);
        if (!start.isPresent() && !end.isPresent()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cancellation period is missing."));
            return;
        }
        final PartialOrCompleteTimePeriod.Builder builder = PartialOrCompleteTimePeriod.builder();
        if (start.isPresent()) {
            builder.setStartTime(PartialOrCompleteTimeInstant.of(start.get()));
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Cancellation start time is missing."));
        }
        if (end.isPresent()) {
            builder.setEndTime(PartialOrCompleteTimeInstant.of(end.get()));
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Cancellation end time is missing."));
        }
        properties.set(TAFProperties.Name.VALID_TIME, builder.build());
    }

    public static TAFAirTemperatureForecast temperatures(final MeasureType maximum, final TimeInstantPropertyType maximumTime,
            final MeasureType minimum, final TimeInstantPropertyType minimumTime, final ReferredObjectRetrievalContext context) {
        final TAFAirTemperatureForecastImpl.Builder builder = TAFAirTemperatureForecastImpl.builder();
        asNumericMeasure(maximum).ifPresent(builder::setMaxTemperature);
        if (maximumTime != null) {
            getCompleteTimeInstant(maximumTime, context).ifPresent(builder::setMaxTemperatureTime);
        }
        asNumericMeasure(minimum).ifPresent(builder::setMinTemperature);
        if (minimumTime != null) {
            getCompleteTimeInstant(minimumTime, context).ifPresent(builder::setMinTemperatureTime);
        }
        return builder.build();
    }

    public static SurfaceWind surfaceWind(final boolean variableDirection, final MeasureType direction, final MeasureType speed,
            final String speedOperator, final MeasureType gust, final String gustOperator) {
        final SurfaceWindImpl.Builder builder = SurfaceWindImpl.builder();
        builder.setVariableDirection(variableDirection);
        if (direction != null) {
            builder.setMeanWindDirection(asNumericMeasure(direction));
        }
        asNumericMeasure(speed).ifPresent(builder::setMeanWindSpeed);
        if (speedOperator != null) {
            builder.setMeanWindSpeedOperator(AviationCodeListUser.RelationalOperator.valueOf(speedOperator));
        }
        asNumericMeasure(gust).ifPresent(builder::setWindGust);
        if (gustOperator != null) {
            builder.setWindGustOperator(AviationCodeListUser.RelationalOperator.valueOf(gustOperator));
        }
        return builder.build();
    }

    public static CloudLayer cloudLayer(final ReferenceType amount, final MeasureType base, final ReferenceType cloudType,
            final List<ConversionIssue> issues) {
        final CloudLayerImpl.Builder builder = CloudLayerImpl.builder();
        cloudCode(amount, AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME,
                AviationCodeListUser.CloudAmount.class, "Cloud amount", issues).ifPresent(builder::setAmount);
        asNumericMeasure(base).ifPresent(builder::setBase);
        cloudCode(cloudType, AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE,
                AviationCodeListUser.CloudType.class, "Convective cloud type", issues).ifPresent(builder::setCloudType);
        return builder.build();
    }

    private static <E extends Enum<E>> Optional<E> cloudCode(final ReferenceType reference, final String prefix, final Class<E> codeType,
            final String name, final List<ConversionIssue> issues) {
        if (reference == null || !reference.getNilReason().isEmpty()) {
            return Optional.empty();
        }
        final String href = reference.getHref();
        if (href != null && href.startsWith(prefix)) {
            try {
                return Optional.of(Enum.valueOf(codeType, href.substring(prefix.length())));
            } catch (final IllegalArgumentException e) {
                issues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, name + " has an unknown code: " + href, e));
                return Optional.empty();
            }
        }
        issues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, name + " has an invalid code reference: " + href));
        return Optional.empty();
    }
}
