package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractIWXXM30GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractIWXXM30GenericAviationWeatherMessageScanner {

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
        final IssueList retval = new IssueList();
        collectReportStatus(featureElement, xpath, builder).ifPresent(issue -> retval.add(issue));
        //Issue time:
        collectIssueTime(featureElement, xpath, builder, retval);

        final Optional<String> isCancelMessage = evaluateNonEmptyString(featureElement, xpath, "@isCancelReport");
        if (isCancelMessage.isPresent() && isCancelMessage.get().equalsIgnoreCase("true")) {
            collectValidTime(featureElement, "./iwxxm30:cancelledReportValidPeriod", xpath, builder);
        } else {
            collectValidTime(featureElement, "./iwxxm30:validPeriod", xpath, builder);
        }

        //target aerodrome
        parseAerodromeDesignator(featureElement, "./iwxxm30:aerodrome/aixm:AirportHeliport", xpath, builder, retval);

        return retval;
    }
}
