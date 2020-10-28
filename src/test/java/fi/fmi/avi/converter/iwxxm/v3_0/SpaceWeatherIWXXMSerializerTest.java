package fi.fmi.avi.converter.iwxxm.v3_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMSerializerTest {

    private static final Pattern UUID_DIFFERENCE_PATTERN = Pattern.compile(
            "(((Expected\\sattribute\\svalue\\s)?(\\sbut\\swas\\s)?)('#?uuid.(([a-z0-9]*)-?){5}')){2}");
    private static final Pattern COORDINATE_FORMATTING_DIFFERENCE_PATTERN = Pattern.compile(
            "(((Expected\\stext\\svalue\\s)?(\\sbut\\swas\\s)?)('([\\-0-9.]*[\\s]?){10}')){2}");
    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper OBJECT_MAPPER;

    private void assertEqualsXML(final String input, final String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        final Diff xmlDiff = new Diff(input, actual);
        final DetailedDiff detailedDiff = new DetailedDiff(xmlDiff);

        @SuppressWarnings("unchecked")
        final String diffMessage = ((List<Difference>) detailedDiff.getAllDifferences()).stream()//
                .filter(difference -> !UUID_DIFFERENCE_PATTERN.matcher(difference.toString()).find() //
                        && !COORDINATE_FORMATTING_DIFFERENCE_PATTERN.matcher(difference.toString()).find())//
                .map(difference -> difference.getDescription() + "\n" + difference + "\n")//
                .collect(Collectors.joining("------------------------------------------------\n"));
        assertEquals("", diffMessage);
    }

    @Before
    public void setup() {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    private String getInput(final String fileName) throws IOException {
        try (InputStream is = SpaceWeatherIWXXMSerializerTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void serialize_spacewx_A2_3() throws Exception {
        final String input = getInput("spacewx-A2-3.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertEqualsXML(getInput("spacewx-A2-3.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A2_4() throws Exception {
        final String input = getInput("spacewx-A2-4.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertEqualsXML(getInput("spacewx-A2-4.xml"), result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A2_5() throws Exception {
        final String input = getInput("spacewx-A2-5.json");
        final ConversionResult<String> result = serialize(input);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
        assertEqualsXML(getInput("spacewx-A2-5.xml"), result.getConvertedMessage().get());
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

    private void testParseAndSerialize(final String fileName) throws IOException, SAXException {
        final String input = getInput(fileName);

        final ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        final ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        assertEquals(ConversionResult.Status.SUCCESS, message.getStatus());
        assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());
        assertEqualsXML(input, message.getConvertedMessage().get());
    }

    @Test
    public void parse_and_serialize_nil_remark_test() throws Exception {
        final String input = getInput("spacewx-nil-remark.xml");

        final ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        assertEquals(0, result.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        assertFalse(result.getConvertedMessage().get().getRemarks().isPresent());

        final ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        assertEquals(ConversionResult.Status.SUCCESS, message.getStatus());
        assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());
        assertEqualsXML(input, message.getConvertedMessage().get());
    }

    private ConversionResult<String> serialize(final String input) throws Exception {
        final SpaceWeatherAdvisory swx = OBJECT_MAPPER.readValue(input, SpaceWeatherAdvisoryImpl.class);

        return serialize(swx);
    }

    private ConversionResult<String> serialize(final SpaceWeatherAdvisory swx) {

        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING));
        final ConversionResult<String> message = converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING);
        printIssues(message.getConversionIssues());
        return message;
    }

    private void printIssues(final List<ConversionIssue> issues) {
        if (issues.size() > 0) {
            for (final ConversionIssue item : issues) {
                System.out.println("********************************************************");
                System.out.println(item.getMessage());
            }
        }
    }

}
