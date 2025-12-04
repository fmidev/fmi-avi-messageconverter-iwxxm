package fi.fmi.avi.converter.iwxxm.generic.taf;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.*;

/**
 * Field-XPath provider for generic IWXXM TAF messages.
 */
public final class TAFFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public TAFFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for TAF (structure is the same in 2.1 and 3.0):
        // TAF/issueTime/TimeInstant/timePosition
        map.put(IWXXMField.ISSUE_TIME, Collections.singletonList(
                XPathBuilder.text("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition")));

        // AERODROME location indicator.
        // 1) IWXXM 3.0: TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice.
        // 2) IWXXM 2.1: TAF/baseForecast/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/
        //               sampledFeature/AirportHeliport/timeSlice/AirportHeliportTimeSlice.
        // 3) IWXXM 2.1 cancellation: TAF/previousReportAerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice.
        map.put(IWXXMField.AERODROME, Arrays.asList(
                // IWXXM 3.0 style
                XPathBuilder.node("/iwxxm:TAF/iwxxm:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice"),
                // IWXXM 2.1 baseForecast style
                XPathBuilder.node("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice"),
                // IWXXM 2.1 cancellation style (previousReportAerodrome)
                XPathBuilder.node("/iwxxm:TAF/iwxxm:previousReportAerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice")));

        // Validity time for TAF:
        //  - IWXXM 3.0 normal reports: validPeriod on the root.
        //  - IWXXM 3.0 cancellations: cancelledReportValidPeriod on the root.
        //  - IWXXM 2.1: validTime on the root.
        map.put(IWXXMField.VALID_TIME, Arrays.asList(
                XPathBuilder.relative("./iwxxm:validPeriod"),
                XPathBuilder.relative("./iwxxm:cancelledReportValidPeriod"),
                XPathBuilder.relative("./iwxxm:validTime")));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
