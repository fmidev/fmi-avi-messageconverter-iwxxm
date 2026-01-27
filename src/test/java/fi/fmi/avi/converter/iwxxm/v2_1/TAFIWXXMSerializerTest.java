package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.*;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private TAF getTAF() throws IOException {
        final TAF t = readFromJSON("taf12.json", TAFImpl.class);
        final AerodromeImpl.Builder airportBuilder = AerodromeImpl.builder()
                .setDesignator("EETN")
                .setName("Tallinn Airport")
                .setFieldElevationValue(40.0)
                .setFieldElevationUom("FT")
                .setLocationIndicatorICAO("EETN")
                .setReferencePoint(ElevatedPointImpl.builder()
                        .setCrs(CoordinateReferenceSystemImpl.wgs84())
                        .addCoordinates(24.8325, 59.413333)
                        .setElevationValue(40.0)
                        .setElevationUom("m")
                        .build());
        final TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
        tafBuilder.setAerodrome(airportBuilder.build())
                .withCompleteIssueTime(YearMonth.of(2017, 7))
                .withCompleteForecastTimes(YearMonth.of(2017, 7), 30, 11, ZoneId.of("Z"))
                .setTranslatedTAC("EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z\n" + "TEMPO 3012/3018 3000 RADZ BR OVC004\n"
                        + "BECMG 3018/3020 BKN008 SCT015CB\n" + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" + "BECMG 3104/3106 21016G30KT=")
                .setTranslationTime(ZonedDateTime.now());

        return tafBuilder.build();
    }

    @Test
    public void testCancelledTAFSerialisation() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        final TAF t = readFromJSON("taf-A5-2.json", TAFImpl.class);
        assertTrue(t.isCancelMessage());
        final ConversionResult<String> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING);
        // Test data lacks aerodrome reference point location, which produces an issue
        assertThatConversionResult(result)
                .hasStatus(ConversionResult.Status.SUCCESS)
                .hasIssueContaining("Aerodrome references does not contain reference point location");
    }

    @Test
    public void testTAFStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        final TAF t = getTAF();
        final ConversionResult<String> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING);
        assertThatConversionResult(result).isSuccessful();
    }

    @Test
    public void testTAFDOMSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));
        final TAF t = getTAF();
        final ConversionResult<Document> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM);
        final Document doc = assertThatConversionResult(result).isSuccessful().getMessage();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        final Element docElement = doc.getDocumentElement();

        XPathExpression expr = xpath.compile("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id");
        final String issueTimeId = expr.evaluate(docElement);
        assertNotNull(issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id");
        final String validTimeId = expr.evaluate(docElement);
        assertNotNull(validTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition");
        assertThat(expr.evaluate(docElement)).as("issueTime does not match").isEqualTo("2017-07-30T11:30:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:beginPosition");
        assertThat(expr.evaluate(docElement)).as("validTime begin position does not match").isEqualTo("2017-07-30T12:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:endPosition");
        assertThat(expr.evaluate(docElement)).as("validTime end position does not match").isEqualTo("2017-07-31T12:00:00Z");

        //Base forecast: EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:type/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Base forecast type does not match").isEqualTo("http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast");

        //Temporals:

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:phenomenonTime/@xlink:href");
        String timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Base forecast phenomenonTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Base forecast resultTime does not refer to msg issueTime").isEqualTo("#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Base forecast validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/@gml:id");
        final String procedureId = expr.evaluate(docElement);
        assertFalse("No procedure gml:id", procedureId.isEmpty());

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/gml:description");
        assertThat(expr.evaluate(docElement)).as("Process description does not match").isEqualTo("WMO No. 49 Volume 2 Meteorological Service for International Air Navigation APPENDIX 5 TECHNICAL" + " SPECIFICATIONS RELATED TO FORECASTS");

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:observedProperty/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Observed properties does not match").isEqualTo("http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast");

        //Aerodrome FOI (samplingFeature):

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/@gml:id");
        final String foiId = expr.evaluate(docElement);
        assertFalse("No FOI gml:id", foiId.isEmpty());

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        assertThat(expr.evaluate(docElement)).as("Airport designator does not match").isEqualTo("EETN");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        assertThat(expr.evaluate(docElement)).as("Airport name does not match").isEqualTo("TALLINN AIRPORT");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        assertThat(expr.evaluate(docElement)).as("Airport ICAO code does not match").isEqualTo("EETN");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation");
        assertThat(expr.evaluate(docElement)).as("Airport elevation value does not match").isEqualTo("40");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation/@uom");
        assertThat(expr.evaluate(docElement)).as("Airport elevation unit does not match").isEqualTo("FT");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsName");
        assertThat(expr.evaluate(docElement)).as("Airport reference point srsName does not match").isEqualTo("http://www.opengis.net/def/crs/EPSG/0/4326");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/gml:pos");
        assertThat(expr.evaluate(docElement)).as("Airport reference point position value does not match").isEqualTo("24.8325 59.413333");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation");
        assertThat(expr.evaluate(docElement)).as("Airport reference point elevation value does not match").isEqualTo("40");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation/@uom");
        assertThat(expr.evaluate(docElement)).as("Airport reference point elevation unit does not match").isEqualTo("M");

        //Sampling point:

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/@srsName");
        assertThat(expr.evaluate(docElement)).as("Sampling point srsName does not match").isEqualTo("http://www.opengis.net/def/crs/EPSG/0/4326");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/gml:pos");
        assertThat(expr.evaluate(docElement)).as("Sampling point position value does not match").isEqualTo("24.8325 59.413333");

        //Forecast properties:

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("CAVOK does not match").isEqualTo("false");

        //Wind:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertThat(expr.evaluate(docElement)).as("Variable wind not match").isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertThat(expr.evaluate(docElement)).as("Mean wind direction uom does not match").isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Mean wind direction does not match").isCloseTo(140.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Mean wind speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Mean wind speed does not match").isCloseTo(16.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Wind gust speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Wind gust speed does not match").isCloseTo(26.0, within(0.00001));

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Visibility does not match").isCloseTo(8000.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Visibility uom does not match").isEqualTo("m");

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer)");
        assertThat(expr.evaluate(docElement)).as("Cloud layer count does not match").isEqualTo("2");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Cloud layer 1 base does not match").isCloseTo(1000.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[2]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Cloud layer 1 base does not match").isCloseTo(1500.0, within(0.00001));

        //Temperatures:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperature/@uom");
        assertThat(expr.evaluate(docElement)).as("Max air temperature uom does not match").isEqualTo("degC");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperature");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Max air temperature does not match").isCloseTo(-2.0, within(0.00001));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:maximumAirTemperatureTime");
        assertThat(expr.evaluate(docElement)).as("Max air temperature time does not match").isEqualTo("2017-07-30T15:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperature/@uom");
        assertThat(expr.evaluate(docElement)).as("Min air temperature uom does not match").isEqualTo("degC");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperature");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Min air temperature does not match").isCloseTo(-10.0, within(0.00001));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:temperature/iwxxm"
                + ":AerodromeAirTemperatureForecast/iwxxm:minimumAirTemperatureTime");
        assertThat(expr.evaluate(docElement)).as("Min air temperature time does not match").isEqualTo("2017-07-31T03:00:00Z");

        //Change forecast 1: TEMPO 3012/3018 3000 RADZ BR OVC004

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:type/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 type does not match").isEqualTo("http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast");

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 1 phenomenonTime begin pos does not match").isEqualTo("2017-07-30T12:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 1 phenomenonTime end pos does not match").isEqualTo("2017-07-30T18:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 1 resultTime does not refer to msg issueTime").isEqualTo("#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 1 validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:procedure/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 Procedure does not refer to base forecast procedure").isEqualTo("#" + procedureId);

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 Observed properties does not match").isEqualTo("http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast");

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 FOI reference does not point to base forecast FOI").isEqualTo("#" + foiId);

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 change indicator does not match").isEqualTo("TEMPORARY_FLUCTUATIONS");

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 1 Visibility does not match").isCloseTo(3000.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 visibility uom does not match").isEqualTo("m");

        //Weather:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[1]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 weather 1 does not match").isEqualTo("http://codes.wmo.int/306/4678/RADZ");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[2]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 weather 2 does not match").isEqualTo("http://codes.wmo.int/306/4678/BR");

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer count does not match").isEqualTo("1");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 1 cloud layer 1 base does not match").isCloseTo(400.0, within(0.00001));

        //Change forecast 2: BECMG 3018/3020 BKN008 SCT015CB

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:type/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 type does not match").isEqualTo("http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast");

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 2 phenomenonTime begin pos does not match").isEqualTo("2017-07-30T18:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 2 phenomenonTime end pos does not match").isEqualTo("2017-07-30T20:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 2 resultTime does not refer to msg issueTime").isEqualTo("#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 2 validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:procedure/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 Procedure does not refer to base forecast procedure").isEqualTo("#" + procedureId);

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 observed properties does not match").isEqualTo("http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast");

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 FOI reference does not point to base forecast FOI").isEqualTo("#" + foiId);

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 change indicator does not match").isEqualTo("BECOMING");

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord" + "/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer count does not match").isEqualTo("2");

        //BKN008
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 2 cloud layer 1 base does not match").isCloseTo(800.0, within(0.00001));

        //SCT015CB
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 2 cloud layer 2 base does not match").isCloseTo(1500.0, within(0.00001));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 type does not match").isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        //Change forecast 3: TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:type/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 type does not match").isEqualTo("http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast");

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 3 phenomenonTime begin pos does not match").isEqualTo("2017-07-31T02:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 3 phenomenonTime end pos does not match").isEqualTo("2017-07-31T12:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 3 resultTime does not refer to msg issueTime").isEqualTo("#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 3 validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:procedure/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 procedure does not refer to base forecast procedure").isEqualTo("#" + procedureId);

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 observed properties does not match").isEqualTo("http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast");

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 FOI reference does not point to base forecast FOI").isEqualTo("#" + foiId);

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 change indicator does not match").isEqualTo("TEMPORARY_FLUCTUATIONS");

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord" + "/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 3 Visibility does not match").isCloseTo(3000.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 visibility uom does not match").isEqualTo("m");

        //Weather:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[1]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 weather 1 does not match").isEqualTo("http://codes.wmo.int/306/4678/SHRASN");

        //Cloud:

        //BKN006
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 3 cloud layer 1 base does not match").isCloseTo(600.0, within(0.00001));

        //BKN015CB
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 3 cloud layer 2 base does not match").isCloseTo(1500.0, within(0.00001));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 type does not match").isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        //Change forecast 4: BECMG 3104/3106 21016G30KT VV001=

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:type/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 type does not match").isEqualTo("http://codes.wmo" + ".int/49-2/observation-type/iwxxm/2.1/MeteorologicalAerodromeForecast");

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 4 phenomenonTime begin pos does not match").isEqualTo("2017-07-31T04:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 4 phenomenonTime end pos does not match").isEqualTo("2017-07-31T06:00:00Z");

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:resultTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 4 resultTime does not refer to msg issueTime").isEqualTo("#" + issueTimeId);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Change forecast 4 validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:procedure/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 procedure does not refer to base forecast procedure").isEqualTo("#" + procedureId);

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 observed properties does not match").isEqualTo("http://codes.wmo.int/49-2/observable-property/MeteorologicalAerodromeForecast");

        //FOI reference:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:featureOfInterest/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 FOI reference does not point to base forecast FOI").isEqualTo("#" + foiId);

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 change indicator does not match").isEqualTo("BECOMING");

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord" + "/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Wind: 21016G30KT
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 variable wind not match").isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Mean wind direction uom does not match").isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Mean wind direction does not match").isCloseTo(210.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Mean wind speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Mean wind speed does not match").isCloseTo(16.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Wind gust speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Wind gust speed does not match").isCloseTo(30.0, within(0.00001));

        //VV001
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:verticalVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 4 cloud vertical visibility value does not match").isCloseTo(100, within(0.00001));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[4]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:verticalVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 4 cloud vertical visibility uom does not match").isEqualTo("[ft_i]");
    }

}
