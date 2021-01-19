package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertTrue;

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
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void test() {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO));
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<iwxxm:TAF \n" + "    xmlns:iwxxm=\"http://icao.int/iwxxm/3.0\" \n"
                + "    xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "    xmlns:aixm=\"http://www.aixm.aero/schema/5.1.1\"\n"
                + "    xsi:schemaLocation=\"http://icao.int/iwxxm/3.0 http://schemas.wmo.int/iwxxm/3.0/iwxxm.xsd\"\n"
                + "    gml:id=\"uuid.d6a85870-f32e-4ea8-8502-c7d9be7e0144\"\n" + "    reportStatus=\"NORMAL\"\n" + "    permissibleUsage=\"OPERATIONAL\">\n"
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
    }


}
