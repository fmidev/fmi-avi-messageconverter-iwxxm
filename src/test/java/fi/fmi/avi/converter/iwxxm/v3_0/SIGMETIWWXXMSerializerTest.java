package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SIGMETIWWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;


    @Test
    public void testSIGMETStringSerialization1() throws Exception {
        doTestSIGMETStringSerialization("sigmet1.json", "sigmet1.IWXXM30");
    }

    @Test
    public void testSIGMETStringSerialization2() throws Exception {
        doTestSIGMETStringSerialization("sigmet2.json", "sigmet2.IWXXM30");
    }

    @Test
    public void testSIGMETStringSerialization3() throws Exception {
        doTestSIGMETStringSerialization("sigmet3.json", "sigmet3.IWXXM30");
    }

    @Test
    public void testSIGMETStringSerializationCancel() throws Exception {
        doTestSIGMETStringSerialization("sigmet_cancel.json", "sigmet_cancel.IWXXM30");
    }

    @Test
    public void testSIGMETSTNR() throws Exception {
        //SIGMET describes stationary phenomenon, should result in an IWXXM containing
        //no <directionOfMotion> and <speedOfMotion> elements
        doTestSIGMETStringSerialization("sigmetSTNR.json", "sigmetSTNR.IWXXM30");
    }

    @Test
    public void testSIGMETMOVING() throws Exception {
        //SIGMET has a speed and direction, should result in IWXXM containing
        // directionOfMotion and speedOfMotion elements
        doTestSIGMETStringSerialization("sigmetMOVING.json", "sigmetMOVING.IWXXM30");
    }

    @Test
    public void testSIGMETCleanup() throws Exception {
        //Asserts the generated SIGMET is cleaned up correctly
        doTestSIGMETStringSerialization("sigmetMOVING.json", "sigmetMOVING.IWXXM30");
    }

    @Test
    public void testSIGMETForecastPosition() throws Exception {
        //SIGMET with forecast position for phenomenon
        //should result in IWXXM with no speedOfMotion or directionOfMotion elements
        doTestSIGMETStringSerialization("sigmetFORECASTPOSITION.json", "sigmetFORECASTPOSITION.IWXXM30");
    }

    @Test
    public void testSIGMETForecastPositionNoFcTime() throws Exception {
        //SIGMET with forecast position for phenomenon
        //should result in IWXXM with no speedOfMotion or directionOfMotion elements
        doTestSIGMETStringSerialization("sigmetFORECASTPOSITION_NOFCTIME.json", "sigmetFORECASTPOSITION_NOFCTIME.IWXXM30");
    }

    @Test
    public void testFL() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL.json", "sigmet_FL.IWXXM30");
    }

    @Test
    public void testM() throws Exception {
        doTestSIGMETStringSerialization("sigmet_M.json", "sigmet_M.IWXXM30");
    }

    @Test
    public void testFT() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FT.json", "sigmet_FT.IWXXM30");
    }

    @Test
    public void testSFCFL() throws Exception {
        doTestSIGMETStringSerialization("sigmet_SFC_FL.json", "sigmet_SFC_FL.IWXXM30");
    }

    @Test
    public void testTOPABV() throws Exception {
        doTestSIGMETStringSerialization("sigmet_TOPABV_FL.json", "sigmet_TOPABV_FL.IWXXM30");
    }

    @Test
    public void testTOPBLW() throws Exception {
        doTestSIGMETStringSerialization("sigmet_TOPBLW_FL.json", "sigmet_TOPBLW_FL.IWXXM30");
    }

    @Test
    public void testABV() throws Exception {
        doTestSIGMETStringSerialization("sigmet_ABV_FL.json", "sigmet_ABV_FL.IWXXM30");
    }

    @Test
    public void test_OBS_BEFORE() throws Exception {
        doTestSIGMETStringSerialization("sigmet_OBSBEFORE.json", "sigmet_OBSBEFORE.IWXXM30");
    }

    @Test
    public void test_VA_OBS_BEFORE() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_OBSBEFORE.json", "vasigmet1_OBSBEFORE.IWXXM30");
    }

    @Test
    public void testABV_NOOBSTIME() throws Exception {
        doTestSIGMETStringSerialization("sigmet_ABV_FL_NOOBSTIME.json", "sigmet_ABV_FL_NOOBSTIME.IWXXM30");
    }

    @Test
    public void testTest() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_TEST.json", "sigmet_FL_TEST.IWXXM30");
    }

    @Test
    public void testExercise() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_EXER.json", "sigmet_FL_EXER.IWXXM30");
    }

    @Test
    public void testOperational() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_OPER.json", "sigmet_FL_OPER.IWXXM30");
    }

    @Test
    @Ignore("Test sigmets without meteorological information cannot be represented in IWXXM 3.0")
    public void testMinimalTest() throws Exception {
        doTestSIGMETStringSerialization("sigmet_minimal_test.json", "sigmet_minimal_test.IWXXM30");
    }

    private void doTestSIGMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING)).isTrue();
        final SIGMET s = readFromJSON(fn, SIGMETImpl.class);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString(iwxxmFn));
    }
}
