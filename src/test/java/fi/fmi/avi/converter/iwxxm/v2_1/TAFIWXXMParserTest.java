package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.taf.TAF;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testStringParser() throws Exception {
        final String input = readResourceToString("taf-A5-1.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).isSuccessful();
    }

    @Test
    public void testNoIssuesWithValidTAF() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-A5-1.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).isSuccessful();
    }

    @Test
    public void testCatchesNoIssueTime() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-no-issue-time.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("issueTime");
    }

    @Test
    public void testCatchesNonExistentTimeReference() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-non-existent-basefct-phenomenonTime-ref.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("phenomenon time");
    }

    @Test
    public void testCatchesNoPointFOI() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-basefct-no-foi-position-shape.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("position");
    }

    @Test
    public void testCatchesWrongForecastRecordType() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-wrong-basefct-type.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("observation-type");
    }

    @Test
    public void testCatchesWrongObservedProperty() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-wrong-obsproperty.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("Change forecast end time");
    }

    @Test
    public void testCatchesIllegalWeatherCode() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-illegal-weather-code.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssueContaining("weather");
    }

    @Test
    public void testCancelledTAFParsing() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-A5-2.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).isSuccessful();
    }

    @Test
    public void testTAFParsingWithTemperature() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-with_temperature_fct.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();
        assertTrue(taf.getBaseForecast().get().getTemperatures().isPresent());
    }

    @Test
    public void testTAFParsingWithARP() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-with_airport_ARP.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();
        assertFalse(taf.getAerodrome().getFieldElevationValue().isPresent());
        assertTrue(taf.getAerodrome().getReferencePoint().get().getElevationValue().isPresent());
    }

    @Test
    public void testTAFParsingWithFieldElevationUomFt() throws Exception {
        final Document toValidate = readDocumentFromResource("taf-field-elevation-uom-ft.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();
        assertTrue(taf.getAerodrome().getFieldElevationValue().isPresent());
    }

    @Test
    public void testCAVOKTrue() throws Exception {
        final String input = readResourceToString("taf-cavok.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertTrue(taf.getBaseForecast().isPresent());
        assertTrue(taf.getBaseForecast().get().isCeilingAndVisibilityOk());

        assertTrue(taf.getChangeForecasts().isPresent());
        assertTrue(taf.getChangeForecasts().get().get(1).isCeilingAndVisibilityOk());
    }

}
