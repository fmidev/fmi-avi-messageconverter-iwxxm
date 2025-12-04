package fi.fmi.avi.converter.iwxxm.generic.tca;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Field-XPath provider for generic IWXXM Tropical Cyclone Advisory messages.
 */
public class TCAFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public TCAFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME: root issueTime/TimeInstant/timePosition covers both 2.1 and 3.x
        map.put(IWXXMField.ISSUE_TIME, Collections.singletonList(
                XPathBuilder.text("/iwxxm:TropicalCycloneAdvisory/iwxxm:issueTime/gml:TimeInstant/gml:timePosition")));

        // ISSUING_CENTRE: issuingTropicalCycloneAdvisoryCentre/Unit/timeSlice/UnitTimeSlice
        map.put(IWXXMField.ISSUING_CENTRE, Collections.singletonList(
                XPathBuilder.node("/iwxxm:TropicalCycloneAdvisory/iwxxm:issuingTropicalCycloneAdvisoryCentre/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice")));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
