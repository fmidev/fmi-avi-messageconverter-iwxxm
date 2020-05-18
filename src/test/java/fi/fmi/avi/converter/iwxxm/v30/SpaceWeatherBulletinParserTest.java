package fi.fmi.avi.converter.iwxxm.v30;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.bulletin.MeteorologicalBulletinIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherBulletin;
import fi.fmi.avi.model.bulletin.BulletinHeading;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument(final String filename) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(SpaceWeatherBulletinParserTest.class.getResourceAsStream(filename));
    }

    @Test
    public void testScanner() throws Exception {
        assertTrue(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO));
        BulletinProperties properties = new BulletinProperties();
        MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisory, SpaceWeatherBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO));
        scanner.collectBulletinProperties(this.getBulletinDocument("swx-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertEquals(31, heading.get().getBulletinNumber());
        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParser() throws Exception {
        Document input = this.getBulletinDocument("swx-bulletin.xml");
        ConversionResult<SpaceWeatherBulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_SWX_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        if (result.getConvertedMessage().isPresent()) {
            SpaceWeatherBulletin bulletin = result.getConvertedMessage().get();
            assertEquals(1, bulletin.getMessages().size());
            SpaceWeatherAdvisory mesg = bulletin.getMessages().get(0);
            assertEquals("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD", mesg.getPhenomena().get(0));
        }
    }

}
