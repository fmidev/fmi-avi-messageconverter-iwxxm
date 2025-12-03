package fi.fmi.avi.converter.iwxxm.generic.swx;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.*;

/**
 * Field-XPath provider for generic IWXXM Space Weather Advisory messages.
 * <p>
 * XPaths are written using local-name() and contains(namespace-uri(), ...) so they are
 * robust across IWXXM/AIXM/GML schema versions that share the same namespace URI prefixes.
 */
public final class SWXFieldXPathProvider implements FieldXPathProvider {
    private static final String IWXXM_NS_PREFIX = "://icao.int/iwxxm/";
    private static final String AIXM_NS_PREFIX = "://www.aixm.aero/schema/";
    private static final String GML_NS_PREFIX = "://www.opengis.net/gml/";

    private final Map<IWXXMField, List<String>> expressions;

    public SWXFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for SpaceWeatherAdvisory
        map.put(IWXXMField.ISSUE_TIME, Arrays.asList(
                String.format(
                        "normalize-space((/*[contains(namespace-uri(),'%1$s') and local-name()='SpaceWeatherAdvisory']"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issueTime']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='TimeInstant']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='timePosition'])[1])",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX)));

        // ISSUING_CENTRE for SpaceWeatherAdvisory
        map.put(IWXXMField.ISSUING_CENTRE, Arrays.asList(
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and local-name()='SpaceWeatherAdvisory']"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issuingSpaceWeatherCentre']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='Unit']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='UnitTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, AIXM_NS_PREFIX)));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.<String>emptyList();
    }
}

