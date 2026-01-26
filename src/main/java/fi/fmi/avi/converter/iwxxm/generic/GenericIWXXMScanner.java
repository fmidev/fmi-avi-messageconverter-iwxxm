package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A configurable, version-agnostic generic IWXXM scanner that can be configured
 * declaratively for any message type.
 * <p>
 * Configuration options:
 * <ul>
 *   <li>Whether report status is required or optional</li>
 *   <li>Whether to extract validity time</li>
 *   <li>Which location indicators to extract</li>
 *   <li>A field XPath provider for the message type</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * GenericIWXXMScanner tafScanner = GenericIWXXMScanner.builder()
 *         .fieldProvider(new TAFFieldXPathProvider())
 *         .requireReportStatus(true)
 *         .extractValidityTime(true)
 *         .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
 *         .build();
 * </pre>
 */
public class GenericIWXXMScanner implements GenericAviationWeatherMessageScanner {

    /**
     * Maps IWXXM 2.1 @status attribute values to ReportStatus.
     */
    private static final Map<String, AviationWeatherMessage.ReportStatus> IWXXM_21_STATUS_TO_REPORT_STATUS =
            createIwxxm21StatusToReportStatus();

    private final FieldXPathProvider fieldXPathProvider;
    private final boolean requireReportStatus;
    private final boolean extractValidityTime;
    private final boolean extractObservationTime;
    private final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> locationIndicatorFields;

    private GenericIWXXMScanner(final Builder builder) {
        this.fieldXPathProvider = Objects.requireNonNull(builder.fieldXPathProvider, "fieldXPathProvider");
        this.requireReportStatus = builder.requireReportStatus;
        this.extractValidityTime = builder.extractValidityTime;
        this.extractObservationTime = builder.extractObservationTime;
        this.locationIndicatorFields = Collections.unmodifiableMap(
                new EnumMap<>(builder.locationIndicatorFields));
    }

