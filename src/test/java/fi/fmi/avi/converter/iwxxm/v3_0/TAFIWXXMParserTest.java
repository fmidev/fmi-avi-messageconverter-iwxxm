package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

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
    public void normalMessageTest() {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO));
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<iwxxm:TAF \n" + "    xmlns:iwxxm=\"http://icao.int/iwxxm/3.0\" \n"
                + "    xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "    xmlns:aixm=\"http://www.aixm.aero/schema/5.1.1\"\n"
                + "    xsi:schemaLocation=\"http://icao.int/iwxxm/3.0 http://schemas.wmo.int/iwxxm/3.0/iwxxm.xsd\"\n"
                + "    gml:id=\"uuid.d6a85870-f32e-4ea8-8502-c7d9be7e0144\"\n" + "    reportStatus=\"NORMAL\"\n" + "    permissibleUsageReason=\"TEST\""
                + "    permissibleUsage=\"NON-OPERATIONAL" + "\">\n"
                + "\n" + "    <iwxxm:issueTime>\n" + "        <gml:TimeInstant gml:id=\"uuid.0c02ab83-22e8-4a66-b115-35544e7078f1\">\n"
                + "            <gml:timePosition>2012-08-15T18:00:00Z</gml:timePosition>\n" + "        </gml:TimeInstant>\n" + "    </iwxxm:issueTime>\n" + "\n"
                + "    <iwxxm:aerodrome>\n" + "        <aixm:AirportHeliport gml:id=\"uuid.a5a157ad-5b93-443d-a8e6-8324816ccb7e\">\n"
                + "            <aixm:timeSlice>\n" + "                <aixm:AirportHeliportTimeSlice gml:id=\"uuid.9cf64bc2-0aca-452e-a330-9f6ce9f533e4\">\n"
                + "                    <gml:validTime/>\n" + "                    <aixm:interpretation>SNAPSHOT</aixm:interpretation>\n"
                + "                    <aixm:designator>YUDO</aixm:designator>\n" + "                    <aixm:name>DONLON/INTERNATIONAL</aixm:name>\n"
                + "                    <aixm:locationIndicatorICAO>YUDO</aixm:locationIndicatorICAO>\n" + "                    <aixm:ARP>\n"
                + "                        <aixm:ElevatedPoint gml:id=\"uuid.4f5ce1f8-ae4c-4dea-99c7-6bc15ca6b004\" srsDimension=\"2\" axisLabels=\"Lat Long\" srsName=\"http://www.opengis.net/def/crs/EPSG/0/4326\">\n"
                + "                            <gml:pos>12.34 -12.34</gml:pos>\n"
                + "                            <aixm:elevation uom=\"M\">12</aixm:elevation>\n"
                + "                            <aixm:verticalDatum>EGM_96</aixm:verticalDatum>\n" + "                        </aixm:ElevatedPoint>\n"
                + "                    </aixm:ARP>\n" + "                </aixm:AirportHeliportTimeSlice>\n" + "            </aixm:timeSlice>\n"
                + "        </aixm:AirportHeliport>\n" + "    </iwxxm:aerodrome>\n" + "\n" + "    <iwxxm:validPeriod>\n"
                + "        <gml:TimePeriod gml:id=\"uuid.ed5d465f-51fb-4ecc-834a-2ee26bb1196f\">\n"
                + "            <gml:beginPosition>2012-08-16T00:00:00Z</gml:beginPosition>\n"
                + "            <gml:endPosition>2012-08-16T18:00:00Z</gml:endPosition>\n" + "        </gml:TimePeriod>\n" + "    </iwxxm:validPeriod>\n" + "\n"
                + "    <iwxxm:baseForecast>\n"
                + "        <iwxxm:MeteorologicalAerodromeForecast gml:id=\"uuid.13048235-8f8a-4587-a6be-e8ce99f4525c\" cloudAndVisibilityOK=\"false\">\n"
                + "            <iwxxm:phenomenonTime xlink:href=\"#uuid.ed5d465f-51fb-4ecc-834a-2ee26bb1196f\"/>\n"
                + "            <iwxxm:prevailingVisibility uom=\"m\">9000</iwxxm:prevailingVisibility>\n" + "            <iwxxm:surfaceWind>\n"
                + "                <iwxxm:AerodromeSurfaceWindForecast variableWindDirection=\"false\">\n"
                + "                    <iwxxm:meanWindDirection uom=\"deg\">130</iwxxm:meanWindDirection>\n"
                + "                    <iwxxm:meanWindSpeed uom=\"m/s\">5.0</iwxxm:meanWindSpeed>\n" + "                </iwxxm:AerodromeSurfaceWindForecast>\n"
                + "            </iwxxm:surfaceWind>\n" + "            <iwxxm:cloud>\n"
                + "                <iwxxm:AerodromeCloudForecast gml:id=\"uuid.cbd88f13-e806-4ab9-857f-b82a66cc2854\">\n"
                + "                    <iwxxm:layer>\n" + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">2000</iwxxm:base>\n" + "                        </iwxxm:CloudLayer>\n"
                + "                    </iwxxm:layer>\n" + "                </iwxxm:AerodromeCloudForecast>\n" + "            </iwxxm:cloud>\n"
                + "        </iwxxm:MeteorologicalAerodromeForecast>\n" + "    </iwxxm:baseForecast>\n" + "\n" + "    <iwxxm:changeForecast>\n"
                + "        <iwxxm:MeteorologicalAerodromeForecast gml:id=\"uuid.fe2635c8-3994-4423-b79f-123593fa2b55\" changeIndicator=\"BECOMING\" cloudAndVisibilityOK=\"false\">\n"
                + "            <iwxxm:phenomenonTime>\n" + "                <gml:TimePeriod gml:id=\"uuid.921ff3f9-e89e-402e-acd1-82c8600f9929\">\n"
                + "                    <gml:beginPosition>2012-08-16T06:00:00Z</gml:beginPosition>\n"
                + "                    <gml:endPosition>2012-08-16T08:00:00Z</gml:endPosition>\n" + "                </gml:TimePeriod>\n"
                + "            </iwxxm:phenomenonTime>\n" + "            <iwxxm:cloud>\n"
                + "                <iwxxm:AerodromeCloudForecast gml:id=\"uuid.235edc5d-213f-490b-8fdb-755032480849\">\n"
                + "                    <iwxxm:layer>\n" + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">1500</iwxxm:base>\n"
                + "                            <iwxxm:cloudType xlink:href=\"http://codes.wmo.int/49-2/SigConvectiveCloudType/CB\"/>\n"
                + "                        </iwxxm:CloudLayer>\n" + "                    </iwxxm:layer>\n" + "                    <iwxxm:layer>\n"
                + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">2000</iwxxm:base>\n" + "                        </iwxxm:CloudLayer>\n"
                + "                    </iwxxm:layer>\n" + "                </iwxxm:AerodromeCloudForecast>\n" + "            </iwxxm:cloud>\n"
                + "        </iwxxm:MeteorologicalAerodromeForecast>\n" + "    </iwxxm:changeForecast>\n" + "\n" + "    <iwxxm:changeForecast>\n"
                + "        <iwxxm:MeteorologicalAerodromeForecast gml:id=\"uuid.8985ba4e-f6f0-4dc2-8dd7-2cbf86a64056\" changeIndicator=\"TEMPORARY_FLUCTUATIONS\" cloudAndVisibilityOK=\"false\">\n"
                + "            <iwxxm:phenomenonTime>\n" + "                <gml:TimePeriod gml:id=\"uuid.5bdac885-79bf-4ebe-9693-cd0a4b2b91d0\">\n"
                + "                    <gml:beginPosition>2012-08-16T08:00:00Z</gml:beginPosition>\n"
                + "                    <gml:endPosition>2012-08-16T12:00:00Z</gml:endPosition>\n" + "                </gml:TimePeriod>\n"
                + "            </iwxxm:phenomenonTime>\n" + "            <iwxxm:prevailingVisibility uom=\"m\">1000</iwxxm:prevailingVisibility>\n"
                + "            <iwxxm:surfaceWind>\n" + "                <iwxxm:AerodromeSurfaceWindForecast variableWindDirection=\"false\">\n"
                + "                    <iwxxm:meanWindDirection uom=\"deg\">170</iwxxm:meanWindDirection>\n"
                + "                    <iwxxm:meanWindSpeed uom=\"m/s\">6</iwxxm:meanWindSpeed>\n"
                + "                    <iwxxm:windGustSpeed uom=\"m/s\">12</iwxxm:windGustSpeed>\n" + "                </iwxxm:AerodromeSurfaceWindForecast>\n"
                + "            </iwxxm:surfaceWind>\n" + "            <iwxxm:weather xlink:href=\"http://codes.wmo.int/306/4678/TSRA\"/>\n"
                + "            <iwxxm:cloud>\n" + "                <iwxxm:AerodromeCloudForecast gml:id=\"uuid.5d76e06c-220d-4491-83e8-439108891615\">\n"
                + "                    <iwxxm:layer>\n" + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">1000</iwxxm:base>\n"
                + "                            <iwxxm:cloudType xlink:href=\"http://codes.wmo.int/49-2/SigConvectiveCloudType/CB\"/>\n"
                + "                        </iwxxm:CloudLayer>\n" + "                    </iwxxm:layer>\n" + "                    <iwxxm:layer>\n"
                + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">2000</iwxxm:base>\n" + "                        </iwxxm:CloudLayer>\n"
                + "                    </iwxxm:layer>\n" + "                </iwxxm:AerodromeCloudForecast>\n" + "            </iwxxm:cloud>\n"
                + "        </iwxxm:MeteorologicalAerodromeForecast>\n" + "    </iwxxm:changeForecast>\n" + "\n" + "    <iwxxm:changeForecast>\n"
                + "        <iwxxm:MeteorologicalAerodromeForecast gml:id=\"uuid.aeadfb00-4cfa-46b5-af21-1e79e4b6b1b5\" changeIndicator=\"FROM\" cloudAndVisibilityOK=\"false\">\n"
                + "            <iwxxm:phenomenonTime>\n" + "                <gml:TimePeriod gml:id=\"uuid.06f40788-ea21-471e-8c17-c0ef5b581c98\">\n"
                + "                    <gml:beginPosition>2012-08-16T12:30:00Z</gml:beginPosition>\n"
                + "                    <gml:endPosition>2012-08-16T18:00:00Z</gml:endPosition>\n" + "                </gml:TimePeriod>\n"
                + "            </iwxxm:phenomenonTime>\n" + "            <iwxxm:prevailingVisibility uom=\"m\">10000</iwxxm:prevailingVisibility>\n"
                + "            <iwxxm:prevailingVisibilityOperator>ABOVE</iwxxm:prevailingVisibilityOperator>\n" + "            <iwxxm:surfaceWind>\n"
                + "                <iwxxm:AerodromeSurfaceWindForecast variableWindDirection=\"false\">\n"
                + "                    <iwxxm:meanWindDirection uom=\"deg\">150</iwxxm:meanWindDirection>\n"
                + "                    <iwxxm:meanWindSpeed uom=\"m/s\">4</iwxxm:meanWindSpeed>\n" + "                </iwxxm:AerodromeSurfaceWindForecast>\n"
                + "            </iwxxm:surfaceWind>\n" + "            <iwxxm:cloud>\n"
                + "                <iwxxm:AerodromeCloudForecast gml:id=\"uuid.ad9464f9-82de-49bd-8770-596b774591e5\">\n"
                + "                    <iwxxm:layer>\n" + "                        <iwxxm:CloudLayer>\n"
                + "                            <iwxxm:amount xlink:href=\"http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN\"/>\n"
                + "                            <iwxxm:base uom=\"[ft_i]\">2000</iwxxm:base>\n" + "                        </iwxxm:CloudLayer>\n"
                + "                    </iwxxm:layer>\n" + "                </iwxxm:AerodromeCloudForecast>\n" + "            </iwxxm:cloud>\n"
                + "        </iwxxm:MeteorologicalAerodromeForecast>\n" + "    </iwxxm:changeForecast>\n" + "\n" + "</iwxxm:TAF>";
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
        assertFalse(base.isNoSignificantWeather());
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
    public void cancelMessageTest() {
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<iwxxm:TAF \n" + "    xmlns:iwxxm=\"http://icao.int/iwxxm/3.0\" \n"
                + "    xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
                + "    xmlns:aixm=\"http://www.aixm.aero/schema/5.1.1\"\n" + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"http://icao.int/iwxxm/3.0 http://schemas.wmo.int/iwxxm/3.0/iwxxm.xsd\"\n"
                + "    gml:id=\"uuid.dfbfe27c-3478-4efc-9800-124b90030e39\"\n" + "    reportStatus=\"AMENDMENT\"\n" + "    permissibleUsage=\"OPERATIONAL\"\n"
                + "    isCancelReport=\"true\">\n" + "    <iwxxm:issueTime>\n"
                + "        <gml:TimeInstant gml:id=\"uuid.2a142955-e0ef-4164-a550-ec80a3258150\">\n"
                + "            <gml:timePosition>2012-08-16T15:00:00Z</gml:timePosition>\n" + "        </gml:TimeInstant>\n" + "    </iwxxm:issueTime>\n"
                + "    <iwxxm:aerodrome>\n" + "        <aixm:AirportHeliport gml:id=\"uuid.65730df9-a2fd-4815-9d3f-d6f6770bc6cf\">\n"
                + "            <aixm:timeSlice>\n" + "                <aixm:AirportHeliportTimeSlice gml:id=\"uuid.30a6076d-48ec-42df-bcd5-bf85220fe274\">\n"
                + "                    <gml:validTime/>\n" + "                    <aixm:interpretation>SNAPSHOT</aixm:interpretation>\n"
                + "                    <aixm:designator>YUDO</aixm:designator>\n" + "                    <aixm:name>DONLON/INTERNATIONAL</aixm:name>\n"
                + "                    <aixm:locationIndicatorICAO>YUDO</aixm:locationIndicatorICAO>\n" + "                </aixm:AirportHeliportTimeSlice>\n"
                + "            </aixm:timeSlice>\n" + "        </aixm:AirportHeliport>\n" + "    </iwxxm:aerodrome>\n"
                + "    <iwxxm:cancelledReportValidPeriod>\n" + "        <gml:TimePeriod gml:id=\"uuid.b171a7e2-7fdf-496c-ada8-6cbc6c606291\">\n"
                + "            <gml:beginPosition>2012-08-16T00:00:00Z</gml:beginPosition>\n"
                + "            <gml:endPosition>2012-08-16T18:00:00Z</gml:endPosition>\n" + "        </gml:TimePeriod>\n"
                + "    </iwxxm:cancelledReportValidPeriod>\n" + "</iwxxm:TAF>";

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

        assertEquals("2012-08-16T00:00Z", taf.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z",taf.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());
    }
}
