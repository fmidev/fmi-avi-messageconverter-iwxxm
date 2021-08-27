package fi.fmi.avi.converter.iwxxm.v3_0.metar;

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

public class GenericSPECIIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> SPECI_30_LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> speci21LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        speci21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                "./iwxxm30:aerodrome/aixm:AirportHeliport/aixm" + ":timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");

        SPECI_30_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(speci21LocationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.SPECI);
        final IssueList retval = new IssueList();

        collectIssueTime(xpath, "./iwxxm30:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        collectLocationIndicators(featureElement, xpath, builder, SPECI_30_LOCATION_INDICATOR_EXPRESSIONS, retval);

        retval.addAll(collectValidTime(featureElement, "./iwxxm:validPeriod[1]", xpath, builder));

        return retval;
    }
}