    private static Map<String, AviationWeatherMessage.ReportStatus> createIwxxm21StatusToReportStatus() {
        final Map<String, AviationWeatherMessage.ReportStatus> map = new HashMap<>();
        map.put("NORMAL", AviationWeatherMessage.ReportStatus.NORMAL);
        map.put("CORRECTION", AviationWeatherMessage.ReportStatus.CORRECTION);
        map.put("AMENDMENT", AviationWeatherMessage.ReportStatus.AMENDMENT);
        map.put("MISSING", AviationWeatherMessage.ReportStatus.NORMAL);
        map.put("CANCELLATION", AviationWeatherMessage.ReportStatus.AMENDMENT);
        return Collections.unmodifiableMap(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String nullToEmpty(final String nullableString) {
        return nullableString == null ? "" : nullableString;
    }

    private static XPathEvaluationResult<ZonedDateTime> evaluateZonedDateTime(final Element element,
                                                                              final XPath xPath,
                                                                              final String expression) {
        try {
            final String value = xPath.compile(expression).evaluate(element);
            if (value == null || value.isEmpty()) {
                return XPathEvaluationResult.empty();
            }
            return XPathEvaluationResult.of(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (final XPathExpressionException | DateTimeParseException exception) {
            return XPathEvaluationResult.fail(exception);
        }
    }

    private static <T extends Enum<T>> Optional<T> evaluateEnumeration(final Element element,
                                                                       final XPath xpath,
                                                                       final String expression,
                                                                       final Class<T> enumType)
            throws XPathExpressionException {
        final String value = xpath.compile(expression).evaluate(element);
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(enumType, value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Collects report status from IWXXM message, trying @reportStatus (3.0+) first,
     * then falling back to @status (2.1) with direct string mapping.
     *
     * @param element  the IWXXM root element
     * @param xpath    the XPath evaluator
     * @param builder  the message builder to populate
     * @param issues   the issue list to add errors to
     * @param required if true, adds an error when report status cannot be parsed;
     *                 if false, missing status is silently ignored (for VAA/TCA in IWXXM 2.1)
     */
    private static void collectReportStatus(final Element element,
                                            final XPath xpath,
                                            final GenericAviationWeatherMessageImpl.Builder builder,
                                            final IssueList issues,
                                            final boolean required) {
        try {
            // Try IWXXM 3.0+ @reportStatus first
            final Optional<AviationWeatherMessage.ReportStatus> reportStatus =
                    evaluateEnumeration(element, xpath, "@reportStatus", AviationWeatherMessage.ReportStatus.class);
            if (reportStatus.isPresent()) {
                builder.setReportStatus(reportStatus.get());
                return;
            }

            // Fallback to IWXXM 2.1 @status with direct string mapping
            final String legacyStatus = xpath.compile("@status").evaluate(element);
            if (legacyStatus != null && !legacyStatus.isEmpty()) {
                final AviationWeatherMessage.ReportStatus mappedStatus = IWXXM_21_STATUS_TO_REPORT_STATUS.get(legacyStatus);
                if (mappedStatus != null) {
                    builder.setReportStatus(mappedStatus);
                    return;
                }
            }

            if (required) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                        ConversionIssue.Type.MISSING_DATA,
                        "The report status could not be parsed"));
            }
        } catch (final XPathExpressionException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse report status", exception));
        }
    }

    private static XPathEvaluationResult<PartialOrCompleteTimePeriod> collectValidTime(final Element featureElement,
                                                                                       final String selector,
                                                                                       final XPath xpath) {
        try {
            final Object result = xpath.compile(selector).evaluate(featureElement, XPathConstants.NODE);
            if (!(result instanceof Element)) {
                return XPathEvaluationResult.empty();
            }
            final Element validTimeElement = (Element) result;

            final XPathEvaluationResult<ZonedDateTime> startTime = evaluateZonedDateTime(validTimeElement, xpath,
                    XPathBuilder.text("./gml:TimePeriod/gml:beginPosition"));
            final XPathEvaluationResult<ZonedDateTime> endTime = evaluateZonedDateTime(validTimeElement, xpath,
                    XPathBuilder.text("./gml:TimePeriod/gml:endPosition"));

            if (startTime.hasValue() && endTime.hasValue()) {
                return XPathEvaluationResult.of(PartialOrCompleteTimePeriod.builder()
                        .setStartTime(PartialOrCompleteTimeInstant.of(startTime.getOrThrow()))
                        .setEndTime(PartialOrCompleteTimeInstant.of(endTime.getOrThrow()))
                        .build());
            } else if (startTime.isFailed()) {
                return XPathEvaluationResult.fail(startTime.getException().orElse(null));
            } else if (endTime.isFailed()) {
                return XPathEvaluationResult.fail(endTime.getException().orElse(null));
            }
            return XPathEvaluationResult.empty();
        } catch (final XPathExpressionException exception) {
            return XPathEvaluationResult.fail(exception);
        }
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList issues = new IssueList();

        collectReportStatus(featureElement, xpath, builder, issues, requireReportStatus);
        collectNilStatus(featureElement, xpath, builder, issues);
        collectTimeInstantUsingFieldProvider(featureElement, xpath, issues,
                IWXXMField.ISSUE_TIME, "issue time", builder::setIssueTime);

        if (extractObservationTime) {
            collectTimeInstantUsingFieldProvider(featureElement, xpath, issues,
                    IWXXMField.OBSERVATION_TIME, "observation time", builder::setObservationTime);
        }

        if (extractValidityTime) {
            collectValidityTimeUsingFieldProvider(featureElement, xpath, builder, issues);
        }

        if (!locationIndicatorFields.isEmpty()) {
            collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, locationIndicatorFields, issues);
        }

        return issues;
    }

    private void collectTimeInstantUsingFieldProvider(final Element element,
                                                      final XPath xpath,
                                                      final IssueList issues,
                                                      final IWXXMField field,
                                                      final String fieldName,
                                                      final Consumer<PartialOrCompleteTimeInstant> setter) {
        final XPathEvaluationResult<ZonedDateTime> result = evaluate(field,
                expr -> evaluateZonedDateTime(element, xpath, expr));
        if (result.hasValue()) {
            setter.accept(PartialOrCompleteTimeInstant.of(result.getOrThrow()));
        } else if (result.isFailed()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse " + fieldName, result.getException().orElse(null)));
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.MISSING_DATA,
                    "No " + fieldName + " found for IWXXM message"));
        }
    }

