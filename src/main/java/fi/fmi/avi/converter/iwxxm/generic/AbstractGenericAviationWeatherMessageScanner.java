package fi.fmi.avi.converter.iwxxm.generic;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class AbstractGenericAviationWeatherMessageScanner implements GenericAviationWeatherMessageScanner {

    protected static IssueList collectValidTime(final Element featureElement, final String selector, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList retval = new IssueList();
        try {
            final NodeList results = evaluateNodeSet(featureElement, xpath, selector);
            if (results.getLength() == 1) {
                final Element validTimeElement = (Element) results.item(0);
                final ZonedDateTime startTime = evaluateFirstSuccessfulZonedDateTime(validTimeElement, xpath, "./gml:TimePeriod/gml:beginPosition",
                        "./gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition").orElseThrow(
                        () -> new IllegalArgumentException("No valid time start found from element " + validTimeElement.getTagName()));
                final ZonedDateTime endTime = evaluateFirstSuccessfulZonedDateTime(validTimeElement, xpath, "./gml:TimePeriod/gml:endPosition",
                        "./gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition").orElseThrow(
                        () -> new IllegalArgumentException("No valid time end found from element " + validTimeElement.getTagName()));
                builder.setValidityTime(PartialOrCompleteTimePeriod.builder()
                        .setStartTime(PartialOrCompleteTimeInstant.of(startTime))
                        .setEndTime(PartialOrCompleteTimeInstant.of(endTime))
                        .build());
            }
        } catch (final Exception ex) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Unable to parse valid time for TAF", ex);
        }
        return retval;
    }

    protected static void parseAerodromeDesignator(final Element featureElement, final String timeSliceExpression, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues) throws XPathExpressionException {
        final NodeList nodes = evaluateNodeSet(featureElement, xpath, timeSliceExpression);
        if (nodes.getLength() == 1) {
            final Optional<String> designator = evaluateNonEmptyString((Element) nodes.item(0), xpath,
                    "./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm" + ":designator");

            if (designator.isPresent()) {
                builder.putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, designator.get());
            } else {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No aerodrome designator in AirportHeliportTimeSlice");
            }

        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                    "Aerodrome info not available for TAF of status " + builder.getReportStatus());
        }
    }

    protected static void collectLocationIndicators(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder,
            final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> xpathExpressions, final IssueList issues) throws XPathExpressionException {
        for (final Map.Entry<GenericAviationWeatherMessage.LocationIndicatorType, String> entry : xpathExpressions.entrySet()) {
            final String locationIndicator = xpath.compile(entry.getValue()).evaluate(featureElement);
            if (locationIndicator == null || locationIndicator.isEmpty()) {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        String.format(Locale.ROOT, "Missing location indicator %s", entry.getKey()));
            } else {
                builder.putLocationIndicators(entry.getKey(), locationIndicator);
            }
        }
    }

    protected static void collectIssueTime(XPath xpath, String expression, Element element, GenericAviationWeatherMessageImpl.Builder builder, IssueList issues)
            throws XPathExpressionException {
        Optional<ZonedDateTime> time = evaluateFirstSuccessfulZonedDateTime(element, xpath, expression);
        if (time.isPresent()) {
            builder.setIssueTime(PartialOrCompleteTimeInstant.of(time.get()));
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No issue time found for IWXXM message"));
        }
    }

    protected static Optional<String> evaluateNonEmptyString(final Element element, final XPath xpath, final String expression)
            throws XPathExpressionException {
        return evaluate(element, xpath, expression, str -> str.isEmpty() ? null : str);
    }

    protected static Optional<ZonedDateTime> evaluateFirstSuccessfulZonedDateTime(final Element element, final XPath xPath, final String... expressions)
            throws XPathExpressionException {
        return evaluateFirstSuccessful(element, xPath, str -> str.isEmpty() ? null : ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                expressions);
    }

    protected static <T> Optional<T> evaluateFirstSuccessful(final Element element, final XPath xPath, final Function<String, T> finisher,
            final String... expressions) throws XPathExpressionException {
        for (final String expression : expressions) {
            final Optional<T> result = evaluate(element, xPath, expression, finisher);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    protected static <T> Optional<T> evaluate(final Element element, final XPath xPath, final String expression, final Function<String, T> finisher)
            throws XPathExpressionException {
        return Optional.ofNullable(xPath.compile(expression).evaluate(element)).map(finisher);
    }

    protected static NodeList evaluateNodeSet(final Element element, final XPath xpath, final String expression) throws XPathExpressionException {
        return (NodeList) xpath.compile(expression).evaluate(element, XPathConstants.NODESET);
    }

    protected static <T extends Enum<T>> Optional<T> evaluateEnumeration(Element element, XPath xpath, String expression, Class<T> enumType) throws XPathExpressionException{
        return evaluate(element, xpath, expression, str -> {
            try {
                return Enum.valueOf(enumType, str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }
}
