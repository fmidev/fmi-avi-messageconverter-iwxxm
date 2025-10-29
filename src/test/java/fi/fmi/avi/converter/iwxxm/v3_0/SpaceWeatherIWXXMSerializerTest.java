package fi.fmi.avi.converter.iwxxm.v3_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.immutable.SpaceWeatherAdvisoryAmd79Impl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;

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
    public void serialize_spacewx_A2_4() throws Exception {
        final String input = readResourceToString("spacewx-A2-4.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-A2-4.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A2_5() throws Exception {
        final String input = readResourceToString("spacewx-A2-5.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-A2-5.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_daylight_side() throws Exception {
        final String input = readResourceToString("spacewx-daylight-side.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-daylight-side.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_partial_daylight_side() throws Exception {
        final String input = readResourceToString("spacewx-partial-daylight-side.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-daylight-side.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_issuing_centre() throws Exception {
        final String input = readResourceToString("spacewx-issuing-centre.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(readResourceToString("spacewx-issuing-centre.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void parse_and_serialize_test_A2_3() throws Exception {
        testParseAndSerialize("spacewx-A2-3.xml");
    }

    @Test
    public void parse_and_serialize_test_A2_4() throws Exception {
        testParseAndSerialize("spacewx-A2-4.xml");
    }

    @Test
    public void parse_and_serialize_test_A2_5() throws Exception {
        testParseAndSerialize("spacewx-A2-5.xml");
    }

    @Test
    public void parse_and_serialize_test_daylight_side() throws Exception {
        testParseAndSerialize("spacewx-daylight-side.xml");
    }

    private void testParseAndSerialize(final String fileName) throws IOException, SAXException {
        final String input = readResourceToString(fileName);

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        final ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        assertEquals(Collections.emptyList(), message.getConversionIssues());
        assertEquals(ConversionResult.Status.SUCCESS, message.getStatus());
        assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(input, message.getConvertedMessage().get());
    }

    @Test
    public void parse_and_serialize_nil_remark_test() throws Exception {
        final String input = readResourceToString("spacewx-nil-remark.xml");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        assertEquals(0, result.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        assertFalse(result.getConvertedMessage().get().getRemarks().isPresent());

        final ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        assertEquals(ConversionResult.Status.SUCCESS, message.getStatus());
        assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());
        assertXMLEqualsIgnoringVariables(input, message.getConvertedMessage().get());
    }

    private ConversionResult<String> serialize(final String input) throws Exception {
        final SpaceWeatherAdvisoryAmd79 swx = objectMapper.readValue(input, SpaceWeatherAdvisoryAmd79Impl.class);

        return serialize(swx);
    }

    private ConversionResult<String> serialize(final SpaceWeatherAdvisoryAmd79 swx) {

        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING));
        final ConversionResult<String> message = converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING);
        printIssues(message.getConversionIssues());
        return message;
    }

}
