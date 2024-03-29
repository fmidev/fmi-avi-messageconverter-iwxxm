package fi.fmi.avi.converter.iwxxm.v2_1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.DOMParsingTestBase;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest extends DOMParsingTestBase {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testStringParser() throws Exception {
        final InputStream is = TAFIWXXMParserTest.class.getResourceAsStream("taf-A5-1.xml");
        Objects.requireNonNull(is);
        final String input = IOUtils.toString(is, "UTF-8");
        is.close();
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidTAF() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-A5-1.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesNoIssueTime() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-no-issue-time.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesNonExistentTimeReference() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-non-existent-basefct-phenomenonTime-ref.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesNoPointFOI() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-basefct-no-foi-position-shape.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesWrongForecastRecordType() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-wrong-basefct-type.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesWrongObservedProperty() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-wrong-obsproperty.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesIllegalWeatherCode() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-illegal-weather-code.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCancelledTAFParsing() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-A5-2.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConversionIssues().isEmpty());
    }

    @Test
    public void testTAFParsingWithTemperature() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-with_temperature_fct.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConversionIssues().isEmpty());
        assertTrue(result.getConvertedMessage().get().getBaseForecast().get().getTemperatures().isPresent());
    }

    @Test
    public void testTAFParsingWithARP() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-with_airport_ARP.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConversionIssues().isEmpty());
        assertFalse(result.getConvertedMessage().get().getAerodrome().getFieldElevationValue().isPresent());
        assertTrue(result.getConvertedMessage().get().getAerodrome().getReferencePoint().get().getElevationValue().isPresent());
    }

    @Test
    public void testTAFParsingWithFieldElevationUomFt() throws Exception {
        final Document toValidate = readDocument(TAFIWXXMParserTest.class, "taf-field-elevation-uom-ft.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConversionIssues().isEmpty());
        assertTrue(result.getConvertedMessage().get().getAerodrome().getFieldElevationValue().isPresent());
    }

    @Test
    public void testCAVOKTrue() throws Exception {
        final InputStream is = TAFIWXXMParserTest.class.getResourceAsStream("taf-cavok.xml");
        Objects.requireNonNull(is);
        final String input = IOUtils.toString(is, "UTF-8");
        is.close();
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        TAF taf = result.getConvertedMessage().get();

        assertTrue(taf.getBaseForecast().isPresent());
        assertTrue(taf.getBaseForecast().get().isCeilingAndVisibilityOk());

        assertTrue(taf.getChangeForecasts().isPresent());
        assertTrue(taf.getChangeForecasts().get().get(1).isCeilingAndVisibilityOk());
    }

}
