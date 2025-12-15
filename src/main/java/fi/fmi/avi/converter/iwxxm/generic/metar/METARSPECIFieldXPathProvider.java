package fi.fmi.avi.converter.iwxxm.generic.metar;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for METAR/SPECI messages.
 */
public final class METARSPECIFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public METARSPECIFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for METAR/SPECI:
        // 1) IWXXM 3.0+: issueTime/TimeInstant/timePosition
        // 2) IWXXM 2.1: observation/OM_Observation/phenomenonTime/TimeInstant/timePosition
        XPathBuilder.put(map, IWXXMField.ISSUE_TIME,
                // IWXXM 3.0+ style
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition",
                // IWXXM 2.1 style
                "./iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:phenomenonTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        // OBSERVATION_TIME for METAR/SPECI:
        // 1) IWXXM 3.0+: observationTime/TimeInstant/timePosition
        // 2) IWXXM 2.1: observation/OM_Observation/phenomenonTime/TimeInstant/timePosition (same as issue time)
        XPathBuilder.put(map, IWXXMField.OBSERVATION_TIME,
                // IWXXM 3.0+ style
                "./iwxxm:observationTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition",
                // IWXXM 2.1 style
                "./iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:phenomenonTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        // AERODROME location indicator:
        // 1) IWXXM 3.0+: aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        // 2) IWXXM 2.1: observation/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/
        //               sampledFeature/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        XPathBuilder.put(map, IWXXMField.AERODROME,
                // IWXXM 3.0+ style
                "./iwxxm:aerodrome"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice",
                // IWXXM 2.1 style
                "./iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:featureOfInterest"
                        + "/sams:SF_SpatialSamplingFeature"
                        + "/sam:sampledFeature"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice");

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
