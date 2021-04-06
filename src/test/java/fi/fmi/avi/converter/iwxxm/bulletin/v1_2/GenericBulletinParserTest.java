package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
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
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream inputStream = GenericBulletinParserTest.class.getResourceAsStream(filename)) {
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testScanner() throws Exception {
        final BulletinProperties properties = new BulletinProperties();
        final IWXXMGenericBulletinScanner scanner = new IWXXMGenericBulletinScanner();
        scanner.collectBulletinProperties(this.getBulletinDocument("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);

        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParserWithTAF() throws Exception {
        final Document input = this.getBulletinDocument("taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input, IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    public void testParserWithSIGMET() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input, IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
    }
}
