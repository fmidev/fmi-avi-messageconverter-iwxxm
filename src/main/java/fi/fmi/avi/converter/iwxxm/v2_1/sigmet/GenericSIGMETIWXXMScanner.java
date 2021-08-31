package fi.fmi.avi.converter.iwxxm.v2_1.sigmet;

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

public class GenericSIGMETIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> SIGMET_21_LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> sigmet21LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm" + ":originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm:issuingAirTrafficServicesUnit/aixm" + ":Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        sigmet21LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm:analysis/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:Airspace/aixm"
                        + ":timeSlice/aixm:AirspaceTimeSlice/aixm:designator");
        SIGMET_21_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(sigmet21LocationIndicatorExpressions);
    }

    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.SIGMET);
        final IssueList retval = new IssueList();
        collectIWXXM21TAFStatus(featureElement, xpath, builder);
        //Issue time:
        collectIssueTime(xpath, "./iwxxm:analysis/om:OM_Observation/om:resultTime/gml:TimeInstant/gml:timePosition", featureElement, builder, retval);

        collectLocationIndicators(featureElement, xpath, builder, SIGMET_21_LOCATION_INDICATOR_EXPRESSIONS, retval);

        retval.addAll(collectValidTime(featureElement, "./iwxxm:validPeriod[1]", xpath, builder));

        return retval;
    }
}
