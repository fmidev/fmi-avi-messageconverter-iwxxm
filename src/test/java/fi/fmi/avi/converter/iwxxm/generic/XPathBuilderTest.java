package fi.fmi.avi.converter.iwxxm.generic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XPathBuilderTest {

    @Test
    public void convertsMultipleNamespacedElements() {
        final String result = XPathBuilder.toVersionAgnostic(
                "./iwxxm:observation/om:OM_Observation/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport");
        assertEquals(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='observation']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/om/') and local-name()='OM_Observation']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/samplingSpatial/') and local-name()='SF_SpatialSamplingFeature']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/sampling/') and local-name()='sampledFeature']" +
                        "/*[contains(namespace-uri(),'://www.aixm.aero/schema/') and local-name()='AirportHeliport']",
                result);
    }

    @Test
    public void absolutePath() {
        final String result = XPathBuilder.toVersionAgnostic("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        assertEquals(
                "/*[contains(namespace-uri(),'://def.wmo.int/collect/') and local-name()='MeteorologicalBulletin']" +
                        "/*[contains(namespace-uri(),'://def.wmo.int/collect/') and local-name()='bulletinIdentifier']",
                result);
    }

    @Test
    public void unknownPrefixFallsBackToLocalNameOnly() {
        final String result = XPathBuilder.toVersionAgnostic("./iwxxm:parent/unknown:child/gml:grandchild");
        assertEquals(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='parent']" +
                        "/*[local-name()='child']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='grandchild']",
                result);
    }

    @Test
    public void preservesPredicates() {
        final String result = XPathBuilder.toVersionAgnostic("./iwxxm:element[1]/gml:child[@attr='value']");
        assertEquals(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='element'][1]" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='child'][@attr='value']",
                result);
    }

    @Test
    public void nonNamespacedPathUnchanged() {
        assertEquals("./element/child", XPathBuilder.toVersionAgnostic("./element/child"));
        assertEquals("", XPathBuilder.toVersionAgnostic(""));
    }

    @Test
    public void wrapsWithNormalizeSpace() {
        final String result = XPathBuilder.text("./iwxxm:issueTime");
        assertEquals("normalize-space((./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime'])[1])", result);
    }

    @Test
    public void wrapsWithIndexSelector() {
        final String result = XPathBuilder.node("./iwxxm:validTime");
        assertEquals("(./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='validTime'])[1]", result);
    }

}

