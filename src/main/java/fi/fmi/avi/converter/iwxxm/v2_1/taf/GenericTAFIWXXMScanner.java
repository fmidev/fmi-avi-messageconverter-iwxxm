package fi.fmi.avi.converter.iwxxm.v2_1.taf;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
        final IssueList retval = new IssueList();
        //Issue time:
        collectIssueTime(xpath, "./iwxxm:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        final String status = evaluateString(xpath, "@status", featureElement);

        if (!"MISSING".equals(status)) {
            //validity time
            retval.addAll(collectValidTime(featureElement, "./iwxxm:validTime[1]", xpath, builder));
        }

        //target aerodrome
        if ("CANCELLATION".equals(status)) {
            parseAerodromeDesignator(featureElement, "./iwxxm:previousReportAerodrome/aixm:AirportHeliport", xpath, builder, retval, status);
        } else {
            parseAerodromeDesignator(featureElement, "./iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport", xpath, builder, retval, status);
        }


        return retval;
    }
}
