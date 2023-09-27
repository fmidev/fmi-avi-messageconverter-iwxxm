package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

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
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class AIRMETIWWXXMSerializerTest {

    @Autowired
    ObjectMapper om;
    @Autowired
    private AviMessageConverter converter;

    protected AIRMET readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = this.getClass().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, AIRMETImpl.class);
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

    public void doTestAIRMETDOMSerialization(final String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM30_DOM));
        final AIRMET s = readFromJSON(fn);
        final ConversionResult<Document> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM30_DOM);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        for (ConversionIssue iss: result.getConversionIssues()) {
            System.err.println("iss:" + iss);
        }
        assertTrue(result.getConvertedMessage().isPresent());
    }

    @Test
    public void dotestAIRMETStringSerialization1() throws Exception {
        String s = doTestAIRMETStringSerialization("airmet_iwxxm1.json", "airmet_iwxxm1.IWXXM30");
        System.err.println(s);
    }

    @Test
    public void dotestAIRMETMOVING() throws Exception {
        String s = doTestAIRMETStringSerialization("airmetMOVING.json", "airmetMOVING.IWXXM30");
        System.err.println(s);
    }

    private String fixIds(String s){
        if (s==null) return null;
        return s.replaceAll("gml:id=\"(.*)\"", "gml:id=\"GMLID\"").replaceAll("xlink:href=\"(.*)\"", "xlink:href=\"XLINKHREF\"");
    }

    @Test
    public void dotestAIRMETCleanup() throws Exception {
        doTestAIRMETStringSerialization("airmetMOVING.json", "airmetMOVING.IWXXM30");
    }

    @Test
    public void dotestAIRMETSTNR() throws Exception {
        doTestAIRMETStringSerialization("airmetSTNR.json", "airmetSTNR.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2.json", "airmet2.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization4() throws Exception {
        doTestAIRMETStringSerialization("airmet_bkncld.json", "airmet_bkncld.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization5() throws Exception {
        doTestAIRMETStringSerialization("airmet_ovccld.json", "airmet_ovccld.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization6() throws Exception {
        //Because IWXXM30 can not represent ovccld with ABV the result should be the same as without ABV
        doTestAIRMETStringSerialization("airmet_ovccld_abv.json", "airmet_ovccld.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization_wind() throws Exception {
        doTestAIRMETStringSerialization("airmet_wind.json", "airmet_wind.IWXXM30");
    }

    @Test
    public void dotestAIRMETStringSerialization_vis() throws Exception {
        doTestAIRMETStringSerialization("airmet_vis.json", "airmet_vis.IWXXM30");
    }

    @Test
    public void dotestAIRMET_OBS_BEFORE_StringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2_obs_before.json", "airmet2_obs_before.IWXXM30");
    }

    @Test
    public void dotestAIRMET_OPER() throws Exception {
        String s = doTestAIRMETStringSerialization("airmet_OPER.json", "airmet_OPER.IWXXM30");
        System.err.println(s);
    }

    @Test
    public void dotestAIRMET_TEST() throws Exception {
        String s = doTestAIRMETStringSerialization("airmet_TEST.json", "airmet_TEST.IWXXM30");
        System.err.println(s);
    }

    @Test
    public void dotestAIRMET_EXER() throws Exception {
        String s = doTestAIRMETStringSerialization("airmet_EXER.json", "airmet_EXER.IWXXM30");
        System.err.println(s);
    }



    @Test
    public void dotestAIRMETDOMSerialization1() throws Exception {
        doTestAIRMETDOMSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization2() throws Exception {
        doTestAIRMETDOMSerialization("airmetMOVING.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization3() throws Exception {
        doTestAIRMETDOMSerialization("airmet2.json");
    }

    public String doTestAIRMETStringSerialization(final String fn, final String iwxxmFn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM30_STRING));
        final AIRMET s = readFromJSON(fn);
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM30_STRING);

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
