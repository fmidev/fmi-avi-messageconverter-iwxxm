package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import com.google.common.base.Preconditions;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testStringParser() throws Exception {
        InputStream is = TAFIWXXMParserTest.class.getResourceAsStream("taf-A5-1.xml");
        Preconditions.checkNotNull(is);
        String input = IOUtils.toString(is,"UTF-8");
        is.close();
        ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidTAF() throws Exception {
        Document toValidate = readDocument("taf-A5-1.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesNoIssueTime() throws Exception {
        Document toValidate = readDocument("taf-no-issue-time.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesNonExistentTimeReference() throws Exception {
        Document toValidate = readDocument("taf-non-existent-basefct-phenomenonTime-ref.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesNoPointFOI() throws Exception {
        Document toValidate = readDocument("taf-basefct-no-foi-position-shape.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesWrongForecastRecordType() throws Exception {
        Document toValidate = readDocument("taf-wrong-basefct-type.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesWrongObservedProperty() throws Exception {
        Document toValidate = readDocument("taf-wrong-obsproperty.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCatchesIllegalWeatherCode() throws Exception {
        Document toValidate = readDocument("taf-illegal-weather-code.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testCancelledTAFParsing() throws Exception {
        Document toValidate = readDocument("taf-A5-2.xml");
        ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        assertTrue(result.getConversionIssues().isEmpty());
    }



    private Document readDocument(final String name) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(TAFIWXXMParserTest.class.getResourceAsStream(name));
    }

}
