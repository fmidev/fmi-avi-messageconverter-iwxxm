package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * A configurable, version-agnostic generic IWXXM scanner that can be configured
 * declaratively for any message type.
 * <p>
 * Configuration options:
 * <ul>
 *   <li>A message type resolver (fixed or dynamic based on element name)</li>
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
 *         .messageType(MessageType.TAF)
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
    private static final Map<String, AviationWeatherMessage.ReportStatus> IWXXM_21_STATUS_TO_REPORT_STATUS;

    static {
        final Map<String, AviationWeatherMessage.ReportStatus> map = new HashMap<>();
        map.put("NORMAL", AviationWeatherMessage.ReportStatus.NORMAL);
        map.put("CORRECTION", AviationWeatherMessage.ReportStatus.CORRECTION);
        map.put("AMENDMENT", AviationWeatherMessage.ReportStatus.AMENDMENT);
        map.put("MISSING", AviationWeatherMessage.ReportStatus.NORMAL);
        map.put("CANCELLATION", AviationWeatherMessage.ReportStatus.AMENDMENT);
        IWXXM_21_STATUS_TO_REPORT_STATUS = Collections.unmodifiableMap(map);
    }

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

    public static Builder builder() {
        return new Builder();
    }

    private static String nullToEmpty(final String nullableString) {
        return nullableString == null ? "" : nullableString;
    }

    private static Optional<ZonedDateTime> evaluateZonedDateTime(final Element element,
                                                                 final XPath xPath,
                                                                 final String expression) throws XPathExpressionException {
        final String value = xPath.compile(expression).evaluate(element);
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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

    private static IssueList collectValidTime(final Element featureElement,
                                              final String selector,
                                              final XPath xpath,
                                              final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList issues = new IssueList();
        try {
            final NodeList results = (NodeList) xpath.compile(selector)
                    .evaluate(featureElement, XPathConstants.NODESET);
            if (results.getLength() == 1) {
                final Element validTimeElement = (Element) results.item(0);

                final Optional<ZonedDateTime> startTime = evaluateZonedDateTime(validTimeElement, xpath,
                        XPathBuilder.text("./gml:TimePeriod/gml:beginPosition"));
                final Optional<ZonedDateTime> endTime = evaluateZonedDateTime(validTimeElement, xpath,
                        XPathBuilder.text("./gml:TimePeriod/gml:endPosition"));

                if (startTime.isPresent() && endTime.isPresent()) {
                    builder.setValidityTime(PartialOrCompleteTimePeriod.builder()
                            .setStartTime(PartialOrCompleteTimeInstant.of(startTime.get()))
                            .setEndTime(PartialOrCompleteTimeInstant.of(endTime.get()))
                            .build());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                            ConversionIssue.Type.MISSING_DATA,
                            "Unable to parse valid time for " + featureElement.getLocalName()));
                }
            }
        } catch (final XPathExpressionException | DateTimeParseException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.MISSING_DATA,
                    "Unable to parse valid time for " + featureElement.getLocalName(), exception));
        }
        return issues;
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList issues = new IssueList();

        collectReportStatus(featureElement, xpath, builder, issues, requireReportStatus);
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        if (extractObservationTime) {
            collectObservationTimeUsingFieldProvider(featureElement, xpath, builder, issues);
        }

        if (extractValidityTime) {
            collectValidityTimeUsingFieldProvider(featureElement, xpath, builder, issues);
        }

        if (!locationIndicatorFields.isEmpty()) {
            collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, locationIndicatorFields, issues);
        }

        return issues;
    }

    private void collectIssueTimeUsingFieldProvider(final Element element,
                                                    final XPath xpath,
                                                    final GenericAviationWeatherMessageImpl.Builder builder,
                                                    final IssueList issues) {
        try {
            for (final String expression : fieldXPathProvider.getXPaths(IWXXMField.ISSUE_TIME)) {
                final Optional<ZonedDateTime> time = evaluateZonedDateTime(element, xpath, expression);
                if (time.isPresent()) {
                    builder.setIssueTime(PartialOrCompleteTimeInstant.of(time.get()));
                    return;
                }
            }
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.MISSING_DATA,
                    "No issue time found for IWXXM message"));
        } catch (final XPathExpressionException | DateTimeParseException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse issue time", exception));
        }
    }

    private void collectObservationTimeUsingFieldProvider(final Element element,
                                                          final XPath xpath,
                                                          final GenericAviationWeatherMessageImpl.Builder builder,
                                                          final IssueList issues) {
        try {
            for (final String expression : fieldXPathProvider.getXPaths(IWXXMField.OBSERVATION_TIME)) {
                final Optional<ZonedDateTime> time = evaluateZonedDateTime(element, xpath, expression);
                if (time.isPresent()) {
                    builder.setObservationTime(PartialOrCompleteTimeInstant.of(time.get()));
                    return;
                }
            }
        } catch (final XPathExpressionException | DateTimeParseException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse observation time", exception));
        }
    }

    private void collectLocationIndicatorsUsingFieldProvider(
            final Element featureElement,
            final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder,
            final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType,
            final IssueList issues) {

        try {
            final XPathExpression designatorExpression = xpath.compile(XPathBuilder.text("./aixm:designator"));
            final XPathExpression nameExpression = xpath.compile(XPathBuilder.text("./aixm:name"));

            for (final Map.Entry<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> entry
                    : fieldByLocationType.entrySet()) {
                final GenericAviationWeatherMessage.LocationIndicatorType locationIndicatorType = entry.getKey();
                final IWXXMField field = entry.getValue();

                String locationIndicator = "";
                for (final String expression : fieldXPathProvider.getXPaths(field)) {
                    final Object locationNode = xpath.compile(expression).evaluate(featureElement, XPathConstants.NODE);
                    if (locationNode instanceof Element) {
                        locationIndicator = nullToEmpty(designatorExpression.evaluate(locationNode));
                        if (locationIndicator.isEmpty()) {
                            locationIndicator = nullToEmpty(nameExpression.evaluate(locationNode));
                        }
                        if (!locationIndicator.isEmpty()) {
                            break;
                        }
                    }
                }

                if (locationIndicator.isEmpty()) {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            String.format(Locale.ROOT, "Missing location indicator %s", locationIndicatorType)));
                } else {
                    builder.putLocationIndicators(locationIndicatorType, locationIndicator);
                }
            }
        } catch (final XPathExpressionException exception) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse location indicators", exception));
        }
    }

    private void collectValidityTimeUsingFieldProvider(final Element element,
                                                       final XPath xpath,
                                                       final GenericAviationWeatherMessageImpl.Builder builder,
                                                       final IssueList issues) {
        final IssueList accumulatedIssues = new IssueList();
        for (final String expression : fieldXPathProvider.getXPaths(IWXXMField.VALID_TIME)) {
            final IssueList validTimeIssues = collectValidTime(element, expression, xpath, builder);
            if (builder.getValidityTime().isPresent()) {
                return;
            }
            accumulatedIssues.addAll(validTimeIssues);
        }
        issues.addAll(accumulatedIssues);
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

