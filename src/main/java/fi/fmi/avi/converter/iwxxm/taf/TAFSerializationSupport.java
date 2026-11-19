package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsage;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsageReason;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/** TAF serialization rules that do not depend on a version's IWXXM or AIXM bindings. */
public final class TAFSerializationSupport extends IWXXMConverterBase {
    private TAFSerializationSupport() {
        throw new AssertionError();
    }

    public static Optional<TimeInstantPropertyType> issueTime(final TAF source, final ConversionResult<?> result) {
        if (!source.getIssueTime().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time for TAF is missing"));
            return Optional.empty();
        }
        return Optional.of(timeInstant(source.getIssueTime().get()));
    }

    public static Optional<TimePeriodPropertyType> validPeriod(final TAF source, final String id, final ConversionResult<?> result) {
        if (!source.getValidityTime().isPresent()) {
            return Optional.empty();
        }
        final PartialOrCompleteTimePeriod period = source.getValidityTime().get();
        final PartialOrCompleteTimeInstant start = period.getStartTime().orElse(null);
        final PartialOrCompleteTimeInstant end = period.getEndTime().orElse(null);
        if (start == null || end == null) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is missing start or end"));
            return Optional.empty();
        }
        if (!start.getCompleteTime().isPresent() || !end.getCompleteTime().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is not a fully qualified time period"));
            return Optional.empty();
        }
        return Optional.of(create(TimePeriodPropertyType.class,
                property -> AbstractIWXXMSerializer.createTimePeriodPropertyType(property, start, end, id)));
    }

    public static TimePeriodPropertyType cancellationPeriod(final PartialOrCompleteTimePeriod period) {
        return create(TimePeriodPropertyType.class, property -> property.setTimePeriod(create(TimePeriodType.class, target -> {
            target.setId(getUUID());
            period.getStartTime().ifPresent(start -> target.setBeginPosition(create(TimePositionType.class,
                    position -> start.getCompleteTime().map(TAFSerializationSupport::formatTime).ifPresent(position.getValue()::add))));
            period.getEndTime().ifPresent(end -> target.setEndPosition(create(TimePositionType.class,
                    position -> end.getCompleteTime().map(TAFSerializationSupport::formatTime).ifPresent(position.getValue()::add))));
        })));
    }

    public static void forEachChangeForecast(final TAF source, final ConversionResult<?> result,
            final BiConsumer<TAFChangeForecast, TimePeriodPropertyType> writer) {
        final ZonedDateTime validityStart = source.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
        final ZonedDateTime validityEnd = source.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
        if (validityStart == null || validityEnd == null) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "TAF validity time is missing complete start and end time"));
            return;
        }
        for (final TAFChangeForecast forecast : source.getChangeForecasts().orElse(Collections.emptyList())) {
            final ZonedDateTime start = forecast.getPeriodOfChange().getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
            final ZonedDateTime end = forecast.getPeriodOfChange().getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null);
            if (start == null || end == null) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Missing full validity start and/or end times in change forecast for '" + forecast.getPeriodOfChange() + "'"));
                continue;
            }
            if (start.isBefore(validityStart)) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                        "Change group start time '" + start + "' is before TAF validity start time " + formatTime(validityStart)));
            }
            if (end.isAfter(validityEnd)) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL,
                        "Change group end time '" + end + "' is  after TAF validity end time " + formatTime(validityEnd)));
            }
            final TimePeriodPropertyType period = create(TimePeriodPropertyType.class, property ->
                    AbstractIWXXMSerializer.createTimePeriodPropertyType(property, PartialOrCompleteTimeInstant.of(start),
                            PartialOrCompleteTimeInstant.of(end), getUUID()));
            writer.accept(forecast, period);
        }
    }

    /** Returns whether visibility, weather and cloud may be written, retaining the existing CAVOK warnings. */
    public static boolean nonCavokConditions(final TAFForecast source, final ConversionResult<?> result) {
        if (!source.isCeilingAndVisibilityOk()) {
            return true;
        }
        if (source.getPrevailingVisibility().isPresent() || source.getPrevailingVisibilityOperator().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Visibility included with CAVOK, ignoring"));
        }
        if (source.getForecastWeather().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Weather included with CAVOK, ignoring"));
        }
        if (source.getCloud().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Cloud included with CAVOK, ignoring"));
        }
        return false;
    }

    public static <T extends ReferenceType> void writeForecastWeather(final TAFForecast source, final Class<T> weatherType, final List<T> target) {
        if (source.getForecastWeather().isPresent()) {
            for (final Weather weather : source.getForecastWeather().get()) {
                target.add(create(weatherType, reference -> reference.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + weather.getCode())));
            }
        } else if (source.isNoSignificantWeather()) {
            // Forecast weather uses nilReason to indicate the end of significant weather phenomena.
            target.add(create(weatherType,
                    reference -> reference.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)));
        }
    }

    public static <T extends ReferenceType> Optional<T> cloudAmountReference(final CloudLayer source, final Class<T> referenceType) {
        return source.getAmount().map(amount -> create(referenceType,
                reference -> reference.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_CLOUD_AMOUNT_REPORTED_AT_AERODROME + amount.getCode())));
    }

    public static <T extends ReferenceType> Optional<T> cloudTypeReference(final CloudLayer source, final Class<T> referenceType) {
        return source.getCloudType().map(type -> create(referenceType,
                reference -> reference.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_CONVECTIVE_CLOUD_TYPE + type.getCode())));
    }

    public static Optional<TimeInstantPropertyType> temperatureTime(final PartialOrCompleteTimeInstant source, final String extremum,
            final ConversionResult<?> result) {
        if (!source.getCompleteTime().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Time of the base forecast  " + extremum + " temperature is not complete"));
            return Optional.empty();
        }
        return Optional.of(timeInstant(source));
    }

    private static TimeInstantPropertyType timeInstant(final PartialOrCompleteTimeInstant source) {
        return create(TimeInstantPropertyType.class, property -> property.setTimeInstant(create(TimeInstantType.class, target -> {
            target.setId(getUUID());
            target.setTimePosition(create(TimePositionType.class,
                    position -> source.getCompleteTime().map(TAFSerializationSupport::formatTime).ifPresent(position.getValue()::add)));
        })));
    }

    public static void writeMetadata(final TAF source, final ConversionResult<?> result, final MetadataWriter writer) throws ConversionException {
        try {
            final DatatypeFactory factory = DatatypeFactory.newInstance();
            if (source.getPermissibleUsage().isPresent()) {
                writer.setPermissibleUsage(source.getPermissibleUsage().get(), source.getPermissibleUsageReason().orElse(null),
                        source.getPermissibleUsageSupplementary().orElse(null));
            } else {
                writer.setPermissibleUsage(PermissibleUsage.NON_OPERATIONAL, PermissibleUsageReason.TEST, null);
            }
            if (source.isTranslated()) {
                writer.setTranslatedBulletin(source.getTranslatedBulletinID().orElse(null),
                        source.getTranslatedBulletinReceptionTime().map(time -> factory.newXMLGregorianCalendar(formatTime(time))).orElse(null));
                writer.setTranslationCentre(source.getTranslationCentreDesignator().orElse(null), source.getTranslationCentreName().orElse(null));
                source.getTranslationTime().map(time -> factory.newXMLGregorianCalendar(formatTime(time))).ifPresent(writer::setTranslationTime);
                if (result.getStatus() != ConversionResult.Status.SUCCESS) {
                    source.getTranslatedTAC().ifPresent(writer::setTranslationFailedTac);
                }
            }
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
    }

    private static String formatTime(final ZonedDateTime time) {
        return time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** Only the binding-specific setters; defaults and translation rules are applied above. */
    public interface MetadataWriter {
        void setPermissibleUsage(PermissibleUsage usage, PermissibleUsageReason reason, String supplementary);

        void setTranslatedBulletin(String id, XMLGregorianCalendar receptionTime);

        void setTranslationCentre(String designator, String name);

        void setTranslationTime(XMLGregorianCalendar time);

        void setTranslationFailedTac(String tac);
    }
}
