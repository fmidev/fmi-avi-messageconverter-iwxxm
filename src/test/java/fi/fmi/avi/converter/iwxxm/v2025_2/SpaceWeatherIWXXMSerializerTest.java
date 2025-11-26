package fi.fmi.avi.converter.iwxxm.v2025_2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.assertXMLEqualsIgnoringVariables;
import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.printIssues;
import static org.junit.Assert.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    @Test
    public void serialize_spacewx_A2_3() throws Exception {
        final String input = readResourceToString("spacewx-A2-3.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-A2-3.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A7_3() throws Exception {
        final String input = readResourceToString("spacewx-A7-3.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-A7-3.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_repeated_regions() throws Exception {
        final String input = readResourceToString("spacewx-repeated-regions.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-repeated-regions.xml"), result.getConvertedMessage().get());
    }

    private ConversionResult<String> serialize(final String input) throws Exception {
        final SpaceWeatherAdvisoryAmd82 swx = objectMapper.readValue(input, SpaceWeatherAdvisoryAmd82Impl.class);
        return serialize(swx);
    }

    private ConversionResult<String> serialize(final SpaceWeatherAdvisoryAmd82 swx) {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_STRING));
        final ConversionResult<String> message = converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_STRING);
        printIssues(message.getConversionIssues());
        return message;
    }

}
