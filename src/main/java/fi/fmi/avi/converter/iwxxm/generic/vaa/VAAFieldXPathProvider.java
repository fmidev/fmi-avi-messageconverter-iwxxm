package fi.fmi.avi.converter.iwxxm.generic.vaa;

import fi.fmi.avi.converter.iwxxm.generic.AbstractFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Version agnostic XPath provider for Volcanic Ash Advisories.
 */
public final class VAAFieldXPathProvider extends AbstractFieldXPathProvider {

    public VAAFieldXPathProvider() {
        super(createExpressions());
    }

    private static Map<IWXXMField, List<String>> createExpressions() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        put(map, IWXXMField.ISSUE_TIME,
                "./iwxxm:issueTime"
                        + "/gml:TimeInstant"
                        + "/gml:timePosition");

        put(map, IWXXMField.ISSUING_CENTRE,
                "./iwxxm:issuingVolcanicAshAdvisoryCentre"
                        + "/aixm:Unit"
                        + "/aixm:timeSlice"
                        + "/aixm:UnitTimeSlice");

        return Collections.unmodifiableMap(map);
    }
}
