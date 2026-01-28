package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.util.List;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static junit.framework.TestCase.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void normalMessageTest() throws IOException {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO));
        final String input = readResourceToString("taf-reportstatus-normal.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, taf.getReportStatus());
        assertEquals(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL, taf.getPermissibleUsage().get());
        assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST, taf.getPermissibleUsageReason().get());
        //Issue Time
        assertEquals("2012-08-15T18:00Z", taf.getIssueTime().get().getCompleteTime().get().toString());
        //Valid Time
        assertEquals("2012-08-16T00:00Z", taf.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", taf.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        //Aerodrome
        final Aerodrome aerodrome = taf.getAerodrome();
        assertEquals("YUDO", aerodrome.getDesignator());
        assertEquals("DONLON/INTERNATIONAL", aerodrome.getName().get());
        assertEquals("YUDO", aerodrome.getLocationIndicatorICAO().get());
        assertTrue(aerodrome.getReferencePoint().isPresent());
        final ElevatedPoint point = aerodrome.getReferencePoint().get();
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", point.getCrs().get().getName());
        assertEquals("Lat", point.getCrs().get().getAxisLabels().get(0));
        assertEquals("Long", point.getCrs().get().getAxisLabels().get(1));
        assertEquals(Integer.valueOf(2), point.getCrs().get().getDimension().get());
        assertEquals("M", point.getElevationUom().get());
        assertEquals(12d, point.getElevationValue().get());
        assertEquals("EGM_96", point.getVerticalDatum().get());
        assertEquals(12.34, point.getCoordinates().get(0));
        assertEquals(-12.34, point.getCoordinates().get(1));

        //Base Forecast
        assertTrue(taf.getBaseForecast().isPresent());
        final TAFBaseForecast base = taf.getBaseForecast().get();
        assertFalse(base.isCeilingAndVisibilityOk());
        assertTrue(base.isNoSignificantWeather());
        assertEquals("m", base.getPrevailingVisibility().get().getUom());
        assertEquals(9000d, base.getPrevailingVisibility().get().getValue());
        //Base Forecast Surface Wind
        assertTrue(base.getSurfaceWind().isPresent());
        SurfaceWind surfaceWind = base.getSurfaceWind().get();
        assertFalse(surfaceWind.isVariableDirection());
        assertEquals("deg", surfaceWind.getMeanWindDirection().get().getUom());
        assertEquals(130d, surfaceWind.getMeanWindDirection().get().getValue());
        assertEquals("m/s", surfaceWind.getMeanWindSpeed().getUom());
        assertEquals(5d, surfaceWind.getMeanWindSpeed().getValue());
        //Base Forecast Cloud
        assertTrue(base.getCloud().isPresent());
        CloudForecast cloud = base.getCloud().get();
        List<CloudLayer> layers = cloud.getLayers().get();
        assertEquals("BKN", layers.get(0).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(0).getBase().get().getUom());
        assertEquals(2000d, layers.get(0).getBase().get().getValue());

        //Change Forecasts
        assertTrue(taf.getChangeForecasts().isPresent());
        assertFalse(taf.getChangeForecasts().get().isEmpty());

        //Change 1
        TAFChangeForecast change = taf.getChangeForecasts().get().get(0);
        assertFalse(change.isCeilingAndVisibilityOk());
        assertEquals(AviationCodeListUser.TAFChangeIndicator.BECOMING, change.getChangeIndicator());
        assertEquals("2012-08-16T06:00Z", change.getPeriodOfChange().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T08:00Z", change.getPeriodOfChange().getEndTime().get().getCompleteTime().get().toString());
        assertTrue(change.getCloud().isPresent());
        cloud = change.getCloud().get();
        layers = cloud.getLayers().get();
        //Change1 Layer 1
        assertEquals("SCT", layers.get(0).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(0).getBase().get().getUom());
        assertEquals(1500d, layers.get(0).getBase().get().getValue());
        assertEquals("CB", layers.get(0).getCloudType().get().getCode());
        //Change1 Layer 2
        assertEquals("BKN", layers.get(1).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(1).getBase().get().getUom());
        assertEquals(2000d, layers.get(1).getBase().get().getValue());
        //assertEquals();
        //Change 2
        change = taf.getChangeForecasts().get().get(1);
        assertFalse(change.isCeilingAndVisibilityOk());
        assertEquals(AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS, change.getChangeIndicator());
        assertEquals("2012-08-16T08:00Z", change.getPeriodOfChange().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T12:00Z", change.getPeriodOfChange().getEndTime().get().getCompleteTime().get().toString());
        assertEquals("m", change.getPrevailingVisibility().get().getUom());
        assertEquals(1000d, change.getPrevailingVisibility().get().getValue());
        assertEquals("TSRA", change.getForecastWeather().get().get(0).getCode());
        assertTrue(change.getSurfaceWind().isPresent());
        //Change 2 Surface Wind
        surfaceWind = change.getSurfaceWind().get();
        assertFalse(surfaceWind.isVariableDirection());
        assertEquals("deg", surfaceWind.getMeanWindDirection().get().getUom());
        assertEquals(170d, surfaceWind.getMeanWindDirection().get().getValue());
        assertEquals("m/s", surfaceWind.getMeanWindSpeed().getUom());
        assertEquals(6d, surfaceWind.getMeanWindSpeed().getValue());
        assertEquals("m/s", surfaceWind.getWindGust().get().getUom());
        assertEquals(12d, surfaceWind.getWindGust().get().getValue());
        //Change 2 Cloud
        assertTrue(change.getCloud().isPresent());
        cloud = change.getCloud().get();
        layers = cloud.getLayers().get();
        //Change 2 Layer 1
        assertEquals("SCT", layers.get(0).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(0).getBase().get().getUom());
        assertEquals(1000d, layers.get(0).getBase().get().getValue());
        assertEquals("CB", layers.get(0).getCloudType().get().getCode());
        //Change 2 Layer 2
        assertEquals("BKN", layers.get(1).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(1).getBase().get().getUom());
        assertEquals(2000d, layers.get(1).getBase().get().getValue());
        //Change 3
        change = taf.getChangeForecasts().get().get(2);
        assertFalse(change.isCeilingAndVisibilityOk());
        assertEquals(AviationCodeListUser.TAFChangeIndicator.FROM, change.getChangeIndicator());
        assertEquals("2012-08-16T12:30Z", change.getPeriodOfChange().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", change.getPeriodOfChange().getEndTime().get().getCompleteTime().get().toString());
        assertEquals("m", change.getPrevailingVisibility().get().getUom());
        assertEquals(10000d, change.getPrevailingVisibility().get().getValue());
        assertEquals(AviationCodeListUser.RelationalOperator.ABOVE, change.getPrevailingVisibilityOperator().get());
        //Change 3 Surface Wind
        surfaceWind = change.getSurfaceWind().get();
        assertFalse(surfaceWind.isVariableDirection());
        assertEquals("deg", surfaceWind.getMeanWindDirection().get().getUom());
        assertEquals(150d, surfaceWind.getMeanWindDirection().get().getValue());
        assertEquals("m/s", surfaceWind.getMeanWindSpeed().getUom());
        assertEquals(4d, surfaceWind.getMeanWindSpeed().getValue());
        //Change 3 Cloud
        assertTrue(change.getCloud().isPresent());
        cloud = change.getCloud().get();
        layers = cloud.getLayers().get();
        assertEquals("BKN", layers.get(0).getAmount().get().getCode());
        assertEquals("[ft_i]", layers.get(0).getBase().get().getUom());
        assertEquals(2000d, layers.get(0).getBase().get().getValue());
    }

    @Test
    public void cancelMessageTest() throws IOException {
        final String input = readResourceToString("taf-cancel-message.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, taf.getReportStatus());
        assertEquals(AviationCodeListUser.PermissibleUsage.OPERATIONAL, taf.getPermissibleUsage().get());
        assertTrue(taf.isCancelMessage());
        //Issue Time
        assertEquals("2012-08-16T15:00Z", taf.getIssueTime().get().getCompleteTime().get().toString());

        final Aerodrome aerodrome = taf.getAerodrome();
        assertEquals("YUDO", aerodrome.getDesignator());
        assertEquals("DONLON/INTERNATIONAL", aerodrome.getName().get());
        assertEquals("YUDO", aerodrome.getLocationIndicatorICAO().get());

        assertEquals("2012-08-16T00:00Z", taf.getReferredReportValidPeriod().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", taf.getReferredReportValidPeriod().get().getEndTime().get().getCompleteTime().get().toString());
    }

    @Test
    public void noSignificantWeatherTest() throws IOException {
        final String input = readResourceToString("taf-no-significant-weather-or-cloud.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertTrue("Expected noSignificantWeather on nothingOfOperationalSignificance nilReason", //
                taf.getBaseForecast().get().isNoSignificantWeather());
        assertFalse("Expected no noSignificantWeather missing weather", //
                taf.getChangeForecasts().get().get(0).isNoSignificantWeather());
        assertFalse("Expected no noSignificantWeather on present weather", //
                taf.getChangeForecasts().get().get(1).isNoSignificantWeather());
        assertFalse("Expected no noSignificantWeather on 'missing' nilReason", //
                taf.getChangeForecasts().get().get(3).isNoSignificantWeather());
        assertFalse("Expected no weather on 'missing' nilReason", //
                taf.getChangeForecasts().get().get(3).getForecastWeather().isPresent());
        assertFalse(taf.isTranslated());
    }

    @Test
    public void noSignificantCloudTest() throws IOException {
        final String input = readResourceToString("taf-no-significant-weather-or-cloud.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertTrue("Expected noSignificantCloud on nothingOfOperationalSignificance nilReason", //
                taf.getBaseForecast().get().getCloud().get().isNoSignificantCloud());
        assertFalse("Expected empty cloud on missing cloud", //
                taf.getChangeForecasts().get().get(0).getCloud().isPresent());
        assertFalse("Expected no noSignificantCloud on present cloud", //
                taf.getChangeForecasts().get().get(1).getCloud().get().isNoSignificantCloud());
        assertFalse(taf.getChangeForecasts().get().get(2).getCloud().get().isVerticalVisibilityMissing());
        assertEquals("[ft_i]", taf.getChangeForecasts().get().get(2).getCloud().get().getVerticalVisibility().get().getUom());
        assertEquals(2000.0, taf.getChangeForecasts().get().get(2).getCloud().get().getVerticalVisibility().get().getValue());
        assertFalse("Expected no cloud on 'missing' nilReason", //
                taf.getChangeForecasts().get().get(3).getCloud().isPresent());
        assertFalse(taf.isTranslated());
    }

    @Test
    public void translatedMetaPropsTest() throws IOException {
        final String input = readResourceToString("taf-translated.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertTrue(taf.isTranslated());
        assertEquals("123456", taf.getTranslatedBulletinID().get());
        assertEquals("Translator center", taf.getTranslationCentreDesignator().get());
        assertEquals("Translator center 1", taf.getTranslationCentreName().get());
        assertEquals("2012-08-16T00:00Z[GMT]", taf.getTranslatedBulletinReceptionTime().get().toString());
        assertEquals("2012-08-15T00:00Z[GMT]", taf.getTranslationTime().get().toString());

    }

    @Test
    public void CAVOKTrueTest() throws IOException {
        final String input = readResourceToString("taf-cavok.xml");
        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertThatConversionResult(result).isSuccessful().getMessage();

        assertTrue(taf.getBaseForecast().isPresent());
        assertTrue(taf.getBaseForecast().get().isCeilingAndVisibilityOk());

        assertTrue(taf.getChangeForecasts().isPresent());
        assertTrue(taf.getChangeForecasts().get().get(0).isCeilingAndVisibilityOk());
    }
}
