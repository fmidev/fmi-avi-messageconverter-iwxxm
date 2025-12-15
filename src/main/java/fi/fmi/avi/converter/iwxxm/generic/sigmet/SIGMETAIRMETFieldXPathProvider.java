package fi.fmi.avi.converter.iwxxm.generic.sigmet;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Field-XPath provider for generic IWXXM SIGMET and AIRMET messages.
 */
public final class SIGMETAIRMETFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public SIGMETAIRMETFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for SIGMET/AIRMET:
        // 1) IWXXM 3.0+: issueTime/TimeInstant/timePosition
        // 2) IWXXM 2.1: analysis/OM_Observation/resultTime/TimeInstant/timePosition
        XPathBuilder.put(map, IWXXMField.ISSUE_TIME,
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

        // ORIGINATING_MWO location indicator UnitTimeSlice (same structure across all versions)
        XPathBuilder.put(map, IWXXMField.ORIGINATING_MWO,
                "./iwxxm:originatingMeteorologicalWatchOffice"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        // ISSUING_ATS_UNIT (same structure across all IWXXM versions)
        XPathBuilder.put(map, IWXXMField.ISSUING_ATS_UNIT,
                "./iwxxm:issuingAirTrafficServicesUnit"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        // ISSUING_ATS_REGION:
        // - IWXXM 3.0+: issuingAirTrafficServicesRegion/Airspace/TimeSlice
        // - IWXXM 2.1: analysis/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/sampledFeature/Airspace/TimeSlice
        XPathBuilder.put(map, IWXXMField.ISSUING_ATS_REGION,
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

        // Validity time for SIGMET/AIRMET:
        //  - IWXXM 3.0+: validPeriod
        //  - IWXXM 2.1: validTime
        XPathBuilder.put(map, IWXXMField.VALID_TIME,
                "./iwxxm:validPeriod",
                "./iwxxm:validTime");

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
