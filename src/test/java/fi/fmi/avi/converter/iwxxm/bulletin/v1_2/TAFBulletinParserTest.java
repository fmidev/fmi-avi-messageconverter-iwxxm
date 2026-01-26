package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import java.util.Optional;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testScanner() throws Exception {
        assertTrue(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        final BulletinProperties properties = new BulletinProperties();
        final MeteorologicalBulletinIWXXMScanner<TAF, TAFBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        scanner.collectBulletinProperties(readDocumentFromResource("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertEquals(31, heading.get().getBulletinNumber());
        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParser() throws Exception {
        final Document input = readDocumentFromResource("taf-bulletin.xml");
        final ConversionResult<TAFBulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO,
                ConversionHints.EMPTY);
        final TAFBulletin bulletin = assertConversionResult(result).isSuccessful();
        assertEquals(2, bulletin.getMessages().size());
        final TAF mesg = bulletin.getMessages().get(0);
        assertEquals(26.0, mesg.getBaseForecast().get().getSurfaceWind().get().getWindGust().get().getValue(), 0.0001);
    }

}
