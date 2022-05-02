package fi.fmi.avi.converter.iwxxm.v2_1.vaa;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericVolcanicAshAdvisoryIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> locationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                "iwxxm:issuingVolcanicAshAdvisoryCentre/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice");

        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(locationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.VOLCANIC_ASH_ADVISORY);
        final IssueList retval = new IssueList();
        collectIssueTime(xpath, "./iwxxm:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);
        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);
        return retval;
    }
}
