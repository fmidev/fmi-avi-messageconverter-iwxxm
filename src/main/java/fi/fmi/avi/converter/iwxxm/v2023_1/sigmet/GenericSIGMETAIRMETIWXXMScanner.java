package fi.fmi.avi.converter.iwxxm.v2023_1.sigmet;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.v2023_1.generic.AbstractIWXXM20231GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GenericSIGMETAIRMETIWXXMScanner extends AbstractIWXXM20231GenericAviationWeatherMessageScanner {
    private static final Map<String, MessageType> MESSAGE_TYPES_BY_ELEMENT_NAME;
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<String, MessageType> messageTypesByElementName = new HashMap<>();
        messageTypesByElementName.put("SIGMET", MessageType.SIGMET);
        messageTypesByElementName.put("VolcanicAshSIGMET", MessageType.SIGMET);
        messageTypesByElementName.put("TropicalCycloneSIGMET", MessageType.SIGMET);
        messageTypesByElementName.put("AIRMET", MessageType.AIRMET);
        MESSAGE_TYPES_BY_ELEMENT_NAME = Collections.unmodifiableMap(messageTypesByElementName);

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> locationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm2023_1:originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice");
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm2023_1:issuingAirTrafficServicesUnit/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice");
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm2023_1:issuingAirTrafficServicesRegion/aixm:Airspace/aixm:timeSlice/aixm:AirspaceTimeSlice");
        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(locationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();
        final MessageType messageType = MESSAGE_TYPES_BY_ELEMENT_NAME.get(featureElement.getLocalName());
        if (messageType == null) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                    "Document element is not one of SIGMET, VolcanicAshSIGMET, TropicalCycloneSIGMET or AIRMET");
        } else {
            builder.setMessageType(messageType);
        }
        collectReportStatus(featureElement, xpath, builder).ifPresent(retval::add);
        collectIssueTime(featureElement, xpath, builder, retval);
        retval.addAll(collectValidTime(featureElement, "./iwxxm2023_1:validPeriod[1]", xpath, builder));
        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);
        return retval;
    }

}
