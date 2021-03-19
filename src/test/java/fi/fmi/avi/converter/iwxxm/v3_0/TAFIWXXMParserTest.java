package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.ElevatedPoint;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void normalMessageTest() throws IOException {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO));
        String input = getInput("taf-reportstatus-normal.xml");

        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        //assertEquals(0 ,result.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        TAF taf = result.getConvertedMessage().get();
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, taf.getReportStatus().get());
        assertEquals(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL, taf.getPermissibleUsage().get());
        assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST, taf.getPermissibleUsageReason().get());
        //Issue Time
        assertEquals("2012-08-15T18:00Z", taf.getIssueTime().get().getCompleteTime().get().toString());
        //Valid Time
        assertEquals("2012-08-16T00:00Z", taf.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z",taf.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        //Aerodrome
        Aerodrome aerodrome = taf.getAerodrome();
        assertEquals("YUDO", aerodrome.getDesignator());
        assertEquals("DONLON/INTERNATIONAL", aerodrome.getName().get());
        assertEquals("YUDO", aerodrome.getLocationIndicatorICAO().get());
        assertTrue(aerodrome.getReferencePoint().isPresent());
        ElevatedPoint point = aerodrome.getReferencePoint().get();
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
        TAFBaseForecast base = taf.getBaseForecast().get();
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
        assertTrue(taf.getChangeForecasts().get().size() > 0);

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
        String input = getInput("taf-cancel-message.xml");

        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        TAF taf = result.getConvertedMessage().get();
        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, taf.getReportStatus().get());
        assertEquals(AviationCodeListUser.PermissibleUsage.OPERATIONAL, taf.getPermissibleUsage().get());
        assertTrue(taf.isCancelMessage());
        //Issue Time
        assertEquals("2012-08-16T15:00Z", taf.getIssueTime().get().getCompleteTime().get().toString());

        Aerodrome aerodrome = taf.getAerodrome();
        assertEquals("YUDO", aerodrome.getDesignator());
        assertEquals("DONLON/INTERNATIONAL", aerodrome.getName().get());
        assertEquals("YUDO", aerodrome.getLocationIndicatorICAO().get());

        assertEquals("2012-08-16T00:00Z", taf.getReferredReportValidPeriod().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", taf.getReferredReportValidPeriod().get().getEndTime().get().getCompleteTime().get().toString());
    }

    @Test
    public void noSignificantWeatherTest() throws IOException {
        String input = getInput("taf-vertical-visibility-missing.xml");

        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        TAF taf = result.getConvertedMessage().get();

        assertTrue(taf.getBaseForecast().get().isNoSignificantWeather());
        assertTrue(taf.getChangeForecasts().get().get(0).isNoSignificantWeather());
        assertFalse(taf.getChangeForecasts().get().get(1).isNoSignificantWeather());
        assertTrue(taf.getChangeForecasts().get().get(1).getCloud().get().isVerticalVisibilityMissing());
        assertFalse(taf.getChangeForecasts().get().get(2).getCloud().get().isVerticalVisibilityMissing());
        assertEquals("[ft_i]", taf.getChangeForecasts().get().get(2).getCloud().get().getVerticalVisibility().get().getUom());
        assertEquals(2000d, taf.getChangeForecasts().get().get(2).getCloud().get().getVerticalVisibility().get().getValue());
        assertFalse(taf.isTranslated());
    }

    @Test
    public void translatedMetaPropsTest() throws IOException {
        String input = getInput("taf-translated.xml");

        final ConversionResult<TAF> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                ConversionHints.EMPTY);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        TAF taf = result.getConvertedMessage().get();

        assertTrue(taf.isTranslated());
        assertEquals("123456", taf.getTranslatedBulletinID().get());
        assertEquals("Translator center", taf.getTranslationCentreDesignator().get());
        assertEquals("Translator center 1", taf.getTranslationCentreName().get());
        assertEquals("2012-08-16T00:00Z[GMT]" , taf.getTranslatedBulletinReceptionTime().get().toString());
        assertEquals("2012-08-15T00:00Z[GMT]", taf.getTranslationTime().get().toString());

    }

    private String getInput(final String fileName) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }
}
