package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertNotNull;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

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
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper OBJECT_MAPPER;

    @Before
    public void setup() {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    private String getInput(String fileName) throws IOException {
        InputStream is = null;
        try {
            is = SpaceWeatherIWXXMSerializerTest.class.getResourceAsStream(fileName);
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void serialize_spacewx_A2_3() throws Exception {
        String input = getInput("spacewx-A2-3.json");
        ConversionResult<String> result = serialize(input);

        TestCase.assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        TestCase.assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A2_4() throws Exception {
        String input = getInput("spacewx-A2-4.json");
        ConversionResult<String> result = serialize(input);

        TestCase.assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        TestCase.assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void serialize_spacewx_A2_5() throws Exception {
        String input = getInput("spacewx-A2-5.json");
        ConversionResult<String> result = serialize(input);

        TestCase.assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        TestCase.assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void parse_and_serialize_test() throws Exception {
        String input = getInput("spacewx-A2-3.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        TestCase.assertTrue(ConversionResult.Status.SUCCESS == message.getStatus());
        TestCase.assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());

        XMLUnit.setIgnoreWhitespace(true);
        Diff xmlDiff = new Diff(input, message.getConvertedMessage().get());
        DetailedDiff detailedDiff = new DetailedDiff(xmlDiff);

        List filteredDiff =
                (List)detailedDiff.getAllDifferences().stream().filter(d -> filterUUIDDifferences(d)).filter(d -> filterCoordinateFormattingDifferences(d))
                        .collect(Collectors.toList());

        for(Object item : filteredDiff) {
            Difference difference = (Difference) item;
            System.out.println("------------------------------------------------");
            System.out.println(difference.getDescription());
            System.out.println(difference);
            System.out.println("------------------------------------------------");
        }
        Assert.assertEquals(0, filteredDiff.size());
    }

    private boolean filterUUIDDifferences(Object object) {
        Difference d = (Difference) object;
        return !Pattern.compile("(((Expected\\sattribute\\svalue\\s)?(\\sbut\\swas\\s)?){1}(\\'#?uuid.(([a-z0-9]*)-?){5}\\')){2}").matcher(d.toString()).find();
    }

    private boolean filterCoordinateFormattingDifferences(Object object) {
        Difference d = (Difference) object;
        return !Pattern.compile("(((Expected\\stext\\svalue\\s)?(\\sbut\\swas\\s)?)(\\'([\\-0-9\\.]*[\\s]?){10}\\')){2}").matcher(d.toString()).find();
    }

    private ConversionResult<String> serialize(String input) throws Exception {
        SpaceWeatherAdvisory swx = OBJECT_MAPPER.readValue(input, SpaceWeatherAdvisoryImpl.class);

        return serialize(swx);
    }

    private ConversionResult<String> serialize(SpaceWeatherAdvisory swx) throws Exception {


        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING));
        ConversionResult<String> message = converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING);
        printIssues(message.getConversionIssues());
        return message;
    }

    private void printIssues(List<ConversionIssue> issues) {
        if (issues.size() > 0) {
            for (ConversionIssue item : issues) {
                System.out.println("********************************************************");
                System.out.println(item.getMessage());
            }
        }
    }

}
