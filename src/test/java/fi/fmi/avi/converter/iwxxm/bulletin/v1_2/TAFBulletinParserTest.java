package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFBulletin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import java.util.Optional;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        assertThat(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO)).isTrue();
        final BulletinProperties properties = new BulletinProperties();
        final MeteorologicalBulletinIWXXMScanner<TAF, TAFBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        scanner.collectBulletinProperties(readDocumentFromResource("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertThat(properties.contains(BulletinProperties.Name.HEADING)).isTrue();
        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertThat(heading.get().getBulletinNumber()).isEqualTo(31);
        assertThat(properties.contains(BulletinProperties.Name.MESSAGE)).isTrue();
    }

    @Test
    public void testParser() throws Exception {
        final Document input = readDocumentFromResource("taf-bulletin.xml");
        final ConversionResult<TAFBulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO,
                ConversionHints.EMPTY);
        final TAFBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertThat(bulletin.getHeading().getOriginalCollectIdentifier()).hasValue("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml");
        assertThat(bulletin.getMessages()).hasSize(2);
        final TAF mesg = bulletin.getMessages().get(0);
        assertThat(mesg.getBaseForecast()).isPresent();
        final TAFBaseForecast baseForecast = mesg.getBaseForecast().get();
        assertThat(baseForecast.getSurfaceWind()).isPresent();
        final SurfaceWind surfaceWind = baseForecast.getSurfaceWind().get();
        assertThat(surfaceWind.getWindGust().get().getValue()).isCloseTo(26.0, within(0.0001));
    }

}
