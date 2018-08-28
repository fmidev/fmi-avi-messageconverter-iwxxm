package fi.fmi.avi.converter.iwxxm;

import static junit.framework.TestCase.assertFalse;
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
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.metar.METAR;

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

    @Test
    public void testNOSIG() throws Exception {
        Document toValidate = readDocument("metar-A3-1_NOSIG.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesWrongObservationTypes() throws Exception {
        Document toValidate = readDocument("metar-A3-1_invalid-obs-types.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("Invalid observation type")).count() == 2);
    }

    @Test
    public void testCatchesWrongObservedPropertyRefs() throws Exception {
        Document toValidate = readDocument("metar-A3-1_invalid-obs-properties.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("Invalid observed property")).count() == 2);
    }

    @Test
    public void testCatchesMissingPhenomenonTime() throws Exception {
        Document toValidate = readDocument("metar-A3-1_no-phenomenon-time.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("METAR observation phenomenonTime")).count() == 1);
    }

    //TODO: recent weather
    //TODO: wind shear
    //TODO: sea state
    //TODO: snow closure (RWS)
    //TODO: CAVOK with conflicting data
    //TODO: trend wind
    //TODO: trend CAVOK with conflicts
    //TODO: trend cloud (with NSC)
    //TODO: RWS with snow closure conflicts
    //TODO: RWS with all runways flag
    //TODO: RWS with cleared flag conflicts
    //TODO: RWS with depth of deposit
    //TODO: RWS with braking action special values 99 and 127
    //TODO: observed cloud with nilReasons
    //TODO: trend cloud



}
