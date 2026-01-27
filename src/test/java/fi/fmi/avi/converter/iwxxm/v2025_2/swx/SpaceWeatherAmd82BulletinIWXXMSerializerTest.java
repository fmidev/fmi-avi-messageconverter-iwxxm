package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAmd82Bulletin;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAmd82BulletinImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherAmd82BulletinIWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private SpaceWeatherAmd82Bulletin getSWXBulletin(final String... fileNames) throws IOException {
        final BulletinHeading heading = BulletinHeadingImpl.builder()
                .setDataTypeDesignatorT2(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_SPACE_WEATHER_ADVISORY)
                .setType(BulletinHeading.Type.NORMAL)
                .setGeographicalDesignator("XX")
                .setLocationIndicator("TEST")
                .setBulletinNumber(31)
                .setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2020, 5, 18, 8, 15, 0, 0, ZoneId.of("Z"))))
                .build();
        return SpaceWeatherAmd82BulletinImpl.builder()
                .setHeading(heading)
                .setTimeStamp(ZonedDateTime.of(2020, 5, 18, 8, 10, 0, 0, ZoneId.of("Z")))
                .addTimeStampFields(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY)
                .addAllMessages(readAdvisories(fileNames))
                .build();
    }

    private List<SpaceWeatherAdvisoryAmd82> readAdvisories(final String... fileNames) throws IOException {
        final List<SpaceWeatherAdvisoryAmd82> advisories = new ArrayList<>();
        for (final String filename : fileNames) {
            advisories.add(readFromJSON(filename, SpaceWeatherAdvisoryAmd82Impl.class));
        }
        return advisories;
    }

    @Test
    public void testBulletinStringSerialization() throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.SWX_2025_2_BULLETIN_POJO_TO_WMO_COLLECT_STRING)).isTrue();
        final SpaceWeatherAmd82Bulletin bulletin = getSWXBulletin("spacewx-A2-3.json", "spacewx-A7-3.json");
        final ConversionResult<String> result = converter.convertMessage(bulletin, IWXXMConverter.SWX_2025_2_BULLETIN_POJO_TO_WMO_COLLECT_STRING);
        assertThatConversionResult(result).isSuccessful().hasXmlMessageEqualTo(readResourceToString("spacewx-bulletin.xml"));
    }

    @Test
    public void testBulletinDOMSerialization() throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.SWX_2025_2_BULLETIN_POJO_TO_WMO_COLLECT_DOM)).isTrue();
        final SpaceWeatherAmd82Bulletin bulletin = getSWXBulletin("spacewx-A2-3.json", "spacewx-A7-3.json");
        final ConversionResult<Document> result = converter.convertMessage(bulletin, IWXXMConverter.SWX_2025_2_BULLETIN_POJO_TO_WMO_COLLECT_DOM);

        final Document doc = assertThatConversionResult(result).isSuccessful().getMessage();

        final XPath xpath = XPathFactory.newInstance().newXPath();
        final IWXXMNamespaceContext ctx = new IWXXMNamespaceContext();
        ctx.addNamespacePrefix("http://www.w3.org/2000/xmlns/", "xmlns");
        xpath.setNamespaceContext(ctx);


        assertThat(xpath.evaluate("/collect:MeteorologicalBulletin/collect:bulletinIdentifier", doc))
                .isEqualTo("A_LNXX31TEST180815_C_TEST_2020051808----.xml");

        assertThat(xpath.evaluate("count(/collect:MeteorologicalBulletin/collect:meteorologicalInformation)", doc))
                .isEqualTo("2");

        assertThat(xpath.evaluate("/collect:MeteorologicalBulletin"
                + "/collect:meteorologicalInformation[1]"
                + "/iwxxm2025_2:SpaceWeatherAdvisory"
                + "/iwxxm2025_2:analysis[1]"
                + "/iwxxm2025_2:SpaceWeatherAnalysis"
                + "/iwxxm2025_2:intensityAndRegion[1]"
                + "/iwxxm2025_2:SpaceWeatherIntensityAndRegion"
                + "/iwxxm2025_2:region[1]"
                + "/iwxxm2025_2:SpaceWeatherRegion[1]"
                + "/iwxxm2025_2:location[1]"
                + "/aixm:AirspaceVolume"
                + "/aixm:horizontalProjection"
                + "/aixm:Surface"
                + "/gml:patches[1]"
                + "/gml:PolygonPatch"
                + "/gml:exterior"
                + "/gml:LinearRing"
                + "/gml:posList", doc))
                .isEqualTo("-180.0 90.0 -180.0 60.0 180.0 60.0 180.0 90.0 -180.0 90.0");
    }
}
