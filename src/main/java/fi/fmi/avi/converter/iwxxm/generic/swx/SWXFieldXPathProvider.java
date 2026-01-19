package fi.fmi.avi.converter.iwxxm.generic.swx;

import fi.fmi.avi.converter.iwxxm.generic.AbstractFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for Space Weather Advisories.
 */
public final class SWXFieldXPathProvider extends AbstractFieldXPathProvider {

    public SWXFieldXPathProvider() {
        super(createExpressions());
    }

    private static Map<IWXXMField, List<String>> createExpressions() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        put(map, IWXXMField.ISSUE_TIME,
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        put(map, IWXXMField.ISSUING_CENTRE,
                "./iwxxm:issuingSpaceWeatherCentre"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        return Collections.unmodifiableMap(map);
    }
}
