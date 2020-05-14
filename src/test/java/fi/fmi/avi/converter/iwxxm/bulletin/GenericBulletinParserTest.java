package fi.fmi.avi.converter.iwxxm.bulletin;

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
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument(final String filename) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(GenericBulletinParserTest.class.getResourceAsStream(filename));
    }

    @Test
    public void testScanner() throws Exception {
        BulletinProperties properties = new BulletinProperties();
        IWXXMGenericBulletinScanner.collectBulletinProperties(this.getBulletinDocument("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);

        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParserWithTAF() throws Exception {
        Document input = this.getBulletinDocument("taf-bulletin.xml");
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input, IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    public void testParserWithSIGMET() throws Exception {
        Document input = this.getBulletinDocument("sigmet-bulletin.xml");
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input, IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
    }
}
