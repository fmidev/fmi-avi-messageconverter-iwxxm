package fi.fmi.avi.converter.iwxxm.generic.swx;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.converter.iwxxm.generic.XPathBuilder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Field-XPath provider for generic IWXXM Space Weather Advisory messages.
 */
public final class SWXFieldXPathProvider implements FieldXPathProvider {

    private final Map<IWXXMField, List<String>> expressions;

    public SWXFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for SpaceWeatherAdvisory
        map.put(IWXXMField.ISSUE_TIME, Collections.singletonList(
                XPathBuilder.text("/iwxxm:SpaceWeatherAdvisory"
                        + "/iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition")));

        // ISSUING_CENTRE for SpaceWeatherAdvisory
        map.put(IWXXMField.ISSUING_CENTRE, Collections.singletonList(
                XPathBuilder.node("/iwxxm:SpaceWeatherAdvisory"
                        + "/iwxxm:issuingSpaceWeatherCentre"
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

