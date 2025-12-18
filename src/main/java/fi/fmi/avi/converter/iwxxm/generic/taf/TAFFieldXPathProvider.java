package fi.fmi.avi.converter.iwxxm.generic.taf;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for TAF messages.
 */
public final class TAFFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public TAFFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for TAF (structure is the same in 2.1 and 3.0)
        XPathBuilder.put(map, IWXXMField.ISSUE_TIME,
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        // AERODROME location indicator:
        // 1) IWXXM 3.0+: aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        // 2) IWXXM 2.1: baseForecast/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/
        //               sampledFeature/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        // 3) IWXXM 2.1 cancellation: previousReportAerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice
        XPathBuilder.put(map, IWXXMField.AERODROME,
                // IWXXM 3.0+ style
                "./iwxxm:aerodrome"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice",
                // IWXXM 2.1 baseForecast style
                "./iwxxm:baseForecast"
                        + "/om:OM_Observation"
                        + "/om:featureOfInterest"
                        + "/sams:SF_SpatialSamplingFeature"
                        + "/sam:sampledFeature"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice",
                // IWXXM 2.1 cancellation style
                "./iwxxm:previousReportAerodrome"
                        + "/aixm:AirportHeliport"
                        + "/aixm:timeSlice"
                        + "/aixm:AirportHeliportTimeSlice");

        // Validity time for TAF:
        //  - IWXXM 3.0+ normal reports: validPeriod
        //  - IWXXM 3.0+ cancellations: cancelledReportValidPeriod
        //  - IWXXM 2.1: validTime
        XPathBuilder.put(map, IWXXMField.VALID_TIME,
                "./iwxxm:validPeriod",
                "./iwxxm:cancelledReportValidPeriod",
                "./iwxxm:validTime");

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
