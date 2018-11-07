package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;


/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private TAF getTAF() throws IOException {
        TAF t = readFromJSON("taf12.json");
        AerodromeImpl.Builder airportBuilder = new AerodromeImpl.Builder()
                .setDesignator("EETN")
                .setName("Tallinn Airport")
                .setFieldElevationValue(40.0)
                .setLocationIndicatorICAO("EETN")
                .setReferencePoint(new GeoPositionImpl.Builder()
                        .setCoordinateReferenceSystemId("http://www.opengis.net/def/crs/EPSG/0/4326")
                        .setCoordinates(new Double[]{24.8325, 59.413333})
                        .setElevationValue(40.0)
                        .setElevationUom("m")
                        .build()
                );
        TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
        tafBuilder
                .setAerodrome(airportBuilder.build())
                .withCompleteIssueTime(YearMonth.of(2017,7))
                .withCompleteForecastTimes(YearMonth.of(2017,7),30,11,ZoneId.of("Z"))
                .setTranslatedTAC("EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z\n" + "TEMPO 3012/3018 3000 RADZ BR OVC004\n"
                        + "BECMG 3018/3020 BKN008 SCT015CB\n" + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" + "BECMG 3104/3106 21016G30KT=")
                .setTranslationTime(ZonedDateTime.now());

        return tafBuilder.build();
    }

    @Test
    public void testCancelledTAFSerialisation() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        TAF t = readFromJSON("taf-A5-2.json");
        assertTrue(AviationCodeListUser.TAFStatus.CANCELLATION == t.getStatus());
        ConversionResult<String> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void testTAFStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        TAF t = getTAF();
        ConversionResult<String> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }
    @Test
    public void testTAFDOMSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));
        TAF t = getTAF();
        ConversionResult<Document> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        Element docElement = result.getConvertedMessage().map(Document::getDocumentElement).orElse(null);
        assertNotNull(docElement);

        XPathExpression expr = xpath.compile("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id");
        String issueTimeId = expr.evaluate(docElement);
        assertNotNull(issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id");
        String validTimeId = expr.evaluate(docElement);
        assertNotNull(validTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition");
        assertEquals("issueTime does not match", "2017-07-30T11:30:00Z", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:beginPosition");
        assertEquals("validTime begin position does not match", "2017-07-30T12:00:00Z", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:endPosition");
        assertEquals("validTime end position does not match", "2017-07-31T12:00:00Z", expr.evaluate(docElement));

        //Base forecast: EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Base forecast type does not match", "http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Temporals:

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:phenomenonTime/@xlink:href");
        String timeRef = expr.evaluate(docElement);
        assertEquals("Base forecast phenomenonTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Base forecast resultTime does not refer to msg issueTime", timeRef, "#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Base forecast validTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/@gml:id");
        String procedureId = expr.evaluate(docElement);
        assertTrue("No procedure gml:id", !"".equals(procedureId));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/gml:description");
        assertEquals("Process description does not match",
                "WMO No. 49 Volume 2 Meteorological Service for International Air Navigation APPENDIX 5 TECHNICAL" + " SPECIFICATIONS RELATED TO FORECASTS",
                expr.evaluate(docElement));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Observed properties does not match", "http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Aerodrome FOI (samplingFeature):

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/@gml:id");
        String foiId = expr.evaluate(docElement);
        assertTrue("No FOI gml:id", !"".equals(foiId));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        assertEquals("Airport designator does not match", "EETN", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        assertEquals("Airport name does not match", "TALLINN AIRPORT", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        assertEquals("Airport ICAO code does not match", "EETN", expr.evaluate(docElement));


        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation");
        assertEquals("Airport elevation value does not match", "40", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation/@uom");
        assertEquals("Airport elevation unit does not match", "M", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsName");
        assertEquals("Airport reference point srsName does not match", "http://www.opengis.net/def/crs/EPSG/0/4326", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/gml:pos");
        assertEquals("Airport reference point position value does not match", "24.8325 59.413333", expr.evaluate(docElement));


        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation");
        assertEquals("Airport reference point elevation value does not match", "40", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation/@uom");
        assertEquals("Airport reference point elevation unit does not match", "M", expr.evaluate(docElement));

        //Sampling point:

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/@srsName");
        assertEquals("Sampling point srsName does not match", "http://www.opengis.net/def/crs/EPSG/0/4326", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/gml:pos");
        assertEquals("Sampling point position value does not match", "24.8325 59.413333", expr.evaluate(docElement));

        //Forecast properties:

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertEquals("CAVOK does not match", "false", expr.evaluate(docElement));

        //Wind:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertEquals("Variable wind not match", "false", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertEquals("Mean wind direction uom does not match", "deg", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertTrue("Mean wind direction does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 140.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertEquals("Mean wind speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertTrue("Mean wind speed does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 16.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertEquals("Wind gust speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed");
        assertTrue("Wind gust speed does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 26.0) < 0.00001);

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertTrue("Visibility does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 8000.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Visibility uom does not match", "m", expr.evaluate(docElement));

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Cloud layer count does not match", "2", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Cloud layer 1 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/3", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Cloud layer 1 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 1000.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Cloud layer 1 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/4", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Cloud layer 1 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 1500.0) < 0.00001);

        //Temperatures:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperature/@uom");
        assertEquals("Max air temperature uom does not match", "degC", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperature");
        assertTrue("Max air temperature does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - (-2.0)) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperatureTime");
        assertEquals("Max air temperature time does not match", "2017-07-30T15:00:00Z", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperature/@uom");
        assertEquals("Min air temperature uom does not match", "degC", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperature");
        assertTrue("Min air temperature does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - (-10.0)) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                        + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperatureTime");
        assertEquals("Min air temperature time does not match", "2017-07-31T03:00:00Z", expr.evaluate(docElement));


        //Change forecast 1: TEMPO 3012/3018 3000 RADZ BR OVC004

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 1 type does not match", "http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 1 phenomenonTime begin pos does not match", "2017-07-30T12:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 1 phenomenonTime end pos does not match", "2017-07-30T18:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 1 resultTime does not refer to msg issueTime", timeRef, "#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 1 validTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:procedure/@xlink:href");
        assertEquals("Change forecast 1 Procedure does not refer to base forecast procedure",
                "#" + procedureId,
                expr.evaluate(docElement));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 1 Observed properties does not match", "http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertEquals("Change forecast 1 FOI reference does not point to base forecast FOI", "#" + foiId, expr.evaluate(docElement));

        //Change indicator:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 1 change indicator does not match", "TEMPORARY_FLUCTUATIONS", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertEquals("Change forecast 1 CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertTrue("Change forecast 1 Visibility does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 3000.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Change forecast 1 visibility uom does not match", "m", expr.evaluate(docElement));

        //Weather:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[1]"
                + "/@xlink:href");
        assertEquals("Change forecast 1 weather 1 does not match", "http://codes.wmo.int/306/4678/RADZ" , expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[2]"
                + "/@xlink:href");
        assertEquals("Change forecast 1 weather 2 does not match", "http://codes.wmo.int/306/4678/BR" , expr.evaluate(docElement));

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Change Forecast 1 cloud layer count does not match", "1", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 1 cloud layer 1 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/4", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 1 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Change Forecast 1 cloud layer 1 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 400.0) < 0.00001);



        //Change forecast 2: BECMG 3018/3020 BKN008 SCT015CB

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 2 type does not match", "http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 2 phenomenonTime begin pos does not match", "2017-07-30T18:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 2 phenomenonTime end pos does not match", "2017-07-30T20:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 2 resultTime does not refer to msg issueTime", timeRef, "#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 2 validTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:procedure/@xlink:href");
        assertEquals("Change forecast 2 Procedure does not refer to base forecast procedure",
                "#" + procedureId,
                expr.evaluate(docElement));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 2 observed properties does not match", "http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertEquals("Change forecast 2 FOI reference does not point to base forecast FOI", "#" + foiId, expr.evaluate(docElement));

        //Change indicator:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 2 change indicator does not match", "BECOMING", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord"
                + "/@cloudAndVisibilityOK");
        assertEquals("CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Change Forecast 2 cloud layer count does not match", "2", expr.evaluate(docElement));

        //BKN008
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 1 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/3", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 2 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Change Forecast 2 cloud layer 1 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 800.0) < 0.00001);

        //SCT015CB
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 2 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/2", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 2 cloud layer 2 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Change Forecast 2 cloud layer 2 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 1500.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 2 type does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-012/9", expr.evaluate(docElement));


        //Change forecast 3: TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 3 type does not match", "http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 3 phenomenonTime begin pos does not match", "2017-07-31T02:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 3 phenomenonTime end pos does not match", "2017-07-31T12:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 3 resultTime does not refer to msg issueTime", timeRef, "#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 3 validTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:procedure/@xlink:href");
        assertEquals("Change forecast 3 procedure does not refer to base forecast procedure",
                "#" + procedureId,
                expr.evaluate(docElement));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 3 observed properties does not match", "http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertEquals("Change forecast 3 FOI reference does not point to base forecast FOI", "#" + foiId, expr.evaluate(docElement));

        //Change indicator:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 3 change indicator does not match", "TEMPORARY_FLUCTUATIONS", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord"
                + "/@cloudAndVisibilityOK");
        assertEquals("Change forecast 3 CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertTrue("Change forecast 3 Visibility does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 3000.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Change forecast 3 visibility uom does not match", "m", expr.evaluate(docElement));

        //Weather:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[1]"
                + "/@xlink:href");
        assertEquals("Change forecast 3 weather 1 does not match", "http://codes.wmo.int/306/4678/SHRASN" , expr.evaluate(docElement));

        //Cloud:

        //BKN006
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 3 cloud layer 1 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/3", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 3 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Change Forecast 3 cloud layer 1 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 600.0) < 0.00001);

        //BKN015CB
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 3 cloud layer 2 amount does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-008/3", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 3 cloud layer 2 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertTrue("Change Forecast 3 cloud layer 2 base does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 1500.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertEquals("Change Forecast 3 cloud layer 2 type does not match", "http://codes.wmo.int/bufr4/codeflag/0-20-012/9", expr.evaluate(docElement));

        //Change forecast 4: BECMG 3104/3106 21016G30KT VV001=

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 4 type does not match", "http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 4 phenomenonTime begin pos does not match", "2017-07-31T04:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 4 phenomenonTime end pos does not match", "2017-07-31T06:00:00Z", timeRef);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 4 resultTime does not refer to msg issueTime", timeRef, "#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertEquals("Change forecast 4 validTime does not refer to msg validTime", timeRef, "#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:procedure/@xlink:href");
        assertEquals("Change forecast 4 procedure does not refer to base forecast procedure",
                "#" + procedureId,
                expr.evaluate(docElement));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 4 observed properties does not match", "http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast",
                expr.evaluate(docElement));

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertEquals("Change forecast 4 FOI reference does not point to base forecast FOI", "#" + foiId, expr.evaluate(docElement));

        //Change indicator:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 4 change indicator does not match", "BECOMING", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord"
                + "/@cloudAndVisibilityOK");
        assertEquals("Change forecast 4 CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Wind: 21016G30KT
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertEquals("Change forecast 4 variable wind not match", "false", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertEquals("Change forecast 4 Mean wind direction uom does not match", "deg", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertTrue("Change forecast 4 Mean wind direction does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 210.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertEquals("Change forecast 4 Mean wind speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertTrue("Change forecast 4 Mean wind speed does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 16.0) < 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertEquals("Change forecast 4 Wind gust speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed");
        assertTrue("Change forecast 4 Wind gust speed does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 30.0) < 0.00001);

        //VV001
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:verticalVisibility");
        assertTrue("Change Forecast 4 cloud vertical visibility value does not match", Math.abs(Double.parseDouble(expr.evaluate(docElement)) - 100) < 0.00001);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:verticalVisibility/@uom");
        assertEquals("Change Forecast 4 cloud vertical visibility uom does not match", "[ft_i]", expr.evaluate(docElement));

    }

    protected TAF readFromJSON(String fileName) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        InputStream is = TAFIWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            return om.readValue(is, TAFImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }
}
