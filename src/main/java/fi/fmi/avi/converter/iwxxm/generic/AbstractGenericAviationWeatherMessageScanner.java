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
import java.util.*;

/**
 * Version-agnostic IWXXM generic base for field-centric scanning using a message-type-specific FieldXPathProvider.
 */
public abstract class AbstractGenericAviationWeatherMessageScanner implements GenericAviationWeatherMessageScanner {

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

    protected AbstractGenericAviationWeatherMessageScanner(final FieldXPathProvider fieldXPathProvider) {
        this.fieldXPathProvider = Objects.requireNonNull(fieldXPathProvider, "fieldXPathProvider");
    }


    protected static String nullToEmpty(final String nullableString) {
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

    protected static <T extends Enum<T>> Optional<T> evaluateEnumeration(final Element element,
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
     * @param element the message element
     * @param xpath   XPath instance
     * @param builder the builder to set report status on
     * @param issues  list to add conversion issues to
     */
    protected static void collectReportStatus(final Element element,
                                              final XPath xpath,
                                              final GenericAviationWeatherMessageImpl.Builder builder,
                                              final IssueList issues) {
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

            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.MISSING_DATA,
                    "The report status could not be parsed"));
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse report status", ex));
        }
    }

    /**
     * Collects report status from IWXXM message where status is optional (e.g., VAA, TCA in IWXXM 2.1).
     * Tries @reportStatus (3.0+) first, then @status (2.1). No error is added if status is missing.
     *
     * @param element the message element
     * @param xpath   XPath instance
     * @param builder the builder to set report status on
     * @param issues  list to add conversion issues to
     */
    protected static void collectOptionalReportStatus(final Element element,
                                                      final XPath xpath,
                                                      final GenericAviationWeatherMessageImpl.Builder builder,
                                                      final IssueList issues) {
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
                }
            }
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse report status", ex));
        }
    }

    protected static IssueList collectValidTime(final Element featureElement,
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
                        "./gml:TimePeriod/gml:beginPosition");
                final Optional<ZonedDateTime> endTime = evaluateZonedDateTime(validTimeElement, xpath,
                        "./gml:TimePeriod/gml:endPosition");

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
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.MISSING_DATA,
                    "Unable to parse valid time for " + featureElement.getLocalName(), ex));
        }
        return issues;
    }


    protected void collectIssueTimeUsingFieldProvider(final Element element,
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
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse issue time", ex));
        }
    }

    protected void collectObservationTimeUsingFieldProvider(final Element element,
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
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse observation time", ex));
        }
    }

    protected void collectLocationIndicatorsUsingFieldProvider(
            final Element featureElement,
            final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder,
            final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType,
            final IssueList issues) {

        try {
            final XPathExpression designatorExpression = xpath.compile("./aixm:designator");
            final XPathExpression nameExpression = xpath.compile("./aixm:name");

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
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse location indicators", ex));
        }
    }

    protected void collectValidityTimeUsingFieldProvider(final Element element,
                                                         final XPath xpath,
                                                         final GenericAviationWeatherMessageImpl.Builder builder,
                                                         final IssueList issues) {
        try {
            for (final String expression : fieldXPathProvider.getXPaths(IWXXMField.VALID_TIME)) {
                final IssueList validTimeIssues = collectValidTime(element, expression, xpath, builder);
                if (builder.getValidityTime().isPresent()) {
                    return;
                }
                issues.addAll(validTimeIssues);
            }
        } catch (final Exception ex) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    ConversionIssue.Type.OTHER,
                    "Unable to parse validity time", ex));
        }
    }
}
