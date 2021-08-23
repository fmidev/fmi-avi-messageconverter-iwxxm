package fi.fmi.avi.converter.iwxxm.v3_0.sigmet;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericSIGMETIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> SIGMET_30_LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> sigmet30LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm30:originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm30:issuingAirTrafficServicesUnit/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm30:issuingAirTrafficServicesRegion/aixm:Airspace/aixm:timeSlice/aixm:AirspaceTimeSlice/aixm:designator");
        SIGMET_30_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(sigmet30LocationIndicatorExpressions);
    }

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.SIGMET);
        final IssueList retval = new IssueList();
        //Issue time:
        collectIssueTime(xpath, "./iwxxm30:issueTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        retval.addAll(collectValidTime(featureElement, "./iwxxm30:validPeriod[1]", xpath, builder));

        collectLocationIndicators(featureElement, xpath, builder, SIGMET_30_LOCATION_INDICATOR_EXPRESSIONS, retval);

        return retval;
    }

}
