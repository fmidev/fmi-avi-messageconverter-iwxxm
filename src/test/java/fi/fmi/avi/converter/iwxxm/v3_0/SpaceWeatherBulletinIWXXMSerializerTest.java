package fi.fmi.avi.converter.iwxxm.v3_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherBulletinImpl;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherBulletinIWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private SpaceWeatherBulletin getSWXBulletin(final String... fileNames) throws IOException {
        final List<SpaceWeatherAdvisory> swxMessages = new ArrayList<>();
        for (final String fName : fileNames) {
            SpaceWeatherAdvisory t = readFromJSON(fName);
            //Need to do any patching with external data here? (complete dates or geometries?)
            swxMessages.add(t);
        }
        final SpaceWeatherBulletinImpl.Builder bulletinBuilder = SpaceWeatherBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.XMLDataTypeDesignatorT2.XML_SPACE_WEATHER_ADVISORY)//
                        .setType(BulletinHeading.Type.NORMAL)//
                        .setGeographicalDesignator("XX")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(31)//
                        .setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2020, 5, 18, 8, 15, 0, 0, ZoneId.of("Z"))))//
                        .build())
                .setTimeStamp(ZonedDateTime.of(2020, 5, 18, 8, 10, 0, 0, ZoneId.of("Z")))
                .addTimeStampFields(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY);
        bulletinBuilder.addAllMessages(swxMessages);
        return bulletinBuilder.build();
    }

    @Test
    public void testBulletinStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        final SpaceWeatherBulletin tb = getSWXBulletin("spacewx-A2-3.json", "spacewx-A2-4.json", "spacewx-A2-5.json");
        final ConversionResult<String> result = converter.convertMessage(tb, IWXXMConverter.SWX_BULLETIN_POJO_TO_WMO_COLLECT_STRING);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        //Test that the namespace prefix mappings and schemaLocation attributes are included in the correct elements:

        //Collect:
        final String s = result.getConvertedMessage().get();
        final String bulletinElementFQN = "collect:MeteorologicalBulletin";
        final int bulletinElementStartIndex = s.indexOf("<" + bulletinElementFQN);
        final int bulletinElementEndIndex = s.indexOf(">", bulletinElementStartIndex);
        String attrs = s.substring(bulletinElementStartIndex + bulletinElementFQN.length() + 1, bulletinElementEndIndex).trim();
        assertTrue(attrs.contains("xmlns:gml=\"http://www.opengis.net/gml/3.2\""));
        assertTrue(attrs.contains("xmlns:collect=\"http://def.wmo.int/collect/2014\""));

        //Messages:
        final String swxAdvisoryFQN = "iwxxm:SpaceWeatherAdvisory";
        int swxElementStartIndex = -1;
        int swxElementEndIndex = bulletinElementEndIndex;
        for (int i = 0; i < 3; i++) {
            swxElementStartIndex = s.indexOf("<" + swxAdvisoryFQN, swxElementEndIndex + 1);
            swxElementEndIndex = s.indexOf(">", swxElementStartIndex);
            attrs = s.substring(swxElementStartIndex + swxAdvisoryFQN.length() + 1, swxElementEndIndex).trim();
            assertTrue("Missing gml in message #" + (i + 1), attrs.contains("xmlns:gml=\"http://www.opengis.net/gml/3.2\""));
            assertTrue("Missing iwxxm in message #" + (i + 1), attrs.contains("xmlns:iwxxm=\"http://icao.int/iwxxm/3.0\""));
            assertTrue("Missing aixm in message #" + (i + 1), attrs.contains("xmlns:aixm=\"http://www.aixm.aero/schema/5.1.1\""));
            assertTrue("Missing xlink in message #" + (i + 1), attrs.contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\""));
        }

    }

    @Test
    public void testBulletinDOMSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));
        final SpaceWeatherBulletin tb = getSWXBulletin("spacewx-A2-3.json", "spacewx-A2-4.json", "spacewx-A2-5.json");
        final ConversionResult<Document> result = converter.convertMessage(tb, IWXXMConverter.SWX_BULLETIN_POJO_TO_WMO_COLLECT_DOM);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final IWXXMNamespaceContext ctx = new IWXXMNamespaceContext();
        ctx.addNamespacePrefix("http://www.w3.org/2000/xmlns/", "xmlns");
        xpath.setNamespaceContext(ctx);

        final Element docElement = result.getConvertedMessage().map(Document::getDocumentElement).orElse(null);
        assertNotNull(docElement);

        XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        final String bulletinId = expr.evaluate(docElement);
        assertEquals("A_LNXX31EFKL180815_C_EFKL_2020051808----.xml", bulletinId);

        expr = xpath.compile("count(/collect:MeteorologicalBulletin/collect:meteorologicalInformation)");
        assertTrue(3 == Integer.parseInt(expr.evaluate(docElement)));
        expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation[1]/iwxxm30:SpaceWeatherAdvisory/"
                + "iwxxm30:analysis[1]/iwxxm30:SpaceWeatherAnalysis/iwxxm30:region[1]/iwxxm30:SpaceWeatherRegion/iwxxm30:location[1]"
                + "/aixm:AirspaceVolume/aixm:horizontalProjection/aixm:Surface/gml:patches[1]/gml:PolygonPatch/gml:exterior/gml:LinearRing/gml:posList");

        assertEquals("-180.0 90.0 -180.0 60.0 180.0 60.0 180.0 90.0 -180.0 90.0", expr.evaluate(docElement));
    }

    protected SpaceWeatherAdvisory readFromJSON(final String fileName) throws IOException {
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        final InputStream is = SpaceWeatherBulletinIWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            return om.readValue(is, SpaceWeatherAdvisoryImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }
}
