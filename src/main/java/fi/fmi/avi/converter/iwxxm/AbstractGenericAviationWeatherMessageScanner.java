package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
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
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class AbstractGenericAviationWeatherMessageScanner implements GenericAviationWeatherMessageScanner{

    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> SIGMET_30_LOCATION_INDICATOR_EXPRESSIONS;
    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> SIGMET_21_LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> sigmet30LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm30:originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm30:issuingAirTrafficServicesUnit/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm30:issuingAirTrafficServicesRegion/aixm:Airspace/aixm:timeSlice/aixm:AirspaceTimeSlice/aixm:designator");
        SIGMET_30_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(sigmet30LocationIndicatorExpressions);

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> sigmet21LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm" + ":originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm:issuingAirTrafficServicesUnit/aixm" + ":Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm:analysis/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:Airspace/aixm"
                        + ":timeSlice/aixm:AirspaceTimeSlice/aixm:designator");
        SIGMET_21_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(sigmet21LocationIndicatorExpressions);
    }

    protected static IssueList collectValidTime(final Element featureElement, final String selector, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder) {
        final IssueList retval = new IssueList();
        //validity time
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

    protected static void parseAerodromeInfo(final Element featureElement, final XPathExpression timeSliceExpretion, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues, final String status) throws XPathExpressionException {
        final NodeList nodes = (NodeList) timeSliceExpretion.evaluate(featureElement, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
            Optional<Aerodrome> aerodrome = parseAerodromeInfo((Element) nodes.item(0), xpath, issues);
            if (aerodrome.isPresent()) {
                builder.putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, aerodrome.get().getDesignator());
            } else {
                issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Aerodrome info could not be parsed for TAF of status " + status);
            }
        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Aerodrome info not available for TAF of status " + status);
        }
    }

    protected static Optional<Aerodrome> parseAerodromeInfo(final Element airportHeliport, final XPath xpath, final IssueList issues)
            throws XPathExpressionException {
        Optional<Aerodrome> retval = Optional.empty();
        XPathExpression expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:designator");
        final String designator = expr.evaluate(airportHeliport);

        if (designator.isEmpty()) {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No aerodrome designator in AirportHeliportTimeSlice");
            return retval;
        }

        expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        final String locationIndicatorICAO = expr.evaluate(airportHeliport);

        expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:designatorIATA");
        final String designatorIATA = expr.evaluate(airportHeliport);

        expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:name");
        final String name = expr.evaluate(airportHeliport);

        //NOTE: the ARP field elevation of the Aerodrome info is intentionally not parsed here, it's currently not needed in the use cases,
        // and would require more than a few lines of code.

        retval = Optional.of(AerodromeImpl.builder()//
                .setDesignator(designator)//
                .setLocationIndicatorICAO(Optional.ofNullable(locationIndicatorICAO))//
                .setName(Optional.ofNullable(name))//
                .setDesignatorIATA(Optional.ofNullable(designatorIATA))//
                .build());

        return retval;
    }

    protected static ZonedDateTime parseStartTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
        XPathExpression expr = xpath.compile("./gml:TimePeriod/gml:beginPosition");
        String timeStr = expr.evaluate(timeElement);
        if (timeStr.isEmpty()) {
            //validity time, begin/TimeInstant variant:
            expr = xpath.compile("./gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition");
            timeStr = expr.evaluate(timeElement);
        }
        if (!timeStr.isEmpty()) {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            throw new IllegalArgumentException("No valid time begin found from element " + timeElement.getTagName());
        }
    }

    protected static ZonedDateTime parseEndTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
        XPathExpression expr = xpath.compile("./gml:TimePeriod/gml:endPosition");
        String timeStr = expr.evaluate(timeElement);
        if (timeStr.isEmpty()) {
            //validity time, begin/TimeInstant variant:
            expr = xpath.compile("./gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition");
            timeStr = expr.evaluate(timeElement);
        }
        if (!timeStr.isEmpty()) {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            throw new IllegalArgumentException("No valid time end found from element " + timeElement.getTagName());
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
