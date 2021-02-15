package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMTest {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void Test1() throws Exception {

        String input = TestHelper.getXMLString("taf-A5-1.xml");
        System.out.println(input);
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO));
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING));
        final ConversionResult<TAF> stringToPojoresult = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        assertEquals(ConversionResult.Status.SUCCESS, stringToPojoresult.getStatus());
        assertTrue(stringToPojoresult.getConvertedMessage().isPresent());

        TAF tafPojo = stringToPojoresult.getConvertedMessage().get();

        final ConversionResult<String> output = converter.convertMessage(tafPojo, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING);
        assertEquals(ConversionResult.Status.SUCCESS, output.getStatus());
        assertTrue(output.getConvertedMessage().isPresent());



        System.out.println(output.getConvertedMessage().get());
    }

    @Test
    public void Test2() throws IOException, SAXException {
        testParseAndSerialize("taf-A5-1.xml");
    }


    private void testParseAndSerialize(final String fileName) throws IOException, SAXException {
        final String input = TestHelper.getXMLString(fileName);

        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        final ConversionResult<String> message = serialize(result.getConvertedMessage().get());

        Assert.assertEquals(Collections.emptyList(), message.getConversionIssues());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, message.getStatus());
        Assert.assertTrue(message.getConvertedMessage().isPresent());
        assertNotNull(message.getConvertedMessage().get());
        TestHelper.assertEqualsXML(input, message.getConvertedMessage().get());
    }

    private ConversionResult<String> serialize(TAF src) {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING));
        final ConversionResult<String> message = converter.convertMessage(src, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING);
        TestHelper.printIssues(message.getConversionIssues());
        return message;
    }

    /*
    * 1. Load XML as String
    * 2. Parse XML String
    * 3. Serialize POJO
    * 4. Remove Id errors
    * 5. Compare XML Strings
    * */
}
