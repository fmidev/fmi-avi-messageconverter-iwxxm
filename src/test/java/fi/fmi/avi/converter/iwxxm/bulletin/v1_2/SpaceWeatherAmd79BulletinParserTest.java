package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherPhenomenon;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherAmd79BulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument(final String filename) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (final InputStream inputStream = SpaceWeatherAmd79BulletinParserTest.class.getResourceAsStream(filename)) {
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testScanner() throws Exception {
        assertTrue(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO));
        final BulletinProperties properties = new BulletinProperties();
        final MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO));
        scanner.collectBulletinProperties(this.getBulletinDocument("swx-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertEquals(31, heading.get().getBulletinNumber());
        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParser() throws Exception {
        final Document input = this.getBulletinDocument("swx-bulletin.xml");
        final ConversionResult<SpaceWeatherAmd79Bulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_SWX_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        if (result.getConvertedMessage().isPresent()) {
            final SpaceWeatherAmd79Bulletin bulletin = result.getConvertedMessage().get();
            assertEquals(1, bulletin.getMessages().size());
            final SpaceWeatherAdvisoryAmd79 mesg = bulletin.getMessages().get(0);
            assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD"), mesg.getPhenomena().get(0));
        }
    }

}
