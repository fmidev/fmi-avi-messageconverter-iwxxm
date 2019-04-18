package fi.fmi.avi.converter.iwxxm;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
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
        AIRMET s= readFromJSON(fn);
        ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING);
        System.err.println("STATUS: "+result.getStatus());
        if (result.getConvertedMessage().isPresent()) {
            System.err.println("AIRMET:"+result.getConvertedMessage().get());
        }

        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    public void doTestAIRMETDOMSerialization(String fn) throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM));
        AIRMET s= readFromJSON(fn);
        ConversionResult<Document> result = converter.convertMessage(s, IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM);
        System.err.println("STATUS: "+result.getStatus());
        if (result.getConvertedMessage().isPresent()) {
            System.err.println("AIRMET:"+result.getConvertedMessage().get());
        }

        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void dotestAIRMETStringSerialization1() throws Exception {
      doTestAIRMETStringSerialization("airmet_iwxxm1.json");
    }

    @Test
    public void dotestAIRMETStringSerialization2() throws Exception {
        doTestAIRMETStringSerialization("airmet_iwxxmmoving.json");
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
        doTestAIRMETDOMSerialization("airmet_iwxxmmoving.json");
    }

//    @Test
    public void dotestAIRMETDOMSerialization3() throws Exception {
        doTestAIRMETDOMSerialization("airmet2.json");
    }

}
