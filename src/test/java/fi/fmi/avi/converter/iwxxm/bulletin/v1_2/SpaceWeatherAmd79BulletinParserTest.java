package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherAmd79BulletinParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testScanner() throws Exception {
        assertThat(this.converter.isSpecificationSupported(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO)).isTrue();

        final BulletinProperties properties = new BulletinProperties();
        final MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();
        scanner.setMessageConverter(converter.getConverter(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO));
        scanner.collectBulletinProperties(readDocumentFromResource("swx-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertThat(properties.contains(BulletinProperties.Name.HEADING)).isTrue();

        final Optional<BulletinHeading> heading = properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class);
        assertThat(heading).isPresent();
        assertThat(heading.get().getBulletinNumber()).isEqualTo(31);
        assertThat(properties.contains(BulletinProperties.Name.MESSAGE)).isTrue();
    }

    @Test
    public void testParser() throws Exception {
        final Document input = readDocumentFromResource("swx-bulletin.xml");
        final ConversionResult<SpaceWeatherAmd79Bulletin> result = this.converter.convertMessage(input, IWXXMConverter.WMO_COLLECT_DOM_TO_SWX_30_BULLETIN_POJO,
                ConversionHints.EMPTY);
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).isPresent();

        final SpaceWeatherAmd79Bulletin bulletin = result.getConvertedMessage().get();
        assertThat(bulletin.getHeading().getOriginalCollectIdentifier()).hasValue("A_LNXX31EFKL301115_C_EFKL_201902011315--.xml");
        assertThat(bulletin.getMessages()).hasSize(1);

        final SpaceWeatherAdvisoryAmd79 mesg = bulletin.getMessages().get(0);
        assertThat(mesg.getPhenomena().get(0)).isEqualTo(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD"));
    }

}
