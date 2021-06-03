package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class IWXXMGenericBulletinScanner extends MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    private static IssueList collectSIGMETMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        //Issue time:
        final XPathExpression expr = xpath.compile("./iwxxm:analysis/om:OM_Observation/om:resultTime/gml:TimeInstant/gml:timePosition");
        final String timeStr = expr.evaluate(featureElement);
        if (!timeStr.isEmpty()) {
            builder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No issue time found for IWXXM SIGMET");
        }

        retval.addAll(collectValidTime(featureElement, "./iwxxm:validPeriod[1]", xpath, builder));
        return retval;
    }

    private static IssueList collectTAFMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        //Issue time:
        final String timeStr;
        XPathExpression expr = xpath.compile("./iwxxm:issueTime/gml:TimeInstant/gml:timePosition");
        timeStr = expr.evaluate(featureElement);
        if (!timeStr.isEmpty()) {
            builder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No issue time found for IWXXM TAF");
        }

        expr = xpath.compile("@status");
        final String status = expr.evaluate(featureElement);

        if (!"MISSING".equals(status)) {
            //validity time
            retval.addAll(collectValidTime(featureElement, "./gml:validTime[1]", xpath, builder));
        }

        //target aerodrome
        if ("CANCELLATION".equals(status)) {
            expr = xpath.compile("./iwxxm:previousReportAerodrome/aixm:AirportHeliport");
        } else {
            expr = xpath.compile(
                    "./iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/" + "aixm:AirportHeliport");
        }
        if (expr != null) {
            parseAerodromeInfo(featureElement, expr, xpath, builder, retval, status);
        }

        return retval;
    }

    private static IssueList collectTAF30Message(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        //Issue time:
        final String timeStr;
        XPathExpression expr = xpath.compile("./iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        timeStr = expr.evaluate(featureElement);
        if (!timeStr.isEmpty()) {
            builder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        } else {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No issue time found for IWXXM TAF");
        }

        expr = xpath.compile("@isCancelReport");
        final boolean isCancelMessage  = (expr.evaluate(featureElement) != null && expr.evaluate(featureElement).toLowerCase().equals("true"));
        if(isCancelMessage) {
            collectValidTime(featureElement, "./iwxxm30:cancelledReportValidPeriod", xpath, builder);
        } else {
            collectValidTime(featureElement, "./iwxxm30:validPeriod", xpath, builder);
        }

        expr = xpath.compile("@reportStatus");
        final String status = expr.evaluate(featureElement);
        builder.setReportStatus(AviationWeatherMessage.ReportStatus.valueOf(status));

        //target aerodrome
        expr = xpath.compile("./iwxxm30:aerodrome/aixm:AirportHeliport");
        parseAerodromeInfo(featureElement, expr, xpath, builder, retval, status);

        return retval;
    }

    private static IssueList collectValidTime(final Element featureElement, final String selector, final XPath xpath,
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

    private static void parseAerodromeInfo(final Element featureElement, final XPathExpression timeSliceExpretion, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues, final String status) throws XPathExpressionException {
        final NodeList nodes = (NodeList) timeSliceExpretion.evaluate(featureElement, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
            builder.setTargetAerodrome(parseAerodromeInfo((Element) nodes.item(0), xpath, issues));
        } else {
            issues.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Aerodrome info not available for TAF of status " + status);
        }
    }

    private static Optional<Aerodrome> parseAerodromeInfo(final Element airportHeliport, final XPath xpath, final IssueList issues)
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

    private static ZonedDateTime parseStartTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
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

    private static ZonedDateTime parseEndTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
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

    @Override
    protected ConversionResult<GenericAviationWeatherMessage> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        final ConversionResult<GenericAviationWeatherMessage> retval = new ConversionResult<>();
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        builder.setMessageFormat(GenericAviationWeatherMessage.Format.IWXXM);
        builder.setTranslated(true);

        try {
            final String messageType = featureElement.getLocalName();
            switch (messageType) {
                case "TAF":
                    builder.setMessageType(MessageType.TAF);
                    if(featureElement.getNamespaceURI().equals("http://icao.int/iwxxm/3.0")) {
                        retval.addIssue(collectTAF30Message(featureElement, xpath, builder));
                    } else {
                        retval.addIssue(collectTAFMessage(featureElement, xpath, builder));
                    }
                    break;

                case "METAR":
                    builder.setMessageType(MessageType.METAR);
                    break;

                case "SPECI":
                    builder.setMessageType(MessageType.SPECI);
                    break;

                case "SIGMET":
                case "TropicalCycloneSIGMET":
                case "VolcanicAshSIGMET":
                    builder.setMessageType(MessageType.SIGMET);
                    retval.addIssue(collectSIGMETMessage(featureElement, xpath, builder));
                    break;

                case "AIRMET":
                    builder.setMessageType(MessageType.AIRMET);
                    break;

                case "TropicalCycloneAdvisory":
                    builder.setMessageType(MessageType.TROPICAL_CYCLONE_ADVISORY);
                    break;

                case "VolcanicAshAdvisory":
                    builder.setMessageType(MessageType.VOLCANIC_ASH_ADVISORY);
                    break;

                default:
                    retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Unknown message type '" + messageType + "', unable to parse as " + "generic bulletin"));

            }

            //original message (as String)
            try {
                final StringWriter sw = new StringWriter();
                final Result output = new StreamResult(sw);
                final TransformerFactory tFactory = TransformerFactory.newInstance();
                final Transformer transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                final DOMSource dsource = new DOMSource(featureElement);
                transformer.transform(dsource, output);
                builder.setOriginalMessage(sw.toString());
            } catch (final TransformerException e) {
                retval.addIssue(
                        new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unable to write the message content as " + "string",
                                e));
            }
            retval.setConvertedMessage(builder.build());
        } catch (final XPathExpressionException xpee) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                    "Error in parsing content as a GenericAviationWeatherMessage", xpee));
        }
        return retval;
    }
}
