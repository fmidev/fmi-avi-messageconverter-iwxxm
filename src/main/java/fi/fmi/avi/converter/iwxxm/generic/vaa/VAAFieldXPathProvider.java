package fi.fmi.avi.converter.iwxxm.generic.vaa;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Field-XPath provider for generic IWXXM Volcanic Ash Advisory messages.
 */
public class VAAFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public VAAFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for VAA (same structure across all IWXXM versions)
        map.put(IWXXMField.ISSUE_TIME, Collections.singletonList(
                XPathBuilder.text("/iwxxm:VolcanicAshAdvisory"
                        + "/iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")));

        // ISSUING_CENTRE (same structure across all IWXXM versions)
        map.put(IWXXMField.ISSUING_CENTRE, Collections.singletonList(
                XPathBuilder.node("/iwxxm:VolcanicAshAdvisory"
                        + "/iwxxm:issuingVolcanicAshAdvisoryCentre"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice")));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