    private void collectLocationIndicatorsUsingFieldProvider(
            final Element featureElement,
            final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder,
            final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType,
            final IssueList issues) {

        final XPathExpression designatorExpression;
        final XPathExpression nameExpression;
        try {
            designatorExpression = xpath.compile(XPathBuilder.text("./aixm:designator"));
            nameExpression = xpath.compile(XPathBuilder.text("./aixm:name"));
        } catch (final XPathExpressionException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to compile location indicator expressions", exception));
            return;
        }

        for (final Map.Entry<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> entry
                : fieldByLocationType.entrySet()) {
            final GenericAviationWeatherMessage.LocationIndicatorType locationIndicatorType = entry.getKey();
            final IWXXMField field = entry.getValue();

            final XPathEvaluationResult<String> result = evaluate(field,
                    expr -> evaluateLocationIndicator(featureElement, expr, xpath, designatorExpression, nameExpression));

            if (result.hasValue()) {
                builder.putLocationIndicators(locationIndicatorType, result.getOrThrow());
            } else if (result.isFailed()) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                        ConversionIssue.Type.OTHER,
                        "Unable to parse location indicator " + locationIndicatorType, result.getException().orElse(null)));
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        String.format(Locale.ROOT, "Missing location indicator %s", locationIndicatorType)));
            }
        }
    }

    private XPathEvaluationResult<String> evaluateLocationIndicator(final Element featureElement,
                                                                    final String expression,
                                                                    final XPath xpath,
                                                                    final XPathExpression designatorExpression,
                                                                    final XPathExpression nameExpression) {
        try {
            final Object locationNode = xpath.compile(expression).evaluate(featureElement, XPathConstants.NODE);
            if (locationNode instanceof Element) {
                String locationIndicator = nullToEmpty(designatorExpression.evaluate(locationNode));
                if (locationIndicator.isEmpty()) {
                    locationIndicator = nullToEmpty(nameExpression.evaluate(locationNode));
                }
                if (!locationIndicator.isEmpty()) {
                    return XPathEvaluationResult.of(locationIndicator);
                }
            }
            return XPathEvaluationResult.empty();
        } catch (final XPathExpressionException exception) {
            return XPathEvaluationResult.fail(exception);
        }
    }

    private void collectValidityTimeUsingFieldProvider(final Element element,
                                                       final XPath xpath,
                                                       final GenericAviationWeatherMessageImpl.Builder builder,
                                                       final IssueList issues) {
        final XPathEvaluationResult<PartialOrCompleteTimePeriod> result = evaluate(IWXXMField.VALID_TIME,
                expr -> collectValidTime(element, expr, xpath));
        if (result.hasValue()) {
            builder.setValidityTime(result.getOrThrow());
        } else if (result.isFailed()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse validity time", result.getException().orElse(null)));
        }
    }

    private void collectNilStatus(final Element element,
                                  final XPath xpath,
                                  final GenericAviationWeatherMessageImpl.Builder builder,
                                  final IssueList issues) {
        final XPathEvaluationResult<String> result = evaluate(IWXXMField.NIL_REASON, expr -> {
            try {
                final String value = xpath.compile(expr).evaluate(element);
                if (value != null && !value.isEmpty()) {
                    return XPathEvaluationResult.of(value);
                }
                return XPathEvaluationResult.empty();
            } catch (final XPathExpressionException exception) {
                return XPathEvaluationResult.fail(exception);
            }
        });
        if (result.hasValue()) {
            builder.setNil(true);
        } else if (result.isFailed()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse NIL status", result.getException().orElse(null)));
        }
    }

    private <T> XPathEvaluationResult<T> evaluate(final IWXXMField field,
                                                  final Function<String, XPathEvaluationResult<T>> evaluator) {
        Exception failure = null;
        for (final String expression : fieldXPathProvider.getXPaths(field)) {
            final XPathEvaluationResult<T> result = evaluator.apply(expression);
            if (result.hasValue()) {
                return result;
            }
            if (failure == null) {
                failure = result.getException().orElse(null);
            } else {
                result.getException().ifPresent(failure::addSuppressed);
            }
        }
        return failure == null ? XPathEvaluationResult.empty() : XPathEvaluationResult.fail(failure);
    }

    public static class Builder {
        private final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> locationIndicatorFields =
                new EnumMap<>(GenericAviationWeatherMessage.LocationIndicatorType.class);
        private FieldXPathProvider fieldXPathProvider;
        private boolean requireReportStatus = true;
        private boolean extractValidityTime = false;
        private boolean extractObservationTime = false;

        private Builder() {
        }

        /**
         * Sets the field XPath provider for this scanner.
         * This is required.
         *
         * @param provider the field XPath provider
         * @return this builder
         */
        public Builder fieldProvider(final FieldXPathProvider provider) {
            this.fieldXPathProvider = Objects.requireNonNull(provider, "fieldXPathProvider");
            return this;
        }

        /**
         * Sets whether report status is required.
         * If true (default), an error is added when report status cannot be parsed.
         * If false, missing report status is silently ignored (for VAA/TCA in IWXXM 2.1).
         *
         * @param required whether report status is required
         * @return this builder
         */
        public Builder requireReportStatus(final boolean required) {
            this.requireReportStatus = required;
            return this;
        }

        /**
         * Sets whether to extract validity time.
         * Default is false.
         *
         * @param extract whether to extract validity time
         * @return this builder
         */
        public Builder extractValidityTime(final boolean extract) {
            this.extractValidityTime = extract;
            return this;
        }

        /**
         * Sets whether to extract observation time.
         * Default is false. Should be enabled for METAR/SPECI messages.
         *
         * @param extract whether to extract observation time
         * @return this builder
         */
        public Builder extractObservationTime(final boolean extract) {
            this.extractObservationTime = extract;
            return this;
        }

        /**
         * Adds a location indicator to extract.
         *
         * @param type  the location indicator type
         * @param field the IWXXM field that provides the XPath for this location indicator
         * @return this builder
         */
        public Builder locationIndicator(final GenericAviationWeatherMessage.LocationIndicatorType type,
                                         final IWXXMField field) {
            this.locationIndicatorFields.put(
                    Objects.requireNonNull(type, "type"),
                    Objects.requireNonNull(field, "field"));
            return this;
        }

        /**
         * Builds the configured GenericIWXXMScanner.
         *
         * @return a new GenericIWXXMScanner instance
         * @throws NullPointerException if required fields are not set
         */
        public GenericIWXXMScanner build() {
            Objects.requireNonNull(fieldXPathProvider, "fieldProvider must be set");
            return new GenericIWXXMScanner(this);
        }
    }
}

