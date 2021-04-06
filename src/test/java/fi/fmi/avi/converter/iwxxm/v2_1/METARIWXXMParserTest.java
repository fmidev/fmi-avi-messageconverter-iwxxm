package fi.fmi.avi.converter.iwxxm.v2_1;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.junit.Assert;
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
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.ObservedCloudLayer;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.SeaState;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.WindShear;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class METARIWXXMParserTest extends DOMParsingTestBase {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testStringParser() throws Exception {
        final InputStream is = METARIWXXMParserTest.class.getResourceAsStream("metar-A3-1.xml");
        Objects.requireNonNull(is);
        final String input = IOUtils.toString(is, "UTF-8");
        is.close();
        final ConversionResult<METAR> result = converter.convertMessage(input, IWXXMConverter.IWXXM21_STRING_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidMETAR_A3() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNoIssuesWithValidMETAR_RWS() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-EDDF-runwaystate.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        final Optional<List<RunwayState>> states = m.get().getRunwayStates();
        assertTrue(states.isPresent());
        assertEquals(3, states.get().size());

    }

    @Test
    public void testNoIssuesWithValidMETAR_NIL() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-NIL.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testNOSIG() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_NOSIG.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesWrongObservationTypes() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_invalid-obs-types.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertEquals(2, result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("Invalid observation type")).count());
    }

    @Test
    public void testCatchesWrongObservedPropertyRefs() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_invalid-obs-properties.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertEquals(2, result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("Invalid observed property")).count());
    }

    @Test
    public void testCatchesMissingPhenomenonTime() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_no-phenomenon-time.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertEquals(1, result.getConversionIssues().stream().filter(issue -> issue.getMessage().contains("METAR observation phenomenonTime")).count());
    }

    @Test
    public void testRecentWeather() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-recent-weather.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testWindShear() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-wind-shear.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testSeaState() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-sea-state.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testSnowClosure() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-snow-closure.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesCavokConflicts() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-cavok-conflicts.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertFalse(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().contains("Schema validation issue")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("RVR")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("VISIBILITY")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("CLOUD")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("WEATHER")));
    }

    @Test
    public void testTrendSurfaceWindForecast() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-trend-wind.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testCatchesTrendCavokConflicts() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-trend-cavok-conflicts.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertFalse(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().contains("Schema validation issue")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("VISIBILITY")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("CLOUD")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("WEATHER")));
    }

    @Test
    public void testNoCloudsDetectedForecast() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-ncd.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

    @Test
    public void testMissingCloudObservations() throws Exception {
        //have to split in two documents, max no. of layers per METAR obs is 4

        Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-missing-cloud-obs.xml");
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
        assertEquals(layer.getBase().get().getValue(), 1500, 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertTrue(layer.isAmountUnobservableByAutoSystem());

        layer = layers.get().get(3);
        assertFalse(layer.getAmount().isPresent());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertEquals(layer.getBase().get().getValue(), 1500, 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertFalse(layer.isCloudTypeUnobservableByAutoSystem());
        assertTrue(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());

        toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-missing-cloud-obs2.xml");
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
        assertEquals(layer.getBase().get().getValue(), 1500, 0.00001);
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
        assertEquals(AviationCodeListUser.CloudAmount.OVC, layer.getAmount().get());
        assertTrue(layer.getBase().isPresent());
        assertEquals(layer.getBase().get().getUom(), "[ft_i]");
        assertEquals(layer.getBase().get().getValue(), 1500, 0.00001);
        assertFalse(layer.isHeightUnobservableByAutoSystem());
        assertFalse(layer.isHeightNotDetectedByAutoSystem());
        assertTrue(layer.isCloudTypeUnobservableByAutoSystem());
        assertFalse(layer.isAmountNotDetectedByAutoSystem());
        assertFalse(layer.isAmountUnobservableByAutoSystem());
    }

    @Test
    public void testTrendCloudForecast() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-trend-cloud-and-nsc.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        final Optional<List<TrendForecast>> trends = m.get().getTrends();
        assertTrue(trends.isPresent());
        assertEquals(3, trends.get().size());
        final TrendForecast trend = trends.get().get(1);
        final Optional<CloudForecast> cloudForecast = trend.getCloud();
        assertTrue(cloudForecast.isPresent());
        assertTrue(cloudForecast.get().isNoSignificantCloud());

    }

    @Test
    public void testRunwayStateAllRunways() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-EDDF-runwaystate-all-runways.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());

        final Optional<List<RunwayState>> runwayStates = m.get().getRunwayStates();
        assertTrue(runwayStates.isPresent());
        final RunwayState rws = runwayStates.get().get(0);
        assertTrue(rws.isAppliedToAllRunways());
    }

    @Test
    public void testRunwayStateCleared() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-EDDF-runwaystate-cleared.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());

        final Optional<List<RunwayState>> runwayStates = m.get().getRunwayStates();
        assertTrue(runwayStates.isPresent());
        RunwayState rws = runwayStates.get().get(0);
        Assert.assertFalse(rws.isCleared());
        rws = runwayStates.get().get(1);
        assertTrue(rws.isCleared());
        rws = runwayStates.get().get(2);
        assertTrue(rws.isCleared());
    }

    @Test
    public void testRunwayStateClearedConflict() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-EDDF-runwaystate-cleared-conflict.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertFalse("Issues should have been found", result.getConversionIssues().isEmpty());
        assertFalse(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().contains("Schema validation issue")));
        assertTrue(result.getConversionIssues().stream().anyMatch(issue -> issue.getMessage().toUpperCase(Locale.US).contains("CLEARED")));
    }

    @Test
    public void testRunwayStateInfo() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-EDDF-runwaystate-depth-of-deposit.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        final Optional<List<RunwayState>> states = m.get().getRunwayStates();
        assertTrue(states.isPresent());
        assertEquals(7, states.get().size());

        RunwayState rws = states.get().get(0);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07R", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_LESS_THAN_10, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertFalse(rws.getBrakingAction().isPresent());

        assertTrue(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());
        assertEquals(10.1, rws.getDepthOfDeposit().get().getValue(), 0.00001);
        assertEquals("mm", rws.getDepthOfDeposit().get().getUom());

        assertTrue(rws.getEstimatedSurfaceFriction().isPresent());
        assertEquals(0.9, rws.getEstimatedSurfaceFriction().get(), 0.00001);

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertFalse(rws.isRunwayNotOperational());

        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        rws = states.get().get(1);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07C", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertFalse(rws.getBrakingAction().isPresent());

        assertTrue(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());
        assertEquals(-10.1, rws.getDepthOfDeposit().get().getValue(), 0.00001);
        assertEquals("mm", rws.getDepthOfDeposit().get().getUom());

        assertFalse(rws.getEstimatedSurfaceFriction().isPresent());
        assertTrue(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertFalse(rws.isRunwayNotOperational());

        rws = states.get().get(2);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07L", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertFalse(rws.getBrakingAction().isPresent());

        assertTrue(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());
        assertEquals(0.0, rws.getDepthOfDeposit().get().getValue(), 0.00001);
        assertEquals("mm", rws.getDepthOfDeposit().get().getUom());

        assertFalse(rws.getEstimatedSurfaceFriction().isPresent());
        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertTrue(rws.isRunwayNotOperational());

        rws = states.get().get(3);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07L2", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertTrue(rws.getBrakingAction().isPresent());
        assertEquals(AviationCodeListUser.BrakingAction.GOOD, rws.getBrakingAction().get());

        assertTrue(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());
        assertEquals(-0.0, rws.getDepthOfDeposit().get().getValue(), 0.00001);
        assertEquals("mm", rws.getDepthOfDeposit().get().getUom());

        assertFalse(rws.getEstimatedSurfaceFriction().isPresent());
        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertFalse(rws.isRunwayNotOperational());

        rws = states.get().get(4);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07L3", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertTrue(rws.getBrakingAction().isPresent());
        assertEquals(AviationCodeListUser.BrakingAction.POOR, rws.getBrakingAction().get());

        assertFalse(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertTrue(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());

        assertFalse(rws.getEstimatedSurfaceFriction().isPresent());
        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertFalse(rws.isRunwayNotOperational());

        rws = states.get().get(5);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07L4", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertFalse(rws.getBrakingAction().isPresent());

        assertFalse(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertTrue(rws.isDepthInsignificant());

        assertTrue(rws.getEstimatedSurfaceFriction().isPresent());
        assertEquals(0.0, rws.getEstimatedSurfaceFriction().get(), 0.00001);
        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertFalse(rws.isRunwayNotOperational());

        rws = states.get().get(6);
        assertTrue(rws.getRunwayDirection().isPresent());
        assertEquals("07L5", rws.getRunwayDirection().get().getDesignator());

        assertTrue(rws.getContamination().isPresent());
        assertEquals(AviationCodeListUser.RunwayContamination.PCT_COVERED_26_50, rws.getContamination().get());

        assertTrue(rws.getDeposit().isPresent());
        assertEquals(AviationCodeListUser.RunwayDeposit.DAMP, rws.getDeposit().get());

        assertFalse(rws.getBrakingAction().isPresent());

        assertFalse(rws.getDepthOfDeposit().isPresent());
        assertFalse(rws.getDepthOperator().isPresent());
        assertFalse(rws.isDepthNotMeasurable());
        assertFalse(rws.isDepthInsignificant());

        assertTrue(rws.getEstimatedSurfaceFriction().isPresent());
        assertEquals(0.98, rws.getEstimatedSurfaceFriction().get(), 0.00001);
        assertFalse(rws.isEstimatedSurfaceFrictionUnreliable());

        assertFalse(rws.isCleared());
        assertFalse(rws.isAppliedToAllRunways());

        assertFalse(rws.isRepetition());
        assertTrue(rws.isRunwayNotOperational());

    }

    @Test
    public void testSeaStateWithSignificantWaveHeight() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-sea-state-sig-wave-height.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());

        final Optional<SeaState> seaState = m.get().getSeaState();
        assertTrue(seaState.isPresent());
        final Optional<NumericMeasure> wh = seaState.get().getSignificantWaveHeight();
        assertTrue(wh.isPresent());
        assertEquals(5.5, wh.get().getValue(), 0.00001);
        assertEquals("m", wh.get().getUom());
    }

    @Test
    public void testWindShearAllRunways() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-wind-shear-all-runways.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());

        final Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());

        final Optional<WindShear> windShear = m.get().getWindShear();
        assertTrue(windShear.isPresent());

        assertTrue(windShear.get().isAppliedToAllRunways());
    }

    @Test
    public void testTrendPhenomenonTime() throws Exception {
        final Document toValidate = readDocument(METARIWXXMParserTest.class, "metar-A3-1_with-trend-time-instant.xml");
        final ConversionResult<METAR> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, ConversionHints.EMPTY);
        assertTrue("No issues should have been found", result.getConversionIssues().isEmpty());
    }

}
