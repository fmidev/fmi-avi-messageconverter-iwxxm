package fi.fmi.avi.converter.iwxxm.generic.sigmet;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.*;

import static fi.fmi.avi.converter.iwxxm.generic.XPathBuilder.anyOf;

/**
 * Field-XPath provider for generic IWXXM SIGMET and AIRMET messages.
 */
public final class SIGMETAIRMETFieldXPathProvider implements FieldXPathProvider {

    // Selector for any SIGMET or AIRMET root element
    private static final String SIGMET_OR_AIRMET = "/" + anyOf("iwxxm", "SIGMET", "VolcanicAshSIGMET", "TropicalCycloneSIGMET", "AIRMET");

    private final Map<IWXXMField, List<String>> expressions;

    public SIGMETAIRMETFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for SIGMET/AIRMET:
        // 1) IWXXM 3.0+: root/issueTime/TimeInstant/timePosition
        // 2) IWXXM 2.1: analysis/OM_Observation/resultTime/TimeInstant/timePosition
        map.put(IWXXMField.ISSUE_TIME, Arrays.asList(
                // IWXXM 3.0+ style
                "normalize-space((" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])",
                // IWXXM 2.1 analysis/resultTime style
                "normalize-space((" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:analysis"
                        + "/om:OM_Observation"
                        + "/om:resultTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])"));

        // ORIGINATING_MWO location indicator UnitTimeSlice (same structure across all versions)
        map.put(IWXXMField.ORIGINATING_MWO, Collections.singletonList(
                // Same structural pattern across all IWXXM versions
                "(" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:originatingMeteorologicalWatchOffice"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice")
                        + ")[1]"));

        // ISSUING_ATS_UNIT (same structure across all IWXXM versions)
        map.put(IWXXMField.ISSUING_ATS_UNIT, Collections.singletonList(
                "(" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:issuingAirTrafficServicesUnit"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice")
                        + ")[1]"));

        // ISSUING_ATS_REGION:
        // - IWXXM 3.0+: issuingAirTrafficServicesRegion/Airspace/TimeSlice
        // - IWXXM 2.1: analysis/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/sampledFeature/Airspace/TimeSlice
        map.put(IWXXMField.ISSUING_ATS_REGION, Arrays.asList(
                // IWXXM 3.0+ style
                "(" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:issuingAirTrafficServicesRegion"
                        + "/aixm:Airspace"
                        + "/aixm:timeSlice"
                        + "/aixm:AirspaceTimeSlice")
                        + ")[1]",
                // IWXXM 2.1 style
                "(" + SIGMET_OR_AIRMET
                        + XPathBuilder.toVersionAgnostic("/iwxxm:analysis"
                        + "/om:OM_Observation"
                        + "/om:featureOfInterest"
                        + "/sams:SF_SpatialSamplingFeature"
                        + "/sam:sampledFeature"
                        + "/aixm:Airspace"
                        + "/aixm:timeSlice"
                        + "/aixm:AirspaceTimeSlice")
                        + ")[1]"));

        map.put(IWXXMField.VALID_TIME, Arrays.asList(
                XPathBuilder.relative("./iwxxm:validPeriod"),
                XPathBuilder.relative("./iwxxm:validTime")));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
