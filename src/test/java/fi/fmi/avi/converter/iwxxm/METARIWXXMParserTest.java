package fi.fmi.avi.converter.iwxxm;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.ObservedCloudLayer;
import fi.fmi.avi.model.metar.ObservedClouds;

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

    @Test
    public void testRecentWeather() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-recent-weather.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testWindShear() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-wind-shear.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testSeaState() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-sea-state.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testSnowClosure() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-snow-closure.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesCavokConflicts() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-cavok-conflicts.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertFalse(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().contains("Schema validation issue")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("RVR")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("VISIBILITY")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("CLOUD")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("WEATHER")));
    }

    @Test
    public void testTrendSurfaceWindForecast() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-trend-wind.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesTrendCavokConflicts() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-trend-cavok-conflicts.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertFalse(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().contains("Schema validation issue")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("VISIBILITY")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("CLOUD")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase().contains("WEATHER")));
    }

    @Test
    public void testNoCloudsDetectedForecast() throws Exception {
        Document toValidate = readDocument("metar-A3-1_with-ncd.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testMissingCloudObservations() throws Exception {
        //have to split in two documents, max no. of layers per METAR obs is 4

        Document toValidate = readDocument("metar-A3-1_with-missing-cloud-obs.xml");
        ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        Optional<ObservedClouds> clouds = m.get().getClouds();
        assertTrue(clouds.isPresent());
        Optional<List<ObservedCloudLayer>> layers = clouds.get().getLayers();
        assertTrue(layers.isPresent());
        assertEquals(4, layers.get().size());
        ObservedCloudLayer layer = layers.get().get(0);
        assertFalse(layer.getBase().isPresent());
        assertFalse(layer.getAmount().isPresent());
        assertTrue(layer.isAmountUnobservableByAutoSystem());
        assertTrue(layer.isHeightUnobservableByAutoSystem());

        layer = layers.get().get(1);
        assertFalse(layer.getBase().isPresent());
        assertFalse(layer.getAmount().isPresent());
        assertTrue(layer.isAmountNotDetectedByAutoSystem());
        assertTrue(layer.isHeightNotDetectedByAutoSystem());

        layer = layers.get().get(2);
        assertFalse(layer.getAmount().isPresent());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertTrue(Math.abs(layer.getBase().get().getValue() - 1500) < 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertTrue(layer.isAmountUnobservableByAutoSystem());

        layer = layers.get().get(3);
        assertFalse(layer.getAmount().isPresent());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertTrue(Math.abs(layer.getBase().get().getValue() - 1500) < 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertTrue(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());


        toValidate = readDocument("metar-A3-1_with-missing-cloud-obs2.xml");
        result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        clouds = m.get().getClouds();
        assertTrue(clouds.isPresent());
        layers = clouds.get().getLayers();
        assertTrue(layers.isPresent());
        assertEquals(4, layers.get().size());
        layer = layers.get().get(0);
        assertTrue(layer.getAmount().isPresent());
        assertEquals(AviationCodeListUser.CloudAmount.SCT, layer.getAmount().get());
        assertFalse(layer.getBase().isPresent());
        assertTrue(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());

        layer = layers.get().get(1);
        assertTrue(layer.getAmount().isPresent());
        assertEquals(AviationCodeListUser.CloudAmount.SCT, layer.getAmount().get());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertTrue(Math.abs(layer.getBase().get().getValue() - 1500) < 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());

        layer = layers.get().get(2);
        assertTrue(layer.getAmount().isPresent());
        assertEquals(AviationCodeListUser.CloudAmount.SCT, layer.getAmount().get());
        assertFalse(layer.getBase().isPresent());
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertTrue(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());

        layer = layers.get().get(3);
        assertTrue(layer.getAmount().isPresent());
        assertEquals(AviationCodeListUser.CloudAmount.BKN_OVC, layer.getAmount().get());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertTrue(Math.abs(layer.getBase().get().getValue() - 1500) < 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertTrue(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());
    }

    //TODO: RWS with snow closure conflicts
    //TODO: RWS with all runways flag
    //TODO: RWS with cleared flag conflicts
    //TODO: RWS with depth of deposit
    //TODO: RWS with braking action special values 99 and 127



}
