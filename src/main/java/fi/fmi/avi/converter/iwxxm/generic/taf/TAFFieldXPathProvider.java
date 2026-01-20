package fi.fmi.avi.converter.iwxxm.generic.taf;

import fi.fmi.avi.converter.iwxxm.generic.AbstractFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for TAF messages.
 */
public final class TAFFieldXPathProvider extends AbstractFieldXPathProvider {

    public TAFFieldXPathProvider() {
        super(createExpressions());
    }

    private static Map<IWXXMField, List<String>> createExpressions() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        put(map, IWXXMField.ISSUE_TIME,
                // IWXXM 2.1 and 3.0+ style
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        put(map, IWXXMField.AERODROME,
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

        put(map, IWXXMField.VALID_TIME,
                // IWXXM 3.0+ normal reports: validPeriod
                "./iwxxm:validPeriod",
                // IWXXM 3.0+ cancellations: cancelledReportValidPeriod
                "./iwxxm:cancelledReportValidPeriod",
                // IWXXM 2.1: validTime
                "./iwxxm:validTime");

        return Collections.unmodifiableMap(map);
    }
}
