package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
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
        final boolean isCancelMessage = (expr.evaluate(featureElement) != null && expr.evaluate(featureElement).equalsIgnoreCase("true"));
        if (isCancelMessage) {
            collectValidTime(featureElement, "./iwxxm30:cancelledReportValidPeriod", xpath, builder);
        } else {
            collectValidTime(featureElement, "./iwxxm30:validPeriod", xpath, builder);
        }

        expr = xpath.compile("@reportStatus");
        final String status = expr.evaluate(featureElement);
        try {
            builder.setReportStatus(AviationWeatherMessage.ReportStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            retval.add(ConversionIssue.Severity.ERROR, "The report status could not be parsed");
        }

        //target aerodrome
        expr = xpath.compile("./iwxxm30:aerodrome/aixm:AirportHeliport");
        parseAerodromeDesignator(featureElement, expr, xpath, builder, retval, status);

        return retval;
    }
}
