package fi.fmi.avi.converter.iwxxm.v3_0;

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

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class VASIGMETIWWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

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

    public void testSIGMETStringSerialization(final String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING));
        final SIGMET s = readFromJSON(fn, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING);
        assertConversionResult(result).isSuccessful();
    }

    public void doTestSIGMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING));
        final SIGMET s = readFromJSON(fn, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString(iwxxmFn));
    }

    public void doTestSIGMETStringSerializationNoCoords(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING));
        final SIGMET s = readFromJSON(fn, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING);
        assertConversionResult(result).hasXmlEqualing(readResourceToString(iwxxmFn));
    }

}
