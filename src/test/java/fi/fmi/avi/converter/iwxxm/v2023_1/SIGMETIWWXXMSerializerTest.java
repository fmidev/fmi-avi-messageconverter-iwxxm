package fi.fmi.avi.converter.iwxxm.v2023_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.assertXMLEqualsIgnoringVariables;
import static junit.framework.TestCase.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SIGMETIWWXXMSerializerTest {
    @Autowired
    private AviMessageConverter converter;

    protected SIGMET readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = this.getClass().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, SIGMETImpl.class);
            } else {
                throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
            }
        }
    }

    protected String readFromFile(final String fileName) throws IOException {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(fileName).toURI())));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }

    @Test
    public void testSIGMETStringSerialization1() throws Exception {
        doTestSIGMETStringSerialization("sigmet1.json", "sigmet1.xml");
    }

    @Test
    public void testSIGMETStringSerialization2() throws Exception {
        doTestSIGMETStringSerialization("sigmet2.json", "sigmet2.xml");
    }

    @Test
    public void testSIGMETStringSerialization3() throws Exception {
        doTestSIGMETStringSerialization("sigmet3.json", "sigmet3.xml");
    }

    @Test
    public void testSIGMETStringSerializationCancel() throws Exception {
        doTestSIGMETStringSerialization("sigmet_cancel.json", "sigmet_cancel.xml");
    }

    @Test
    public void testSIGMETSTNR() throws Exception {
        // SIGMET describes stationary phenomenon, should result in an IWXXM containing
        // an empty iwxxm:directionOfMotion with nilReason "http://codes.wmo.int/common/nil/inapplicable"
        // and iwxxm:speedOfMotion of 0
        doTestSIGMETStringSerialization("sigmetSTNR.json", "sigmetSTNR.xml");
    }

    @Test
    public void testSIGMETMOVING() throws Exception {
        //SIGMET has a speed and direction, should result in IWXXM containing
        // directionOfMotion and speedOfMotion elements
        doTestSIGMETStringSerialization("sigmetMOVING.json", "sigmetMOVING.xml");
    }

    @Test
    public void testSIGMETCleanup() throws Exception {
        //Asserts the generated SIGMET is cleaned up correctly
        doTestSIGMETStringSerialization("sigmetMOVING.json", "sigmetMOVING.xml");
    }

    @Test
    public void testSIGMETForecastPosition() throws Exception {
        //SIGMET with forecast position for phenomenon
        //should result in IWXXM with no speedOfMotion or directionOfMotion elements
        doTestSIGMETStringSerialization("sigmetFORECASTPOSITION.json", "sigmetFORECASTPOSITION.xml");
    }

    @Test
    public void testSIGMETForecastPositionNoFcTime() throws Exception {
        //SIGMET with forecast position for phenomenon
        //should result in IWXXM with no speedOfMotion or directionOfMotion elements
        doTestSIGMETStringSerialization("sigmetFORECASTPOSITION_NOFCTIME.json", "sigmetFORECASTPOSITION_NOFCTIME.xml");
    }

    @Test
    public void testFL() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL.json", "sigmet_FL.xml");
    }

    @Test
    public void testM() throws Exception {
        doTestSIGMETStringSerialization("sigmet_M.json", "sigmet_M.xml");
    }

    @Test
    public void testFT() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FT.json", "sigmet_FT.xml");
    }

    @Test
    public void testSFCFL() throws Exception {
        doTestSIGMETStringSerialization("sigmet_SFC_FL.json", "sigmet_SFC_FL.xml");
    }

    @Test
    public void testTOPABV() throws Exception {
        doTestSIGMETStringSerialization("sigmet_TOPABV_FL.json", "sigmet_TOPABV_FL.xml");
    }

    @Test
    public void testTOPBLW() throws Exception {
        doTestSIGMETStringSerialization("sigmet_TOPBLW_FL.json", "sigmet_TOPBLW_FL.xml");
    }

    @Test
    public void testABV() throws Exception {
        doTestSIGMETStringSerialization("sigmet_ABV_FL.json", "sigmet_ABV_FL.xml");
    }

    @Test
    public void test_OBS_BEFORE() throws Exception {
        doTestSIGMETStringSerialization("sigmet_OBSBEFORE.json", "sigmet_OBSBEFORE.xml");
    }

    @Test
    public void test_VA_OBS_BEFORE() throws Exception {
        doTestSIGMETStringSerialization("vasigmet1_OBSBEFORE.json", "vasigmet1_OBSBEFORE.xml");
    }

    @Test
    public void testABV_NOOBSTIME() throws Exception {
        doTestSIGMETStringSerialization("sigmet_ABV_FL_NOOBSTIME.json", "sigmet_ABV_FL_NOOBSTIME.xml");
    }

    @Test
    public void testTest() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_TEST.json", "sigmet_FL_TEST.xml");
    }

    @Test
    public void testExercise() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_EXER.json", "sigmet_FL_EXER.xml");
    }

    @Test
    public void testOperational() throws Exception {
        doTestSIGMETStringSerialization("sigmet_FL_OPER.json", "sigmet_FL_OPER.xml");
    }

    @Test
    public void testRadioactiveCloudWithCircleArea() throws Exception {
        doTestSIGMETStringSerialization("sigmet_rdoact_cld.json", "sigmet_rdoact_cld.xml");
    }

    @Test
    public void testMinimalTest() throws Exception {
        doTestSIGMETStringSerialization("sigmet_minimal_test.json", "sigmet_minimal_test.xml");
    }

    @Test
    public void testMinimalVaTest() throws Exception {
        doTestSIGMETStringSerialization("sigmet_minimal_va_test.json", "sigmet_minimal_va_test.xml");
    }

    public String doTestSIGMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING));
        final SIGMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING);

        for (ConversionIssue iss : result.getConversionIssues()) {
            System.err.println("iss:" + iss.getMessage() + "===" + iss.getCause());
        }

        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        System.err.println("XML:\n" + result.getConvertedMessage().get());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        assertXMLEqualsIgnoringVariables(readFromFile(iwxxmFn), result.getConvertedMessage().get());
        return result.getConvertedMessage().orElse(null);
    }
}
