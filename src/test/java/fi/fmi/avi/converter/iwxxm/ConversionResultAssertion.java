package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluent assertion helper for {@link ConversionResult} in tests.
 *
 * <p>Example usage for successful conversion with result extraction:</p>
 * <pre>{@code
 * TAF taf = assertConversionResult(result).successfullyConverted();
 * }</pre>
 */
public final class ConversionResultAssertion<T> {

    private final ConversionResult<T> result;

    private ConversionResultAssertion(final ConversionResult<T> result) {
        assertThat(result).as("ConversionResult").isNotNull();
        this.result = result;
    }

    public static <T> ConversionResultAssertion<T> assertThatConversionResult(final ConversionResult<T> result) {
        return new ConversionResultAssertion<>(result);
    }

    public T getMessage() {
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        return result.getConvertedMessage().get();
    }

    public ConversionResultAssertion<T> isSuccessful() {
        assertThat(result.getConversionIssues()).as("conversionIssues").isEmpty();
        assertThat(result.getStatus()).as("status").isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).as("convertedMessage").isPresent();
        return this;
    }

    public ConversionResultAssertion<T> hasIssue(final ConversionIssue.Severity severity, final ConversionIssue.Type type) {
        assertThat(result.getConversionIssues())
                .as("conversionIssues with severity " + severity + " and type " + type)
                .anyMatch(issue -> issue.getSeverity() == severity && issue.getType() == type);
        return this;
    }

    public ConversionResultAssertion<T> hasIssue(final ConversionIssue.Severity severity,
                                                 final ConversionIssue.Type type,
                                                 final String messageSubstring) {
        assertThat(result.getConversionIssues())
                .as("conversionIssues with severity " + severity + ", type " + type + ", containing '" + messageSubstring + "'")
                .anyMatch(issue -> issue.getSeverity() == severity
                        && issue.getType() == type
                        && issue.getMessage() != null
                        && issue.getMessage().contains(messageSubstring));
        return this;
    }

    public ConversionResultAssertion<T> hasIssueContaining(final String messageSubstring) {
        assertThat(result.getConversionIssues())
                .as("conversionIssues containing '" + messageSubstring + "'")
                .anyMatch(issue -> issue.getMessage() != null && issue.getMessage().contains(messageSubstring));
        return this;
    }

    public ConversionResultAssertion<T> hasNoIssueContaining(final String messageSubstring) {
        assertThat(result.getConversionIssues())
                .as("conversionIssues not containing '" + messageSubstring + "'")
                .noneMatch(issue -> issue.getMessage() != null && issue.getMessage().contains(messageSubstring));
        return this;
    }

    public ConversionResultAssertion<T> hasXmlMessageEqualTo(final String expectedXml) throws SAXException, IOException {
        assertThat(result.getConvertedMessage()).as("convertedMessage").containsInstanceOf(String.class);
        IWXXMConverterTests.assertXMLEqualsIgnoringVariables(expectedXml, (String) result.getConvertedMessage().get());
        return this;
    }

    public ConversionResultAssertion<T> hasStatus(final ConversionResult.Status expectedStatus) {
        assertThat(result.getStatus()).as("status").isEqualTo(expectedStatus);
        return this;
    }
}
