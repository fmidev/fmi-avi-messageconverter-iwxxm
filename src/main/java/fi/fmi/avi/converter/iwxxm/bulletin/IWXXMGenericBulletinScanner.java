package fi.fmi.avi.converter.iwxxm.bulletin;

import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import fi.fmi.avi.util.GTSExchangeFileInfo;

public class IWXXMGenericBulletinScanner extends AbstractIWXXMScanner {

    public static List<ConversionIssue> collectBulletinProperties(final Document input,
            final BulletinProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            final String bulletinIdentifier = expr.evaluate(input.getDocumentElement());
            if ("".equals(bulletinIdentifier)) {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No or empty bulletinIdentifier in MeteorologicalBulletin");
                return retval;
            }

            retval.addAll(collectHeading(bulletinIdentifier, properties));

            expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation/*");
            final NodeList features = (NodeList) expr.evaluate(input.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < features.getLength(); i++) {
                collectGenericAviationWeatherMessage((Element) features.item(i), xpath, properties);
            }
        } catch (final XPathExpressionException xee) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unexpected error in parsing MeteorologicalBulletin", xee);
        }
        return retval;
    }

    private static IssueList collectHeading(final String bulletinIdentifier, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        try {
            final GTSExchangeFileInfo info = GTSExchangeFileInfo.Builder.from(bulletinIdentifier).build();
            properties.set(BulletinProperties.Name.HEADING, info.getHeading());
            info.getTimeStampYear().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_YEAR, value));
            info.getTimeStampMonth().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_MONTH, value));
            info.getTimeStampDay().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_DAY, value));
            info.getTimeStampHour().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_HOUR, value));
            info.getTimeStampMinute().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_MINUTE, value));
            info.getTimeStampSecond().ifPresent((value) -> properties.set(BulletinProperties.Name.TIMESTAMP_SECOND, value));
        } catch (final Exception e) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Could not parse bulletin heading info from the bulletinIdentifier", e);
        }
        return retval;
    }

    private static IssueList collectGenericAviationWeatherMessage(final Element featureElement, final XPath xpath, final BulletinProperties properties) {
        final IssueList retval = new IssueList();
        final GenericAviationWeatherMessageImpl.Builder builder = new GenericAviationWeatherMessageImpl.Builder();
        builder.setMessageFormat(GenericAviationWeatherMessage.Format.IWXXM);
        builder.setTranslated(true);

        try {
            final String messageType = featureElement.getLocalName();
            switch (messageType) {
                case "TAF":
                    builder.setMessageType(MessageType.TAF);
                    retval.addAll(collectTAFMessage(featureElement, xpath, builder));
                    break;

                case "METAR":
                    builder.setMessageType(MessageType.METAR);
                    break;

                case "SPECI":
                    builder.setMessageType(MessageType.SPECI);

                case "SIGMET":
                case "TropicalCycloneSIGMET":
                case "VolcanicAshSIGMET":
                    builder.setMessageType(MessageType.SIGMET);
                    retval.addAll(collectSIGMETMessage(featureElement, xpath, builder));
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
                    retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Unknown message type '" + messageType + "', unable to parse as " + "generic bulletin");

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
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Unable to write the message content as string", e);
            }
        } catch (final XPathExpressionException xpee) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Error in parsing content as a GenericAviationWeatherMessage", xpee);
        }
        properties.addToList(BulletinProperties.Name.MESSAGE, builder.build());
        return retval;
    }

    private static IssueList collectSIGMETMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        //Issue time:
        final XPathExpression expr = xpath.compile("./iwxxm:analysis/om:OM_Observation/om:resultTime/gml:TimeInstant/gml:timePosition");
        final String timeStr = expr.evaluate(featureElement);
        if (!"".equals(timeStr)) {
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
        String timeStr = null;
        XPathExpression expr = xpath.compile("./iwxxm:issueTime/gml:TimeInstant/gml:timePosition");
        timeStr = expr.evaluate(featureElement);
        if (!"".equals(timeStr)) {
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
        expr = null;
        if ("CANCELLATION".equals(status)) {
            expr = xpath.compile("./iwxxm:previousReportAerodrome/aixm:AirportHeliport");
        } else {
            expr = xpath.compile(
                    "./iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/" + "aixm:AirportHeliport");
        }
        if (expr != null) {
            final NodeList nodes = (NodeList) expr.evaluate(featureElement, XPathConstants.NODESET);
            if (nodes.getLength() == 1) {
                builder.setTargetAerodrome(parseAerodromeInfo((Element) nodes.item(0), xpath, retval));
            } else {
                retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Aerodrome info not available for TAF of status " + status);
            }
        }

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
                builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()//
                        .setStartTime(PartialOrCompleteTimeInstant.of(startTime))//
                        .setEndTime(PartialOrCompleteTimeInstant.of(endTime))//
                        .build());
            }
        } catch (final Exception ex) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Unable to parse valid time for TAF", ex);
        }
        return retval;
    }

    private static Optional<Aerodrome> parseAerodromeInfo(final Element airportHeliport, final XPath xpath, final IssueList issues)
            throws XPathExpressionException {
        Optional<Aerodrome> retval = Optional.empty();
        XPathExpression expr = xpath.compile("./aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice/aixm:designator");
        final String designator = expr.evaluate(airportHeliport);

        if ("".equals(designator)) {
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

        retval = Optional.of(new AerodromeImpl.Builder()//
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
        if ("".equals(timeStr)) {
            //validity time, begin/TimeInstant variant:
            expr = xpath.compile("./gml:TimePeriod/gml:begin/gml:TimeInstant/gml:timePosition");
            timeStr = expr.evaluate(timeElement);
        }
        if (!"".equals(timeStr)) {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            throw new IllegalArgumentException("No valid time begin found from element " + timeElement.getTagName());
        }
    }

    private static ZonedDateTime parseEndTime(final Element timeElement, final XPath xpath) throws XPathExpressionException {
        XPathExpression expr = xpath.compile("./gml:TimePeriod/gml:endPosition");
        String timeStr = expr.evaluate(timeElement);
        if ("".equals(timeStr)) {
            //validity time, begin/TimeInstant variant:
            expr = xpath.compile("./gml:TimePeriod/gml:end/gml:TimeInstant/gml:timePosition");
            timeStr = expr.evaluate(timeElement);
        }
        if (!"".equals(timeStr)) {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            throw new IllegalArgumentException("No valid time end found from element " + timeElement.getTagName());
        }
    }
}
