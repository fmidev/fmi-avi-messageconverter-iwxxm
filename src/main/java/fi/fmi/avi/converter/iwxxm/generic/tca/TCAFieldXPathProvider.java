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
public final class TCAFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public TCAFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        XPathBuilder.put(map, IWXXMField.ISSUE_TIME,
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        XPathBuilder.put(map, IWXXMField.ISSUING_CENTRE,
                "./iwxxm:issuingTropicalCycloneAdvisoryCentre"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.emptyList();
    }
}
