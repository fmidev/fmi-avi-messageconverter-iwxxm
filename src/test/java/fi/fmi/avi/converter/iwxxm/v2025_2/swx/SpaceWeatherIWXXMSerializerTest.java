package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

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

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.junit.Assert.assertTrue;

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
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-A2-3.xml"));
    }

    @Test
    public void serialize_spacewx_A7_3() throws Exception {
        final String input = readResourceToString("spacewx-A7-3.json");
        final ConversionResult<String> result = serialize(input);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-A7-3.xml"));
    }

    @Test
    public void serialize_spacewx_A7_5() throws Exception {
        final String input = readResourceToString("spacewx-A7-5.json");
        final ConversionResult<String> result = serialize(input);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-A7-5.xml"));
    }

    @Test
    public void serialize_spacewx_repeated_regions() throws Exception {
        final String input = readResourceToString("spacewx-repeated-regions.json");
        final ConversionResult<String> result = serialize(input);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-repeated-regions.xml"));
    }

    @Test
    public void serialize_rounded_polygon_coordinates() throws Exception {
        final String input = readResourceToString("spacewx-polygon-coordinate-rounding.json");
        final ConversionResult<String> result = serialize(input);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-polygon-coordinate-rounding.xml"));
    }

    @Test
    public void serialize_nil_reasons() throws Exception {
        final String input = readResourceToString("spacewx-nil-reasons.json");
        final ConversionResult<String> result = serialize(input);
        assertConversionResult(result).assertSuccessful().hasXmlEqualing(readResourceToString("spacewx-nil-reasons.xml"));
    }

    @Test
    public void serialize_too_many_replace_nrs() throws Exception {
        final String input = readResourceToString("spacewx-too-many-replace-nrs.json");
        final ConversionResult<String> result = serialize(input);

        assertConversionResult(result)
                .hasStatus(ConversionResult.Status.WITH_ERRORS)
                .hasXmlEqualing(readResourceToString("spacewx-too-many-replace-nrs.xml"));
    }

    private ConversionResult<String> serialize(final String input) throws Exception {
        final SpaceWeatherAdvisoryAmd82 swx = objectMapper.readValue(input, SpaceWeatherAdvisoryAmd82Impl.class);
        return serialize(swx);
    }

    private ConversionResult<String> serialize(final SpaceWeatherAdvisoryAmd82 swx) {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_STRING));
        return converter.convertMessage(swx, IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_STRING);
    }

}
