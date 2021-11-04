package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

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


    private String fixIds(String s){
        if (s==null) return null;
        return s.replaceAll("gml:id=\"(.*)\"", "gml:id=\"GMLID\"")
            .replaceAll("xlink:href=\"(.*)\"", "xlink:href=\"XLINKHREF\"")
            .replaceAll("volcanoId=\"(.*)\"", "volcanoId=\"VOLCANOID\"");
    }

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

    public String doTestSIGMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING));
        final SIGMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING);

        for (ConversionIssue iss: result.getConversionIssues()) {
            System.err.println("iss:"+iss.getMessage()+"==="+iss.getCause());
        }

        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        System.err.println("XML:\n"+result.getConvertedMessage().get());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        String expectedXml = fixIds(readFromFile(iwxxmFn));
        assertEquals(expectedXml, fixIds(result.getConvertedMessage().get()));
        return result.getConvertedMessage().orElse(null);
    }
}
