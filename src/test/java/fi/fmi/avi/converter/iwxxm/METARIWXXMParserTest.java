package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.metar.METAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class METARIWXXMParserTest extends DOMParsingTestBase {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testStringParser() throws Exception {
        InputStream is = METARIWXXMParserTest.class.getResourceAsStream("metar-A3-1.xml");
        Objects.requireNonNull(is);
        String input = IOUtils.toString(is,"UTF-8");
        is.close();
        ConversionResult<METAR> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidMETAR_A3() throws Exception {
        Document toValidate = readDocument("metar-A3-1.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidMETAR_RVS() throws Exception {
        Document toValidate = readDocument("metar-EDDF-runwaystate.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidMETAR_NIL() throws Exception {
        Document toValidate = readDocument("metar-NIL.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
    }
}
