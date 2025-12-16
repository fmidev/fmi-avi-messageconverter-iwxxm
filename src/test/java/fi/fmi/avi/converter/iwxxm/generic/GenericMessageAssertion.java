package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.Format;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluent assertion helper for {@link GenericAviationWeatherMessage} in tests.
 * <p>
 * Example usage:
 * </p>
 * <p>
 * GenericMessageAssertion.assertMessage(result)
 *     .hasNoIssues()
 *     .hasFormat(Format.IWXXM)
 *     .hasNamespace(IWXXM_2_1_NAMESPACE)
 *     .hasMessageType(MessageType.TAF)
 *     .isTranslated()
 *     .hasReportStatus(ReportStatus.NORMAL)
 *     .hasIssueTime("2017-07-30T11:30Z")
 *     .hasValidityPeriod("2017-07-30T12:00Z", "2017-07-31T12:00Z")
 *     .hasLocationIndicator(LocationIndicatorType.AERODROME, "EETN");
 * </p>
 */
public final class GenericMessageAssertion {

    private final ConversionResult<GenericAviationWeatherMessage> result;
    private final GenericAviationWeatherMessage message;

    private GenericMessageAssertion(final ConversionResult<GenericAviationWeatherMessage> result) {
        this.result = result;
        this.message = result.getConvertedMessage().orElse(null);
    }

    private GenericMessageAssertion(final GenericAviationWeatherMessage message) {
        this.result = null;
        this.message = message;
    }

    public static GenericMessageAssertion assertMessage(final ConversionResult<GenericAviationWeatherMessage> result) {
        assertThat(result).as("ConversionResult").isNotNull();
        return new GenericMessageAssertion(result);
    }

    public static GenericMessageAssertion assertFirstMessage(final ConversionResult<GenericMeteorologicalBulletin> result) {
        assertThat(result).as("ConversionResult").isNotNull();
        assertThat(result.getConversionIssues()).as("conversionIssues").isEmpty();
        assertThat(result.getStatus()).as("status").isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        assertThat(result.getConvertedMessage().get().getMessages()).as("messages").isNotEmpty();
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);
        return new GenericMessageAssertion(message);
    }

    private void assertMessageNotNull() {
        assertThat(message).as("message").isNotNull();
    }

    private void assertResultNotNull() {
        assertThat(result).as("result (use assertMessage with ConversionResult)").isNotNull();
    }

    public GenericMessageAssertion hasNoIssues() {
        assertResultNotNull();
        assertThat(result.getConversionIssues()).as("conversionIssues").isEmpty();
        assertThat(result.getStatus()).as("status").isEqualTo(ConversionResult.Status.SUCCESS);
        return this;
    }

    public GenericMessageAssertion hasIssues() {
        assertResultNotNull();
        assertThat(result.getConversionIssues()).as("conversionIssues").isNotEmpty();
        return this;
    }

    public GenericMessageAssertion hasIssue(final ConversionIssue.Severity severity, final ConversionIssue.Type type) {
        assertResultNotNull();
        assertThat(result.getConversionIssues())
                .as("conversionIssues with severity " + severity + " and type " + type)
                .anyMatch(issue -> issue.getSeverity() == severity && issue.getType() == type);
        return this;
    }

    public GenericMessageAssertion hasIssue(final ConversionIssue.Severity severity,
                                            final ConversionIssue.Type type,
                                            final String messageSubstring) {
        assertResultNotNull();
        assertThat(result.getConversionIssues())
                .as("conversionIssues with severity " + severity + ", type " + type + ", containing '" + messageSubstring + "'")
                .anyMatch(issue -> issue.getSeverity() == severity
                        && issue.getType() == type
                        && issue.getMessage() != null
                        && issue.getMessage().contains(messageSubstring));
        return this;
    }

    public GenericMessageAssertion isPresent() {
        assertResultNotNull();
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        return this;
    }

    public GenericMessageAssertion hasFormat(final Format expected) {
        assertMessageNotNull();
        assertThat(message.getMessageFormat()).as("messageFormat").isEqualTo(expected);
        return this;
    }

    public GenericMessageAssertion hasNamespace(final String expected) {
        assertMessageNotNull();
        assertThat(message.getXMLNamespace()).as("xmlNamespace").hasValue(expected);
        return this;
    }

    public GenericMessageAssertion hasMessageType(final MessageType expected) {
        assertMessageNotNull();
        assertThat(message.getMessageType()).as("messageType").hasValue(expected);
        return this;
    }

    public GenericMessageAssertion isTranslated() {
        assertMessageNotNull();
        assertThat(message.isTranslated()).as("translated").isTrue();
        return this;
    }

    public GenericMessageAssertion isNotTranslated() {
        assertMessageNotNull();
        assertThat(message.isTranslated()).as("translated").isFalse();
        return this;
    }

    public GenericMessageAssertion hasReportStatus(final ReportStatus expected) {
        assertMessageNotNull();
        assertThat(message.getReportStatus()).as("reportStatus").isEqualTo(expected);
        return this;
    }

    /**
     * Assert the issue time.
     *
     * @param expected ISO-8601 formatted time (e.g. "2017-07-30T11:30Z")
     */
    public GenericMessageAssertion hasIssueTime(final String expected) {
        assertMessageNotNull();
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
        assertMessageNotNull();
        assertThat(message.getObservationTime()
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .orElse(null))
                .as("observationTime")
                .isEqualTo(ZonedDateTime.parse(expected));
        return this;
    }

    public GenericMessageAssertion hasNoObservationTime() {
        assertMessageNotNull();
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
        assertMessageNotNull();
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
        assertMessageNotNull();
        assertThat(message.getValidityTime()).as("validityTime").isEmpty();
        return this;
    }

    public GenericMessageAssertion hasLocationIndicator(final LocationIndicatorType type, final String value) {
        return hasLocationIndicators(Collections.singletonMap(type, value));
    }

    public GenericMessageAssertion hasLocationIndicators(final Map<LocationIndicatorType, String> expected) {
        assertMessageNotNull();
        assertThat(message.getLocationIndicators()).as("locationIndicators").isEqualTo(expected);
        return this;
    }

    public GenericAviationWeatherMessage getMessage() {
        return message;
    }

    public ConversionResult<GenericAviationWeatherMessage> getResult() {
        return result;
    }
}
