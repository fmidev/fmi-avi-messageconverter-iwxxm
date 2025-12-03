package fi.fmi.avi.converter.iwxxm.generic.sigmet;

import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;

import java.util.*;

/**
 * Field-XPath provider for generic IWXXM SIGMET and AIRMET messages.
 * <p>
 * Uses local-name() and contains(namespace-uri(), ...) so the expressions are
 * robust across IWXXM/AIXM/GML/OM schema versions that share the same URI prefixes.
 */
public final class SIGMETAIRMETFieldXPathProvider implements FieldXPathProvider {
    private static final String IWXXM_NS_PREFIX = "://icao.int/iwxxm/";
    private static final String GML_NS_PREFIX = "://www.opengis.net/gml/";
    private static final String AIXM_NS_PREFIX = "://www.aixm.aero/schema/";
    private static final String OM_NS_PREFIX = "://www.opengis.net/om/";
    private static final String SAMS_NS_PREFIX = "://www.opengis.net/samplingSpatial/";
    private static final String SAM_NS_PREFIX = "://www.opengis.net/sampling/";

    private final Map<IWXXMField, List<String>> expressions;

    public SIGMETAIRMETFieldXPathProvider() {
        final Map<IWXXMField, List<String>> map = new EnumMap<>(IWXXMField.class);

        // ISSUE_TIME for SIGMET/AIRMET.
        // 1) IWXXM 3.0 and 2023-1: root/issueTime/TimeInstant/timePosition.
        // 2) IWXXM 2.1 SIGMET and AIRMET: analysis/OM_Observation/resultTime/TimeInstant/timePosition.
        map.put(IWXXMField.ISSUE_TIME, Arrays.asList(
                // IWXXM 3.0+ style
                String.format(
                        "normalize-space((/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issueTime']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='TimeInstant']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='timePosition'])[1])",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX),
                // IWXXM 2.1 SIGMET and AIRMET analysis/resultTime style
                String.format(
                        "normalize-space((/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='analysis']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='OM_Observation']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='resultTime']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='TimeInstant']"
                                + "/*[contains(namespace-uri(),'%2$s') and local-name()='timePosition'])[1])",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX, OM_NS_PREFIX)));

        // ORIGINATING_MWO location indicator UnitTimeSlice across versions.
        map.put(IWXXMField.ORIGINATING_MWO, Arrays.asList(
                // IWXXM 3.0+/2023-1 and 2.1 share the same structural pattern here; we just ignore exact prefix.
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='originatingMeteorologicalWatchOffice']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='Unit']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='UnitTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX)));

        // ISSUING_ATS_UNIT: issuingAirTrafficServicesUnit/Unit/timeSlice/UnitTimeSlice
        map.put(IWXXMField.ISSUING_ATS_UNIT, Arrays.asList(
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issuingAirTrafficServicesUnit']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='Unit']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='UnitTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX)));

        // ISSUING_ATS_REGION: differs between 2.1 (analysis featureOfInterest/Airspace) and 3+/2023-1 (issuingAirTrafficServicesRegion/Airspace).
        map.put(IWXXMField.ISSUING_ATS_REGION, Arrays.asList(
                // IWXXM 3.0+ style: issuingAirTrafficServicesRegion/Airspace/TimeSlice
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='issuingAirTrafficServicesRegion']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='Airspace']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='AirspaceTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX),
                // IWXXM 2.1 style: analysis/OM_Observation/featureOfInterest/SF_SpatialSamplingFeature/sampledFeature/Airspace/TimeSlice
                String.format(
                        "(/*[contains(namespace-uri(),'%1$s') and (local-name()='SIGMET' or local-name()='VolcanicAshSIGMET' or local-name()='TropicalCycloneSIGMET' or local-name()='AIRMET')]"
                                + "/*[contains(namespace-uri(),'%1$s') and local-name()='analysis']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='OM_Observation']"
                                + "/*[contains(namespace-uri(),'%4$s') and local-name()='featureOfInterest']"
                                + "/*[contains(namespace-uri(),'%5$s') and local-name()='SF_SpatialSamplingFeature']"
                                + "/*[contains(namespace-uri(),'%6$s') and local-name()='sampledFeature']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='Airspace']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='timeSlice']"
                                + "/*[contains(namespace-uri(),'%3$s') and local-name()='AirspaceTimeSlice'])[1]",
                        IWXXM_NS_PREFIX, GML_NS_PREFIX, AIXM_NS_PREFIX, OM_NS_PREFIX, SAMS_NS_PREFIX, SAM_NS_PREFIX)));

        map.put(IWXXMField.VALID_TIME, Arrays.asList(
                "./*[contains(namespace-uri(), '://icao.int/iwxxm/') and local-name()='validPeriod']",
                "./*[contains(namespace-uri(), '://icao.int/iwxxm/') and local-name()='validTime'][1]"
        ));

        this.expressions = Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getXPaths(final IWXXMField field) {
        final List<String> result = expressions.get(field);
        return result != null ? result : Collections.<String>emptyList();
    }
}
