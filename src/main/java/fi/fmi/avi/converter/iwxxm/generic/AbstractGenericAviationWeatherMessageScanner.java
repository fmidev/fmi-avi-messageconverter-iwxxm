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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Version-agnostic IWXXM generic base for field-centric scanning using a message-type-specific FieldXPathProvider.
 */
public abstract class AbstractGenericAviationWeatherMessageScanner implements GenericAviationWeatherMessageScanner {

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

    protected Optional<ConversionIssue> collectReportStatus(final Element element, final XPath xpath,
                                                            final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final Optional<AviationWeatherMessage.ReportStatus> reportStatus =
                evaluateEnumeration(element, xpath, "@reportStatus", AviationWeatherMessage.ReportStatus.class);
        if (reportStatus.isPresent()) {
            builder.setReportStatus(reportStatus.get());
            return Optional.empty();
        } else {
            return Optional.of(new ConversionIssue(ConversionIssue.Severity.ERROR,
                    "The report status could not be parsed"));
        }
    }

    protected void collectIssueTimeUsingFieldProvider(final Element element,
                                                      final XPath xpath,
                                                      final GenericAviationWeatherMessageImpl.Builder builder,
                                                      final IssueList issues) throws XPathExpressionException {
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
    }

    protected void collectLocationIndicatorsUsingFieldProvider(
            final Element featureElement,
            final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder,
            final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType,
            final IssueList issues) throws XPathExpressionException {

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
    }

    protected void collectValidityTimeUsingFieldProvider(final Element element,
                                                         final XPath xpath,
                                                         final GenericAviationWeatherMessageImpl.Builder builder,
                                                         final IssueList issues) {
        for (final String expression : fieldXPathProvider.getXPaths(IWXXMField.VALID_TIME)) {
            final IssueList localIssues = collectValidTime(element, expression, xpath, builder);
            issues.addAll(localIssues);
            if (builder.getValidityTime().isPresent()) {
                return;
            }
        }
    }
}
