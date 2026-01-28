package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.ConversionResultAssertion;
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

import java.io.IOException;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class VASIGMETIWWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private <T> ConversionResultAssertion<T> assertSIGMETSerialization(
            final String jsonFileName, final ConversionSpecification<SIGMET, T> conversionSpecification) throws IOException {
        assertThat(converter.isSpecificationSupported(conversionSpecification)).isTrue();
        final SIGMET sigmet = readFromJSON(jsonFileName, SIGMETImpl.class);
        final ConversionResult<T> result = converter.convertMessage(sigmet, conversionSpecification);
        return assertThatConversionResult(result);
    }

    private void doTestSIGMETStringSerialization(final String jsonFileName, final String iwxxmFilename) throws Exception {
        assertSIGMETSerialization(jsonFileName, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING)
                .isSuccessful()
                .hasXmlMessageEqualTo(readResourceToString(iwxxmFilename));
    }

    private void doTestSIGMETStringSerializationNoCoords(final String jsonFileName, final String iwxxmFilename) throws Exception {
        assertSIGMETSerialization(jsonFileName, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING)
                .hasIssueContaining("VolcanicAshSIGMET-6")
                .hasXmlMessageEqualTo(readResourceToString(iwxxmFilename));
    }

    @Test
    public void testVaSigmet() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1.json", "vasigmet1.IWXXM30");
    }

    @Test
    public void testVaSigmet_NoCoords() throws Exception {
        doTestSIGMETStringSerializationNoCoords("vasigmet1_nocoords.json", "vasigmet1_nocoords.IWXXM30");
    }

    @Test
    public void testVaSigmet_NoName() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_noname.json", "vasigmet1_noname.IWXXM30");
    }

    @Test
    public void testVaSigmetCancel() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_cancel.json", "vasigmet1_cancel.IWXXM30");
    }

    @Test
    public void testVaSigmetCancelMovToFir() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_cancel_movtofir.json", "vasigmet1_cancel_movtofir.IWXXM30");
    }


}
