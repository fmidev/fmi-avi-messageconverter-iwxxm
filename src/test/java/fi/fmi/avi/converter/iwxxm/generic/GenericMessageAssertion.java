package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.Format;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluent assertion helper for {@link GenericAviationWeatherMessage} in tests.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * assertMessage(message)
 *     .hasFormat(Format.IWXXM)
 *     .hasMessageType(MessageType.TAF)
 *     .hasIssueTime("2017-07-30T11:30Z");
 * }</pre>
 *
 * <p>For conversion result assertions, use {@link fi.fmi.avi.converter.iwxxm.ConversionResultAssertion}:</p>
 * <pre>{@code
 * GenericAviationWeatherMessage message = assertConversionResult(result).successfullyConverted();
 * assertMessage(message)
 *     .hasFormat(Format.IWXXM)
 *     .hasMessageType(MessageType.TAF);
 * }</pre>
 */
public final class GenericMessageAssertion {

    private final GenericAviationWeatherMessage message;

    private GenericMessageAssertion(final GenericAviationWeatherMessage message) {
        assertThat(message).as("message").isNotNull();
        this.message = message;
    }

    public static GenericMessageAssertion assertMessage(final GenericAviationWeatherMessage message) {
        return new GenericMessageAssertion(message);
    }

    public GenericMessageAssertion hasFormat(final Format expected) {
        assertThat(message.getMessageFormat()).as("messageFormat").isEqualTo(expected);
        return this;
    }

    public GenericMessageAssertion hasNamespace(final String expected) {
        assertThat(message.getXMLNamespace()).as("xmlNamespace").hasValue(expected);
        return this;
    }

    public GenericMessageAssertion hasMessageType(final MessageType expected) {
        assertThat(message.getMessageType()).as("messageType").hasValue(expected);
        return this;
    }

    public GenericMessageAssertion isTranslated() {
        assertThat(message.isTranslated()).as("translated").isTrue();
        return this;
    }

    public GenericMessageAssertion isNotTranslated() {
        assertThat(message.isTranslated()).as("translated").isFalse();
        return this;
    }

    public GenericMessageAssertion hasReportStatus(final ReportStatus expected) {
        assertThat(message.getReportStatus()).as("reportStatus").isEqualTo(expected);
        return this;
    }

    public GenericMessageAssertion isNil() {
        assertThat(message.isNil()).as("nil").isTrue();
        return this;
    }

    public GenericMessageAssertion isNotNil() {
        assertThat(message.isNil()).as("nil").isFalse();
        return this;
    }

    /**
     * Assert the issue time.
     *
     * @param expected ISO-8601 formatted time (e.g. "2017-07-30T11:30Z")
     */
    public GenericMessageAssertion hasIssueTime(final String expected) {
        assertThat(message.getIssueTime()
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null))
                .as("issueTime")
                .isEqualTo(ZonedDateTime.parse(expected));
        return this;
    }

    /**
     * Assert the observation time.
     *
     * @param expected ISO-8601 formatted time (e.g. "2012-08-22T16:30Z")
     */
    public GenericMessageAssertion hasObservationTime(final String expected) {
        assertThat(message.getObservationTime()
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null))
                .as("observationTime")
                .isEqualTo(ZonedDateTime.parse(expected));
        return this;
    }

    public GenericMessageAssertion hasNoObservationTime() {
        assertThat(message.getObservationTime()).as("observationTime").isEmpty();
        return this;
    }

    /**
     * Assert the validity period.
     *
     * @param start ISO-8601 formatted start time
     * @param end   ISO-8601 formatted end time
     */
    public GenericMessageAssertion hasValidityPeriod(final String start, final String end) {
        assertThat(message.getValidityTime()).as("validityTime").isPresent();
        assertThat(message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null))
                .as("validityTime.startTime")
                .isEqualTo(ZonedDateTime.parse(start));
        assertThat(message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null))
                .as("validityTime.endTime")
                .isEqualTo(ZonedDateTime.parse(end));
        return this;
    }

    public GenericMessageAssertion hasNoValidityPeriod() {
        assertThat(message.getValidityTime()).as("validityTime").isEmpty();
        return this;
    }

    public GenericMessageAssertion hasLocationIndicator(final LocationIndicatorType type, final String value) {
        return hasLocationIndicators(Collections.singletonMap(type, value));
    }

    public GenericMessageAssertion hasLocationIndicators(final Map<LocationIndicatorType, String> expected) {
        assertThat(message.getLocationIndicators()).as("locationIndicators").isEqualTo(expected);
        return this;
    }
}
