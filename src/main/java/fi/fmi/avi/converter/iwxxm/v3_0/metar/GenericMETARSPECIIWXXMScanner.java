package fi.fmi.avi.converter.iwxxm.v3_0.metar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.v3_0.generic.AbstractIWXXM30GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericMETARSPECIIWXXMScanner extends AbstractIWXXM30GenericAviationWeatherMessageScanner {
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
                "./iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice");
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
        collectReportStatus(featureElement, xpath, builder).ifPresent(retval::add);
        collectIssueTime(featureElement, xpath, builder, retval);
        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);
        return retval;
    }
}
