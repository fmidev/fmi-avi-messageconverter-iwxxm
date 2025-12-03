package fi.fmi.avi.converter.iwxxm.generic.tca;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Field-XPath provider for generic IWXXM Tropical Cyclone Advisory messages.
 */
public class TCAFieldXPathProvider implements FieldXPathProvider {
    private static final String IWXXM_NS_PREFIX = "://icao.int/iwxxm/";
    private static final String GML_NS_PREFIX = "://www.opengis.net/gml/";
    private static final String AIXM_NS_PREFIX = "://www.aixm.aero/schema/";

    private final Map<IWXXMField, List<String>> expressions;

    public TCAFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME: root issueTime/TimeInstant/timePosition covers both 2.1 and 3.x
        map.put(IWXXMField.ISSUE_TIME, Collections.singletonList(String.format(
                "normalize-space((/*[contains(namespace-uri(),'%1$s') and local-name()='TropicalCycloneAdvisory']"
                        + "/*[contains(namespace-uri(),'%1$s') and local-name()='issueTime']"
                        + "/*[contains(namespace-uri(),'%2$s') and local-name()='TimeInstant']"
                        + "/*[contains(namespace-uri(),'%2$s') and local-name()='timePosition'])[1])",
                IWXXM_NS_PREFIX, GML_NS_PREFIX)));

        // ISSUING_CENTRE: issuingTropicalCycloneAdvisoryCentre/Unit/timeSlice/UnitTimeSlice
        map.put(IWXXMField.ISSUING_CENTRE, Collections.singletonList(String.format(
                "(/*[contains(namespace-uri(),'%1$s') and local-name()='TropicalCycloneAdvisory']"
                        + "/*[contains(namespace-uri(),'%1$s') and local-name()='issuingTropicalCycloneAdvisoryCentre']"
                        + "/*[contains(namespace-uri(),'%3$s') and local-name()='Unit']"
                        + "/*[contains(namespace-uri(),'%3$s') and local-name()='timeSlice']"
                        + "/*[contains(namespace-uri(),'%3$s') and local-name()='UnitTimeSlice'])[1]",
                IWXXM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX)));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
