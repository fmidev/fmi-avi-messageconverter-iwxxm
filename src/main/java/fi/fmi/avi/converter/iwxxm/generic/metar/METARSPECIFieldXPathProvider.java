package fi.fmi.avi.converter.iwxxm.generic.metar;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.*;

/**
 * Field-XPath provider for generic IWXXM METAR/SPECI messages.
 * <p>
 * XPaths are written using local-name() and contains(namespace-uri(), ...) so they are
 * robust across IWXXM/AIXM/GML schema versions that share the same namespace URI prefixes.
 */
public final class METARSPECIFieldXPathProvider implements FieldXPathProvider {
    private static final String IWXXM_NS_PREFIX = "://icao.int/iwxxm/";
    private static final String OM_NS_PREFIX = "://www.opengis.net/om/";
    private static final String GML_NS_PREFIX = "://www.opengis.net/gml/";
    private static final String AIXM_NS_PREFIX = "://www.aixm.aero/schema/";
    private static final String SAMS_NS_PREFIX = "://www.opengis.net/samplingSpatial/";
    private static final String SAM_NS_PREFIX = "://www.opengis.net/sampling/";

    private final Map<IWXXMField, List<String>> expressions;

    public METARSPECIFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for METAR/SPECI.
        // 1) IWXXM 3.0: issueTime/TimeInstant/timePosition directly under METAR/SPECI.
        // 2) IWXXM 2.1: observation/OM_Observation/phenomenonTime/TimeInstant/timePosition.
        map.put(IWXXMField.ISSUE_TIME, Arrays.asList(
                // IWXXM 3.0 style
                String.format(
                        "normalize-space((/*[contains(namespace-uri(),'%1$s') and (local-name()='METAR' or local-name()='SPECI')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issueTime']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='TimeInstant']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timePosition'])[1])",
                        IWXXM_NS_PREFIX, OM_NS_PREFIX, GML_NS_PREFIX),
                // IWXXM 2.1 style
                String.format(
                        "normalize-space((/*[contains(namespace-uri(),'%1$s') and (local-name()='METAR' or local-name()='SPECI')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='observation']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='OM_Observation']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='phenomenonTime']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='TimeInstant']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timePosition'])[1])",
                        IWXXM_NS_PREFIX, OM_NS_PREFIX, GML_NS_PREFIX)));

        // AERODROME location indicator.
        // 1) IWXXM 3.0: aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice.
        // 2) IWXXM 2.1: observation/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/
        //               sampledFeature/AirportHeliport/timeSlice/AirportHeliportTimeSlice.
        map.put(IWXXMField.AERODROME, Arrays.asList(
                // IWXXM 3.0 style
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='METAR' or local-name()='SPECI')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='aerodrome']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='AirportHeliport']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='AirportHeliportTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, OM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX),
                // IWXXM 2.1 style
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='METAR' or local-name()='SPECI')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='observation']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='OM_Observation']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='featureOfInterest']"
                                + "/*[contains(namespace-uri(),'%5$s') and local-name()='SF_SpatialSamplingFeature']"
                                + "/*[contains(namespace-uri(),'%6$s') and local-name()='sampledFeature']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='AirportHeliport']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='AirportHeliportTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, OM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX, SAMS_NS_PREFIX, SAM_NS_PREFIX)));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.<String>emptyList();
    }
}
