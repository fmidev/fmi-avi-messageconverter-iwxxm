package fi.fmi.avi.converter.iwxxm.v2_1.airmet;

import java.util.Collections;
import java.util.EnumMap;
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

public class GenericAIRMETIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> locationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                "./iwxxm" + ":originatingMeteorologicalWatchOffice/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                "./iwxxm:issuingAirTrafficServicesUnit/aixm" + ":Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:designator");
        locationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                "./iwxxm:analysis/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:Airspace/aixm"
                        + ":timeSlice/aixm:AirspaceTimeSlice/aixm:designator");
        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(locationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.AIRMET);
        final IssueList retval = new IssueList();

        final AviationCodeListUser.SigmetAirmetReportStatus status = evaluateEnumeration(featureElement, xpath, "@status", AviationCodeListUser.SigmetAirmetReportStatus.class).orElse(null);
        if (status == null) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        } else {
            builder.setReportStatus(status.getReportStatus());
        }
        retval.addAll(collectValidTime(featureElement, "./iwxxm:validPeriod", xpath, builder));
        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);

        return retval;
    }
}
