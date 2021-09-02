package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractIWXXM30GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractIWXXM30GenericAviationWeatherMessageScanner {

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
        final IssueList retval = new IssueList();
        collectReportStatus(featureElement, xpath, builder).ifPresent(retval::add);
        collectIssueTime(featureElement, xpath, builder, retval);

        final boolean isCancelReport = evaluateBoolean(featureElement, xpath, "@isCancelReport").orElse(false);
        if (isCancelReport) {
            collectValidTime(featureElement, "./iwxxm30:cancelledReportValidPeriod", xpath, builder);
        } else {
            collectValidTime(featureElement, "./iwxxm30:validPeriod", xpath, builder);
        }

        parseAerodromeDesignator(featureElement, "./iwxxm30:aerodrome/aixm:AirportHeliport", xpath, builder, retval);

        return retval;
    }
}
