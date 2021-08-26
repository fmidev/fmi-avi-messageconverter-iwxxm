package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
        final IssueList retval = new IssueList();
        //Issue time:
        collectIssueTime(xpath, "./iwxxm30:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        final Optional<String> isCancelMessage = evaluateNonEmptyString(featureElement, xpath, "@isCancelReport");
        if (isCancelMessage.isPresent() && isCancelMessage.get().equalsIgnoreCase("true")) {
            collectValidTime(featureElement, "./iwxxm30:cancelledReportValidPeriod", xpath, builder);
        } else {
            collectValidTime(featureElement, "./iwxxm30:validPeriod", xpath, builder);
        }

        final Optional<String> status = evaluateNonEmptyString(featureElement, xpath, "@reportStatus");
        try {
            builder.setReportStatus(AviationWeatherMessage.ReportStatus.valueOf(status.orElse("")));
        } catch (IllegalArgumentException e) {
            retval.add(ConversionIssue.Severity.ERROR, "The report status could not be parsed");
        }

        //target aerodrome
        parseAerodromeDesignator(featureElement, "./iwxxm30:aerodrome/aixm:AirportHeliport", xpath, builder, retval, status.get());

        return retval;
    }
}
