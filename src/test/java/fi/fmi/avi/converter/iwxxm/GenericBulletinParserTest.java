package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertTrue;

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
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.bulletin.IWXXMGenericBulletinScanner;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(GenericBulletinParserTest.class.getResourceAsStream("taf-bulletin.xml"));
    }

    @Test
    public void testScanner() throws Exception {
        BulletinProperties properties = new BulletinProperties();
        IWXXMGenericBulletinScanner.collectBulletinProperties(this.getBulletinDocument(), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
    }
}
