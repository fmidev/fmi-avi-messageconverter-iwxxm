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

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SIGMETIWWXXMSerializerTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;


    @Test
    public void testSIGMETStringSerialization1() throws Exception {
        doTestSIGMETStringSerialization("sigmet1.json");
    }

    @Test
    public void testSIGMETStringSerialization2() throws Exception {
        doTestSIGMETStringSerialization("sigmet2.json");
    }

    @Test
    public void testSIGMETStringSerialization3() throws Exception {
        doTestSIGMETStringSerialization("sigmet3.json");
    }

    @Test
    public void testSIGMETSTNR() throws Exception {
        //SIGMET describes stationary phenomenon, should result in an IWXXM containing
        //<directionOfMotion uom="deg" xsi:nil="true" nilReason="http://.."/> and
        //<speedOfMotion uom="[kt_i]">0.0</speedOfMotion>
        doTestSIGMETStringSerialization("sigmetSTNR.json");
    }

    @Test
    public void testSIGMETMOVING() throws Exception {
        //SIGMET has a speed and direction, should result in IWXXM containing
        // directionOfMotion and speedOfMotion elements
        doTestSIGMETStringSerialization("sigmetMOVING.json");
    }

    @Test
    public void testSIGMETCleanup() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING));
        final SIGMET s = readFromJSON("sigmetMOVING.json", SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("sigmetMOVING.IWXXM21"));
    }

    @Test
    public void testSIGMETForecastPosition() throws Exception {
        //SIGMET with forecast position for phenomenon
        //should result in IWXXM with no speedOfMotion or directionOfMotion elements
        doTestSIGMETStringSerialization("sigmetFORECASTPOSITION.json");
    }

    public String doTestSIGMETStringSerialization(final String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING));
        final SIGMET s = readFromJSON(fn, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING);
        return assertConversionResult(result).isSuccessful();
    }
}
