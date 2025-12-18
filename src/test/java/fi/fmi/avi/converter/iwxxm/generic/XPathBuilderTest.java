package fi.fmi.avi.converter.iwxxm.generic;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XPathBuilderTest {

    @Test
    public void convertsMultipleNamespacedElements() {
        final String result = XPathBuilder.toVersionAgnostic(
                "./iwxxm:observation/om:OM_Observation/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport");
        assertThat(result).isEqualTo(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='observation']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/om/') and local-name()='OM_Observation']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/samplingSpatial/') and local-name()='SF_SpatialSamplingFeature']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/sampling/') and local-name()='sampledFeature']" +
                        "/*[contains(namespace-uri(),'://www.aixm.aero/schema/') and local-name()='AirportHeliport']");
    }

    @Test
    public void absolutePath() {
        final String result = XPathBuilder.toVersionAgnostic("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        assertThat(result).isEqualTo(
                "/*[contains(namespace-uri(),'://def.wmo.int/collect/') and local-name()='MeteorologicalBulletin']" +
                        "/*[contains(namespace-uri(),'://def.wmo.int/collect/') and local-name()='bulletinIdentifier']");
    }

    @Test
    public void unknownPrefixFallsBackToLocalNameOnly() {
        final String result = XPathBuilder.toVersionAgnostic("./iwxxm:parent/unknown:child/gml:grandchild");
        assertThat(result).isEqualTo(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='parent']" +
                        "/*[local-name()='child']" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='grandchild']");
    }

    @Test
    public void preservesPredicates() {
        final String result = XPathBuilder.toVersionAgnostic("./iwxxm:element[1]/gml:child[@attr='value']");
        assertThat(result).isEqualTo(
                "./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='element'][1]" +
                        "/*[contains(namespace-uri(),'://www.opengis.net/gml/') and local-name()='child'][@attr='value']");
    }

    @Test
    public void nonNamespacedPathUnchanged() {
        assertThat(XPathBuilder.toVersionAgnostic("./element/child")).isEqualTo("./element/child");
        assertThat(XPathBuilder.toVersionAgnostic("")).isEqualTo("");
    }

    @Test
    public void wrapsWithNormalizeSpace() {
        final String result = XPathBuilder.text("./iwxxm:issueTime");
        assertThat(result).isEqualTo("normalize-space((./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='issueTime'])[1])");
    }

    @Test
    public void wrapsWithIndexSelector() {
        final String result = XPathBuilder.node("./iwxxm:validTime");
        assertThat(result).isEqualTo("(./*[contains(namespace-uri(),'://icao.int/iwxxm/') and local-name()='validTime'])[1]");
    }

}

