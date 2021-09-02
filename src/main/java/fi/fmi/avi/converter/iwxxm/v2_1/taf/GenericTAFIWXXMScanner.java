package fi.fmi.avi.converter.iwxxm.v2_1.taf;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericTAFIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.TAF);
        final IssueList retval = new IssueList();

        collectIssueTime(xpath, "./iwxxm:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        final AviationCodeListUser.TAFStatus status = evaluateEnumeration(featureElement, xpath, "@status", AviationCodeListUser.TAFStatus.class).orElse(null);

        if (status == null) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        } else {
            builder.setReportStatus(status.getReportStatus());
        }

        if (status != AviationCodeListUser.TAFStatus.MISSING) {
            retval.addAll(collectValidTime(featureElement, "./iwxxm:validTime[1]", xpath, builder));
        }

        if (status == AviationCodeListUser.TAFStatus.CANCELLATION) {
            parseAerodromeDesignator(featureElement, "./iwxxm:previousReportAerodrome/aixm:AirportHeliport", xpath, builder, retval);
        } else {
            parseAerodromeDesignator(featureElement,
                    "./iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport", xpath,
                    builder, retval);
        }

        return retval;
    }
}
