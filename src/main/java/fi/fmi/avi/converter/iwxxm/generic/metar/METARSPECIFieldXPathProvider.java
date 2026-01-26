package fi.fmi.avi.converter.iwxxm.generic.metar;

import fi.fmi.avi.converter.iwxxm.generic.AbstractFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for METAR/SPECI messages.
 */
public final class METARSPECIFieldXPathProvider extends AbstractFieldXPathProvider {

    public METARSPECIFieldXPathProvider() {
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
                "./iwxxm:observation"
                        + "/om:OM_Observation"
                        + "/om:phenomenonTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        put(map, IWXXMField.OBSERVATION_TIME,
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

        put(map, IWXXMField.AERODROME,
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

        put(map, IWXXMField.NIL_REASON,
                // IWXXM 3.0+ style
                "./iwxxm:observation[not(*)]/@nilReason",
                // IWXXM 2.1 style
                "@status[.='MISSING']");

        return Collections.unmodifiableMap(map);
    }
}
