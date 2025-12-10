package fi.fmi.avi.converter.iwxxm.generic.metar;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.*;

import static fi.fmi.avi.converter.iwxxm.generic.XPathBuilder.anyOf;

/**
 * Field-XPath provider for generic IWXXM METAR/SPECI messages.
 */
public final class METARSPECIFieldXPathProvider implements FieldXPathProvider {

    // Selector for METAR or SPECI root element
    private static final String METAR_OR_SPECI = "/" + anyOf("iwxxm", "METAR", "SPECI");

    private final Map<IWXXMField, List<String>> expressions;

    public METARSPECIFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for METAR/SPECI:
        // 1) IWXXM 3.0+: issueTime/TimeInstant/timePosition directly under METAR/SPECI
        // 2) IWXXM 2.1: observation/OM_Observation/phenomenonTime/TimeInstant/timePosition
        map.put(IWXXMField.ISSUE_TIME, Arrays.asList(
                // IWXXM 3.0+ style
                "normalize-space((" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])",
                // IWXXM 2.1 style
                "normalize-space((" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:phenomenonTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])"));

        // OBSERVATION_TIME for METAR/SPECI:
        // 1) IWXXM 3.0+: observationTime/TimeInstant/timePosition directly under METAR/SPECI
        // 2) IWXXM 2.1: observation/OM_Observation/phenomenonTime/TimeInstant/timePosition
        map.put(IWXXMField.OBSERVATION_TIME, Arrays.asList(
                // IWXXM 3.0+ style
                "normalize-space((" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:observationTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])",
                // IWXXM 2.1 style (same as issue time - phenomenonTime is the observation time)
                "normalize-space((" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:phenomenonTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")
                        + ")[1])"));

        // AERODROME location indicator:
        // 1) IWXXM 3.0+: aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        // 2) IWXXM 2.1: observation/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/
        //               sampledFeature/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        map.put(IWXXMField.AERODROME, Arrays.asList(
                // IWXXM 3.0+ style
                "(" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:aerodrome"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice")
                        + ")[1]",
                // IWXXM 2.1 style
                "(" + METAR_OR_SPECI
                        + XPathBuilder.toVersionAgnostic("/iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:featureOfInterest"
                        + "/sams:SF_SpatialSamplingFeature"
                        + "/sam:sampledFeature"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice")
                        + ")[1]"));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
