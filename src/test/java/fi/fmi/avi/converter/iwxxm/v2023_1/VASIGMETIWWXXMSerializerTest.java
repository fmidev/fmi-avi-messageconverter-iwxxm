package fi.fmi.avi.converter.iwxxm.v2023_1;

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

    private void assertSuccessfulSIGMETSerialization(final String jsonFileName, final String iwxxmFilename) throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING)).isTrue();
        final SIGMET sigmet = readFromJSON(jsonFileName, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(sigmet, IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING);
        assertThatConversionResult(result)
                .isSuccessful()
                .hasXmlMessageEqualTo(readResourceToString(iwxxmFilename));
    }

    @Test
    public void testVaSigmet() throws Exception {
        assertSuccessfulSIGMETSerialization("vasigmet1.json", "vasigmet1.xml");
    }

    @Test
    public void testVaSigmet_NoCoords() throws Exception {
        assertSuccessfulSIGMETSerialization("vasigmet1_nocoords.json", "vasigmet1_nocoords.xml");
    }

    @Test
    public void testVaSigmet_NoName() throws Exception {
        assertSuccessfulSIGMETSerialization("vasigmet1_noname.json", "vasigmet1_noname.xml");
    }

    @Test
    public void testVaSigmetCancel() throws Exception {
        assertSuccessfulSIGMETSerialization("vasigmet1_cancel.json", "vasigmet1_cancel.xml");
    }

    @Test
    public void testVaSigmetCancelMovToFir() throws Exception {
        assertSuccessfulSIGMETSerialization("vasigmet1_cancel_movtofir.json", "vasigmet1_cancel_movtofir.xml");
    }

}
