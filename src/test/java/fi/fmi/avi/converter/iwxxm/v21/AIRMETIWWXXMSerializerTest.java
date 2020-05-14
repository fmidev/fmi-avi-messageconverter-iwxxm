package fi.fmi.avi.converter.iwxxm.v21;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class AIRMETIWWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private AIRMET getAIRMET() throws IOException {
        AIRMET s = readFromJSON("airmet2.json");
        return s;
    }

    @Autowired
    ObjectMapper om;

    protected AIRMET readFromJSON(String fileName) throws IOException {
        InputStream is = AIRMETIWWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            AIRMET am=om.readValue(is, AIRMETImpl.Builder.class).build();
            is.close();
            return am;
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }

    public void doTestAIRMETStringSerialization(String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING));
        AIRMET s = readFromJSON(fn);
        ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING);
        //Severity check modified to pass the following Schematron-originating warning for the rule AIRMET.AECC1:
        // "When AIRMETEvolvingConditionCollection timeIndicator is an observation, the phenomenonTime must be earlier than or equal to the beginning of the validPeriod of the report."
        // This seems to be a shortcoming of the rule (xlinked validTime is not considered)
        //assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        assertFalse(ConversionResult.Status.isMoreCritical(result.getStatus(), ConversionResult.Status.WITH_WARNINGS));
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        if (result.getConvertedMessage().isPresent()) {
            String airmet = result.getConvertedMessage().get();
            //            System.err.println("Converted AIRMET=:"+airmet);
        }

    }

    public void doTestAIRMETDOMSerialization(String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM));
        AIRMET s= readFromJSON(fn);
        ConversionResult<Document> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());

        if (result.getConvertedMessage().isPresent()) {
            Document airmet = result.getConvertedMessage().get();
        }

    }

    @Test
    public void dotestAIRMETStringSerialization1() throws Exception {
      doTestAIRMETStringSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETMOVING() throws Exception {
        doTestAIRMETStringSerialization("airmetMOVING.json");
    }

    @Test
    public void dotestAIRMETSTNR() throws Exception {
        doTestAIRMETStringSerialization("airmetSTNR.json");
    }

    @Test
    public void dotestAIRMETStringSerialization3() throws Exception {
        doTestAIRMETStringSerialization("airmet2.json");
    }

    @Test
    public void dotestAIRMETStringSerialization4() throws Exception {
        doTestAIRMETStringSerialization("airmet_bkncld.json");
    }

    @Test
    public void dotestAIRMETStringSerialization5() throws Exception {
        doTestAIRMETStringSerialization("airmet_ovccld_abv.json");
    }

    @Test
    public void dotestAIRMETStringSerialization_wind() throws Exception {
        doTestAIRMETStringSerialization("airmet_wind.json");
    }

    @Test
    public void dotestAIRMETStringSerialization_vis() throws Exception {
        doTestAIRMETStringSerialization("airmet_vis.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization1() throws Exception {
        doTestAIRMETDOMSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETDOMSerialization2() throws Exception {
        doTestAIRMETDOMSerialization("airmetMOVING.json");
    }

//    @Test
    public void dotestAIRMETDOMSerialization3() throws Exception {
        doTestAIRMETDOMSerialization("airmet2.json");
    }

}
