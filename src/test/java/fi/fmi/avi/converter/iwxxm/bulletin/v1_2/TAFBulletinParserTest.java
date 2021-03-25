package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

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
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument(final String filename) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(TAFBulletinParserTest.class.getResourceAsStream(filename));
    }

    @Test
    public void testScanner() throws Exception {
        assertTrue(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        final BulletinProperties properties = new BulletinProperties();
        final MeteorologicalBulletinIWXXMScanner<TAF, TAFBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        scanner.collectBulletinProperties(this.getBulletinDocument("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertEquals(31, heading.get().getBulletinNumber());
        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParser() throws Exception {
        final Document input = this.getBulletinDocument("taf-bulletin.xml");
        final ConversionResult<TAFBulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        if (result.getConvertedMessage().isPresent()) {
            final TAFBulletin bulletin = result.getConvertedMessage().get();
            assertEquals(2, bulletin.getMessages().size());
            final TAF mesg = bulletin.getMessages().get(0);
            assertEquals(26.0, mesg.getBaseForecast().get().getSurfaceWind().get().getWindGust().get().getValue(), 0.0001);
        }
    }

}
