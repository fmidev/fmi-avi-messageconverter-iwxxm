package fi.fmi.avi.converter.iwxxm.generic.sigmet;

import fi.fmi.avi.converter.iwxxm.generic.AbstractFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for SIGMET and AIRMET messages.
 */
public final class SIGMETAIRMETFieldXPathProvider extends AbstractFieldXPathProvider {

    public SIGMETAIRMETFieldXPathProvider() {
        super(createExpressions());
    }

    private static Map<IWXXMField, List<String>> createExpressions() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        put(map, IWXXMField.ISSUE_TIME,
                // IWXXM 3.0+ style
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition",
                // IWXXM 2.1 style
                "./iwxxm:analysis"
                        + "/om:OM_Observation"
                        + "/om:resultTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        put(map, IWXXMField.ORIGINATING_MWO,
                // IWXXM 2.1 and 3.0+ style
                "./iwxxm:originatingMeteorologicalWatchOffice"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        put(map, IWXXMField.ISSUING_ATS_UNIT,
                // IWXXM 2.1 and 3.0+ style
                "./iwxxm:issuingAirTrafficServicesUnit"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        put(map, IWXXMField.ISSUING_ATS_REGION,
                // IWXXM 3.0+ style
                "./iwxxm:issuingAirTrafficServicesRegion"
                        + "/aixm:Airspace"
                        + "/aixm:timeSlice"
                        + "/aixm:AirspaceTimeSlice",
                // IWXXM 2.1 style
                "./iwxxm:analysis"
                        + "/om:OM_Observation"
                        + "/om:featureOfInterest"
                        + "/sams:SF_SpatialSamplingFeature"
                        + "/sam:sampledFeature"
                        + "/aixm:Airspace"
                        + "/aixm:timeSlice"
                        + "/aixm:AirspaceTimeSlice");

        put(map, IWXXMField.VALID_TIME,
                // IWXXM 3.0+ style
                "./iwxxm:validPeriod",
                // IWXXM 2.1 style
                "./iwxxm:validTime");

        return Collections.unmodifiableMap(map);
    }
}
