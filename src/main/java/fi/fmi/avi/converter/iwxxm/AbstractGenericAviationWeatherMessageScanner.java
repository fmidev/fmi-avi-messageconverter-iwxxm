package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class AbstractGenericAviationWeatherMessageScanner implements GenericAviationWeatherMessageScanner {

    protected static IssueList collectValidTime(final Element featureElement, final String selector, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList retval = new IssueList();
        try {
            ZonedDateTime startTime = null, endTime = null;
            final XPathExpression expr = xpath.compile(selector);
            final NodeList results = (NodeList) expr.evaluate(featureElement, XPathConstants.NODESET);
            if (results.getLength() == 1) {
                final Element validTimeElement = (Element) results.item(0);
                startTime = parseStartTime(validTimeElement, xpath);
                endTime = parseEndTime(validTimeElement, xpath);
            }
            if (startTime != null && endTime != null) {
                builder.setValidityTime(PartialOrCompleteTimePeriod.builder()//
                        .setStartTime(PartialOrCompleteTimeInstant.of(startTime))//
                        .setEndTime(PartialOrCompleteTimeInstant.of(endTime))//
                        .build());
            }
        } catch (final Exception ex) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Unable to parse valid time for TAF", ex);
        }
        return retval;
    }

    protected static void parseAerodromeDesignator(final Element featureElement, final XPathExpression timeSliceExpretion, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues, final String status) throws XPathExpressionException {
        final NodeList nodes = (NodeList) timeSliceExpretion.evaluate(featureElement, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
            XPathExpression expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:designator");
            final String designator = expr.evaluate(nodes.item(0));

            if (designator == null || designator.isEmpty()) {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No aerodrome designator in AirportHeliportTimeSlice");
            } else {
                builder.putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, designator);
            }

        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Aerodrome info not available for TAF of status " + status);
        }
    }

    protected static ZonedDateTime parseStartTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
        return parseTimeInstant(timeElement, xpath, "./gml:TimePeriod/gml:beginPosition", "./gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition");
    }

    protected static ZonedDateTime parseEndTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
        return parseTimeInstant(timeElement, xpath, "./gml:TimePeriod/gml:endPosition", "./gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition");
    }

    private static ZonedDateTime parseTimeInstant(final Element timeElement, final XPath xpath, String checkTimePositionExpression,
            String timePositionExpression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(checkTimePositionExpression);
        String timeStr = expr.evaluate(timeElement);
        if (timeStr.isEmpty()) {
            expr = xpath.compile(timePositionExpression);
            timeStr = expr.evaluate(timeElement);
        }
        if (!timeStr.isEmpty()) {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            throw new IllegalArgumentException("No valid time begin found from element " + timeElement.getTagName());
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

}
