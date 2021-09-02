package fi.fmi.avi.converter.iwxxm.v3_0.metar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractIWXXM30GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericMETARIWXXMScanner extends AbstractIWXXM30GenericAviationWeatherMessageScanner {
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> locationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                "./iwxxm30:aerodrome/aixm:AirportHeliport/aixm" + ":timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(locationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.METAR);
        final IssueList retval = new IssueList();
        collectReportStatus(featureElement, xpath, builder).ifPresent(retval::add);
        collectIssueTime(featureElement, xpath, builder, retval);
        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);
        return retval;
    }
}
