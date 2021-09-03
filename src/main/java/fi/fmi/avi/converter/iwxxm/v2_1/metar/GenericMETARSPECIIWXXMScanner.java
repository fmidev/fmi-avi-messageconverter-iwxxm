package fi.fmi.avi.converter.iwxxm.v2_1.metar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericMETARSPECIIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    private static final Map<String, MessageType> MESSAGE_TYPES_BY_ELEMENT_NAME;
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<String, MessageType> messageTypesByElementName = new HashMap<>();
        messageTypesByElementName.put("METAR", MessageType.METAR);
        messageTypesByElementName.put("SPECI", MessageType.SPECI);
        MESSAGE_TYPES_BY_ELEMENT_NAME = Collections.unmodifiableMap(messageTypesByElementName);

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> locationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                "iwxxm:observation/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(locationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        final MessageType messageType = MESSAGE_TYPES_BY_ELEMENT_NAME.get(featureElement.getLocalName());
        if (messageType == null) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Document element is not METAR or SPECI");
        } else {
            builder.setMessageType(messageType);
        }

        collectStatus(featureElement, xpath, builder, retval);
        collectIssueTime(xpath, featureElement, builder, retval);
        collectLocationIndicators(featureElement, xpath, builder, retval);

        return retval;
    }

    protected void collectStatus(final Element element, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues)
            throws XPathExpressionException {
        final AviationCodeListUser.MetarStatus status = evaluateEnumeration(element, xpath, "@status", AviationCodeListUser.MetarStatus.class).orElse(null);
        if (status == null) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        } else {
            builder.setReportStatus(status.getReportStatus());
        }
    }

    protected void collectIssueTime(final XPath xpath, final Element element, final GenericAviationWeatherMessageImpl.Builder builder, final IssueList issues)
            throws XPathExpressionException {
        collectIssueTime(xpath, "./iwxxm:observation/om:OM_Observation/om:phenomenonTime/gml:TimeInstant/gml:timePosition", element, builder, issues);
    }

    protected void collectLocationIndicators(final Element element, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder,
            final IssueList issues) throws XPathExpressionException {
        collectLocationIndicators(element, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, issues);
    }
}
