package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class VASIGMETIWWXXMSerializerTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    private SIGMET getSIGMET() throws Exception {
        return readFromJSON("vasigmet1.json", SIGMETImpl.class);
    }

    @Test
    public void testSIGMETStringSerialization() throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING)).isTrue();
        final SIGMET s = getSIGMET();
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING);
        assertThatConversionResult(result).isSuccessful();
    }
}
