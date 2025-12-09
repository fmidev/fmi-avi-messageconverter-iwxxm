package fi.fmi.avi.converter.iwxxm.generic;

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
 * <pre>
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
 * </pre>
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

    private void assertMessageNotNull() {
        assertThat(message).as("message").isNotNull();
    }

    /**
     * Creates a new assertion for the given conversion result.
     *
     * @param result the conversion result to assert on
     * @return a new fluent assertion instance
     */
    public static GenericMessageAssertion assertMessage(final ConversionResult<GenericAviationWeatherMessage> result) {
        assertThat(result).as("ConversionResult").isNotNull();
        return new GenericMessageAssertion(result);
    }

    /**
     * Creates a new assertion for the first message in a bulletin conversion result.
     *
     * @param result the bulletin conversion result
     * @return a new fluent assertion instance for the first message
     */
    public static GenericMessageAssertion assertFirstMessage(final ConversionResult<GenericMeteorologicalBulletin> result) {
        assertThat(result).as("ConversionResult").isNotNull();
        assertThat(result.getConversionIssues()).as("conversionIssues").isEmpty();
        assertThat(result.getStatus()).as("status").isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        assertThat(result.getConvertedMessage().get().getMessages()).as("messages").isNotEmpty();
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);
        return new GenericMessageAssertion(message);
    }

    /**
     * Asserts that the conversion had no issues and was successful.
     */
    public GenericMessageAssertion hasNoIssues() {
        assertThat(result.getConversionIssues()).as("conversionIssues").isEmpty();
        assertThat(result.getStatus()).as("status").isEqualTo(ConversionResult.Status.SUCCESS);
        return this;
    }

    /**
     * Asserts that the converted message is present.
     */
    public GenericMessageAssertion isPresent() {
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        return this;
    }

    /**
     * Asserts the message format.
     */
    public GenericMessageAssertion hasFormat(final Format expected) {
        assertMessageNotNull();
        assertThat(message.getMessageFormat()).as("messageFormat").isEqualTo(expected);
        return this;
    }

    /**
     * Asserts the XML namespace.
     */
    public GenericMessageAssertion hasNamespace(final String expected) {
        assertMessageNotNull();
        assertThat(message.getXMLNamespace()).as("xmlNamespace").hasValue(expected);
        return this;
    }

    /**
     * Asserts the message type.
     */
    public GenericMessageAssertion hasMessageType(final MessageType expected) {
        assertMessageNotNull();
        assertThat(message.getMessageType()).as("messageType").hasValue(expected);
        return this;
    }

    /**
     * Asserts that the message is translated.
     */
    public GenericMessageAssertion isTranslated() {
        assertMessageNotNull();
        assertThat(message.isTranslated()).as("translated").isTrue();
        return this;
    }

    /**
     * Asserts that the message is not translated.
     */
    public GenericMessageAssertion isNotTranslated() {
        assertMessageNotNull();
        assertThat(message.isTranslated()).as("translated").isFalse();
        return this;
    }

    /**
     * Asserts the report status.
     */
    public GenericMessageAssertion hasReportStatus(final ReportStatus expected) {
        assertMessageNotNull();
        assertThat(message.getReportStatus()).as("reportStatus").isEqualTo(expected);
        return this;
    }

    /**
     * Asserts the issue time.
     *
     * @param expected ISO-8601 formatted time string (e.g., "2017-07-30T11:30Z")
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
     * Asserts the validity period.
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

    /**
     * Asserts that there is no validity period.
     */
    public GenericMessageAssertion hasNoValidityPeriod() {
        assertMessageNotNull();
        assertThat(message.getValidityTime()).as("validityTime").isEmpty();
        return this;
    }

    /**
     * Asserts a single location indicator.
     */
    public GenericMessageAssertion hasLocationIndicator(final LocationIndicatorType type, final String value) {
        return hasLocationIndicators(Collections.singletonMap(type, value));
    }

    /**
     * Asserts all location indicators.
     */
    public GenericMessageAssertion hasLocationIndicators(final Map<LocationIndicatorType, String> expected) {
        assertMessageNotNull();
        assertThat(message.getLocationIndicators()).as("locationIndicators").isEqualTo(expected);
        return this;
    }

    /**
     * Returns the message for additional custom assertions.
     */
    public GenericAviationWeatherMessage getMessage() {
        return message;
    }

    /**
     * Returns the conversion result for additional custom assertions.
     */
    public ConversionResult<GenericAviationWeatherMessage> getResult() {
        return result;
    }
}
