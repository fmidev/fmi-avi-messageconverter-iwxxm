package fi.fmi.avi.converter.iwxxm.v2_1;

import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class VASIGMETIWWXXMSerializerTest {
    @Autowired
    private AviMessageConverter converter;

    private SIGMET getSIGMET() throws IOException {
        return readFromJSON("vasigmet1.json");
    }

    protected SIGMET readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = VASIGMETIWWXXMSerializerTest.class.getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, SIGMETImpl.class);
            } else {
                throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
            }
        }
    }

    @Test
    public void testSIGMETStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING));
        final SIGMET s = getSIGMET();
        final ConversionResult<String> result = converter.convertMessage(s, IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
    }
}
