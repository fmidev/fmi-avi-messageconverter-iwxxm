package fi.fmi.avi.converter.iwxxm.v3_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.*;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMSerializerTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void confTest() {
        Assert.assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING));
        Assert.assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM));
    }

    @Test
    public void conversionTest() throws IOException {
        final TAF input = getTafObject();
        final ConversionResult<String> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, ConversionHints.EMPTY);
        assertConversionResult(result).assertSuccessful();
    }

    @Test
    public void serializeA51Taf() throws XPathExpressionException {
        final TAF input = createTafMessage();
        final ConversionResult<Document> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM, ConversionHints.EMPTY);
        final Document doc = assertConversionResult(result).isSuccessful();

        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());

        XPathExpression expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-15T18:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T00:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("DONLON/INTERNATIONAL", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsDimension");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@axisLabels");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("Lat Lon", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsName");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/gml:pos");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12.34 -12.34", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("M", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:verticalDatum");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("EGM_96", expResult);

        //BASE FORECAST
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("9000.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("130.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("5.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30"
                + ":layer/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        //Change forecasts
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("BECOMING", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T06:00:00Z", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1500.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:cloudType/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/306/4678/TSRA", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("TEMPORARY_FLUCTUATIONS", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30"
                + ":AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("170.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("6.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:windGustSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:windGustSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:cloudType/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("TEMPORARY_FLUCTUATIONS", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("FROM", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:30:00Z", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibilityOperator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("ABOVE", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("150.0", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);
    }

    @Test
    public void serializeA52Taf() throws XPathExpressionException {
        final TAF input = createTafMessageWithCancellation();
        final ConversionResult<Document> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM, ConversionHints.EMPTY);
        final Document doc = assertConversionResult(result).isSuccessful();

        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());

        XPathExpression expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T15:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("DONLON/INTERNATIONAL", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:cancelledReportValidPeriod/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T00:00:00Z", expResult);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:cancelledReportValidPeriod/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);
    }

    @Test
    public void testTAFDOMSerialization() throws Exception {
        Assert.assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM));
        final TAF t = getTafObject();
        final ConversionResult<Document> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM);

        final Document doc = assertConversionResult(result).isSuccessful();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        final Element docElement = doc.getDocumentElement();

        XPathExpression expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/@gml:id");
        final String issueTimeId = expr.evaluate(docElement);
        assertNotNull(issueTimeId);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/@gml:id");
        final String validTimeId = expr.evaluate(docElement);
        assertNotNull(validTimeId);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        Assert.assertEquals("issueTime does not match", "2012-08-30T11:30:00Z", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:beginPosition");
        Assert.assertEquals("validTime begin position does not match", "2017-07-30T12:00:00Z", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:endPosition");
        Assert.assertEquals("validTime end position does not match", "2017-07-31T12:00:00Z", expr.evaluate(docElement));

        //Base forecast: EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z

        //Temporals:

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/@xlink:href");
        String timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Base forecast phenomenonTime does not refer to msg validTime", timeRef, "#" + validTimeId);
/*
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Base forecast validTime does not refer to msg validTime", timeRef, "#" + validTimeId);
*/

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        Assert.assertEquals("Airport designator does not match", "EETN", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        Assert.assertEquals("Airport designator does not match", "EETN", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        Assert.assertEquals("Airport name does not match", "TALLINN AIRPORT", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:interpretation");
        Assert.assertEquals("Airport name does not match", "SNAPSHOT", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/aixm:elevation");
        Assert.assertEquals("Airport elevation value does not match", "40", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/aixm:elevation/@uom");
        Assert.assertEquals("Airport elevation value does not match", "M", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint" + "/@srsName");
        Assert.assertEquals("Airport reference point srsName does not match", "http://www.opengis.net/def/crs/EPSG/0/4326", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/@srsDimension");
        Assert.assertEquals("Airport reference point srsName does not match", "2", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/@axisLabels");
        Assert.assertEquals("Airport reference point srsName does not match", "Lat Lon", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint" + "/gml:pos");
        Assert.assertEquals("Airport reference point position value does not match", "24.8325 59.413333", expr.evaluate(docElement));

        //Wind:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        Assert.assertEquals("Variable wind not match", "false", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection/@uom");
        Assert.assertEquals("Mean wind direction uom does not match", "deg", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection");
        Assert.assertEquals("Mean wind direction does not match", Double.parseDouble(expr.evaluate(docElement)), 140.0, 0.00001);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed/@uom");
        Assert.assertEquals("Mean wind speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed");
        Assert.assertEquals("Mean wind speed does not match", Double.parseDouble(expr.evaluate(docElement)), 16.0, 0.00001);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed/@uom");
        Assert.assertEquals("Wind gust speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed");
        Assert.assertEquals("Wind gust speed does not match", Double.parseDouble(expr.evaluate(docElement)), 26.0, 0.00001);

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        Assert.assertEquals("Visibility does not match", Double.parseDouble(expr.evaluate(docElement)), 8000.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        Assert.assertEquals("Visibility uom does not match", "m", expr.evaluate(docElement));

        //iwxxm30:clouds:
        expr = xpath.compile(
                "count(/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer)");
        Assert.assertEquals("Cloud layer count does not match", "2", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Cloud layer 1 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(docElement)), 1000.0, 0.00001);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Cloud layer 1 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(docElement)), 1500.0, 0.00001);

        //Temperatures:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperature/@uom");
        Assert.assertEquals("Max air temperature uom does not match", "degC", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperature");
        Assert.assertEquals("Max air temperature does not match", Double.parseDouble(expr.evaluate(docElement)), -2.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperatureTime");
        Assert.assertEquals("Max air temperature time does not match", "2017-07-30T15:00:00Z", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/"
                + "iwxxm30:temperature/iwxxm30:AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperature/@uom");
        Assert.assertEquals("Min air temperature uom does not match", "degC", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperature");
        Assert.assertEquals("Min air temperature does not match", Double.parseDouble(expr.evaluate(docElement)), -10.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperatureTime");
        Assert.assertEquals("Min air temperature time does not match", "2017-07-31T03:00:00Z", expr.evaluate(docElement));

        //Change forecast 1: TEMPO 3012/3018 3000 RADZ BR OVC004

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 1 phenomenonTime begin pos does not match", "2017-07-30T12:00:00Z", timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":endPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 1 phenomenonTime end pos does not match", "2017-07-30T18:00:00Z", timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        Assert.assertEquals("Change forecast 1 change indicator does not match", "TEMPORARY_FLUCTUATIONS", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        Assert.assertEquals("Change forecast 1 CAVOK does not match", "false", expr.evaluate(docElement));

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        Assert.assertEquals("Change forecast 1 Visibility does not match", Double.parseDouble(expr.evaluate(docElement)), 3000.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        Assert.assertEquals("Change forecast 1 visibility uom does not match", "m", expr.evaluate(docElement));

        //Weather:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[1]" + "/@xlink:href");
        Assert.assertEquals("Change forecast 1 weather 1 does not match", "http://codes.wmo.int/306/4678/RADZ", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[2]" + "/@xlink:href");
        Assert.assertEquals("Change forecast 1 weather 2 does not match", "http://codes.wmo.int/306/4678/BR", expr.evaluate(docElement));

        //clouds:
        expr = xpath.compile("count(/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer)");
        Assert.assertEquals("Change Forecast 1 cloud layer count does not match", "1", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Change Forecast 1 cloud layer 1 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC",
                expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30"
                        + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Change Forecast 1 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30"
                        + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Change Forecast 1 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(docElement)), 400.0, 0.00001);

        //Change forecast 2: BECMG 3018/3020 BKN008 SCT015CB

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":beginPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 2 phenomenonTime begin pos does not match", "2017-07-30T18:00:00Z", timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 2 phenomenonTime end pos does not match", "2017-07-30T20:00:00Z", timeRef);

        final String procedureId = "";

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        Assert.assertEquals("Change forecast 2 change indicator does not match", "BECOMING", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        Assert.assertEquals("CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //iwxxm30:clouds:
        expr = xpath.compile("count(/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer)");
        Assert.assertEquals("Change Forecast 2 cloud layer count does not match", "2", expr.evaluate(docElement));

        //BKN008
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Change Forecast 2 cloud layer 1 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN",
                expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Change Forecast 2 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Change Forecast 2 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(docElement)), 800.0, 0.00001);

        //SCT015CB
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Change Forecast 2 cloud layer 2 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT",
                expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Change Forecast 2 cloud layer 2 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Change Forecast 2 cloud layer 2 base does not match", Double.parseDouble(expr.evaluate(docElement)), 1500.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:cloudType/@xlink:href");
        Assert.assertEquals("Change Forecast 2 cloud layer 2 type does not match", "http://codes.wmo.int/49-2/SigConvectiveCloudType/CB",
                expr.evaluate(docElement));

        //Change forecast 3: TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 3 phenomenonTime begin pos does not match", "2017-07-31T02:00:00Z", timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 3 phenomenonTime end pos does not match", "2017-07-31T12:00:00Z", timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        Assert.assertEquals("Change forecast 3 change indicator does not match", "TEMPORARY_FLUCTUATIONS", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        Assert.assertEquals("Change forecast 3 CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        Assert.assertEquals("Change forecast 3 Visibility does not match", Double.parseDouble(expr.evaluate(docElement)), 3000.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        Assert.assertEquals("Change forecast 3 visibility uom does not match", "m", expr.evaluate(docElement));

        //Weather:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[1]" + "/@xlink:href");
        Assert.assertEquals("Change forecast 3 weather 1 does not match", "http://codes.wmo.int/306/4678/SHRASN", expr.evaluate(docElement));

        //iwxxm30:cloud:

        //BKN006
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        Assert.assertEquals("Change Forecast 3 cloud layer 1 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN",
                expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Change Forecast 3 cloud layer 1 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Change Forecast 3 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(docElement)), 600.0, 0.00001);

        //BKN015CB
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");

        Assert.assertEquals("Change Forecast 3 cloud layer 2 amount does not match", "http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN",
                expr.evaluate(docElement));
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        Assert.assertEquals("Change Forecast 3 cloud layer 2 base uom does not match", "[ft_i]", expr.evaluate(docElement));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        Assert.assertEquals("Change Forecast 3 cloud layer 2 base does not match", Double.parseDouble(expr.evaluate(docElement)), 1500.0, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:cloudType/@xlink:href");
        Assert.assertEquals("Change Forecast 3 cloud layer 2 type does not match", "http://codes.wmo.int/49-2/SigConvectiveCloudType/CB",
                expr.evaluate(docElement));

        //Change forecast 4: BECMG 3104/3106 21016G30KT VV001=

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":beginPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 4 phenomenonTime begin pos does not match", "2017-07-31T04:00:00Z", timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":endPosition");
        timeRef = expr.evaluate(docElement);
        Assert.assertEquals("Change forecast 4 phenomenonTime end pos does not match", "2017-07-31T06:00:00Z", timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        Assert.assertEquals("Change forecast 4 change indicator does not match", "BECOMING", expr.evaluate(docElement));

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        Assert.assertEquals("Change forecast 4 CAVOK does not match", "false", expr.evaluate(docElement));

        //Forecast properties:

        //Wind: 21016G30KT
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        Assert.assertEquals("Change forecast 4 variable wind not match", "false", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection/@uom");
        Assert.assertEquals("Change forecast 4 Mean wind direction uom does not match", "deg", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection");
        Assert.assertEquals("Change forecast 4 Mean wind direction does not match", Double.parseDouble(expr.evaluate(docElement)), 210.0, 0.00001);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed/@uom");
        Assert.assertEquals("Change forecast 4 Mean wind speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed");
        Assert.assertEquals("Change forecast 4 Mean wind speed does not match", Double.parseDouble(expr.evaluate(docElement)), 16.0, 0.00001);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed/@uom");
        Assert.assertEquals("Change forecast 4 Wind gust speed uom does not match", "[kn_i]", expr.evaluate(docElement));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed");
        Assert.assertEquals("Change forecast 4 Wind gust speed does not match", Double.parseDouble(expr.evaluate(docElement)), 30.0, 0.00001);

        //VV001
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:verticalVisibility");
        Assert.assertEquals("Change Forecast 4 cloud vertical visibility value does not match", Double.parseDouble(expr.evaluate(docElement)), 100, 0.00001);

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:verticalVisibility/@uom");
        Assert.assertEquals("Change Forecast 4 cloud vertical visibility uom does not match", "[ft_i]", expr.evaluate(docElement));

    }


    private TAF getTafObject() throws IOException {
        final TAF t = readFromJSON("taf12.json");
        final AerodromeImpl.Builder airportBuilder = AerodromeImpl.builder()
                .setDesignator("EETN")
                .setName("TALLINN AIRPORT")
                .setFieldElevationValue(40.0)
                .setFieldElevationUom("FT")
                .setLocationIndicatorICAO("EETN")
                .setReferencePoint(ElevatedPointImpl.builder()
                        .setCrs(CoordinateReferenceSystemImpl.wgs84())
                        .addCoordinates(24.8325, 59.413333)
                        .setElevationValue(40.0)
                        .setElevationUom("M")
                        .build());
        final TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
        tafBuilder.setAerodrome(airportBuilder.build())
                .withCompleteIssueTime(YearMonth.of(2012, 8))
                .withCompleteForecastTimes(YearMonth.of(2017, 7), 30, 11, ZoneId.of("Z"))
                .setTranslatedTAC("EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z\n" + "TEMPO 3012/3018 3000 RADZ BR OVC004\n"
                        + "BECMG 3018/3020 BKN008 SCT015CB\n" + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" + "BECMG 3104/3106 21016G30KT=")
                .setTranslationTime(ZonedDateTime.now());

        return tafBuilder.build();
    }

    protected TAF readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = fi.fmi.avi.converter.iwxxm.v2_1.TAFIWXXMSerializerTest.class.getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, TAFImpl.class);
            } else {
                throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
            }
        }
    }

    private TAF createTafMessageWithCancellation() {
        final TAFImpl.Builder tafBuilder = TAFImpl.builder();
        tafBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 15, 0, 0, 0, ZoneId.of("Z"))));

        tafBuilder.setAerodrome(createAerodrome());

        final PartialOrCompleteTimePeriod cancelPeriod = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 0, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        tafBuilder.setReferredReportValidPeriod(cancelPeriod);

        tafBuilder.setCancelMessage(true);

        tafBuilder.setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT);

        return tafBuilder.build();
    }

    private TAF createTafMessage() {
        final TAFImpl.Builder tafBuilder = TAFImpl.builder();
        tafBuilder.setReportStatus(AviationWeatherMessage.ReportStatus.NORMAL);
        tafBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 15, 18, 0, 0, 0, ZoneId.of("Z"))));

        tafBuilder.setValidityTime(PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 0, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build());

        tafBuilder.setAerodrome(createAerodrome());
        tafBuilder.setBaseForecast(createBaseForecast());

        final List<TAFChangeForecast> forecasts = new ArrayList<>();

        final PartialOrCompleteTimePeriod t1 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 6, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 8, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        final CloudForecast f1 = CloudForecastImpl.builder()
                .setLayers(Arrays.asList(CloudLayerImpl.builder()
                                .setAmount(AviationCodeListUser.CloudAmount.SCT)
                                .setBase(NumericMeasureImpl.of(1500.0, "[ft_i]"))
                                .setCloudType(AviationCodeListUser.CloudType.CB)
                                .build(),
                        CloudLayerImpl.builder().setAmount(AviationCodeListUser.CloudAmount.BKN).setBase(NumericMeasureImpl.of(2000.0, "[ft_i]")).build()))
                .build();

        //Weather
        final List<Weather> w1 = Collections.singletonList(WeatherImpl.builder().setCode("TSRA").build());

        forecasts.add(TAFChangeForecastImpl.builder()//
                .setPeriodOfChange(t1)//
                .setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.BECOMING)//
                .setNullableCloud(f1)//
                .setNullablePrevailingVisibility(null)//
                .setNullablePrevailingVisibilityOperator(null)//
                .setNullableSurfaceWind(null)//
                .setNullableForecastWeather(w1)//
                .build());

        final PartialOrCompleteTimePeriod t2 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 8, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 12, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        final CloudForecast f2 = CloudForecastImpl.builder()
                .setLayers(Arrays.asList(CloudLayerImpl.builder()
                                .setAmount(AviationCodeListUser.CloudAmount.SCT)
                                .setBase(NumericMeasureImpl.of(1000.0, "[ft_i]"))
                                .setCloudType(AviationCodeListUser.CloudType.CB)
                                .build(),
                        CloudLayerImpl.builder().setAmount(AviationCodeListUser.CloudAmount.BKN).setBase(NumericMeasureImpl.of(2000.0, "[ft_i]")).build()))
                .build();

        final SurfaceWind w2 = SurfaceWindImpl.builder()
                .setVariableDirection(false)
                .setMeanWindDirection(NumericMeasureImpl.of(170.0, "deg"))
                .setMeanWindSpeed(NumericMeasureImpl.of(6.0, "m/s"))
                .setWindGust(NumericMeasureImpl.of(12.0, "m/s"))
                .build();

        forecasts.add(TAFChangeForecastImpl.builder()//
                .setPeriodOfChange(t2)//
                .setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS)//
                .setNullableCloud(f2)//
                .setNullablePrevailingVisibility(NumericMeasureImpl.of(1000.0, "m"))//
                .setNullablePrevailingVisibilityOperator(null)//
                .setNullableSurfaceWind(w2)//
                .setNullableForecastWeather(null)//
                .build());

        final PartialOrCompleteTimePeriod t3 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 12, 30, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        final CloudForecast f3 = CloudForecastImpl.builder()
                .setLayers(Collections.singletonList(
                        CloudLayerImpl.builder().setAmount(AviationCodeListUser.CloudAmount.BKN).setBase(NumericMeasureImpl.of(2000.0, "[ft_i]")).build()))
                .build();

        final SurfaceWind w3 = SurfaceWindImpl.builder()
                .setVariableDirection(false)
                .setMeanWindDirection(NumericMeasureImpl.of(150.0, "deg"))
                .setMeanWindSpeed(NumericMeasureImpl.of(4.0, "m/s"))
                .build();

        forecasts.add(TAFChangeForecastImpl.builder()//
                .setPeriodOfChange(t3)//
                .setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.FROM)//
                .setNullableCloud(f3)//
                .setNullablePrevailingVisibility(NumericMeasureImpl.of(1000.0, "m"))//
                .setNullablePrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.fromInt(0))//
                .setNullableSurfaceWind(w3)//
                .setNullableForecastWeather(null)//
                .build());

        tafBuilder.setChangeForecasts(forecasts);

        return tafBuilder.build();
    }

    private Aerodrome createAerodrome() {
        return AerodromeImpl.builder()//
                .setName("DONLON/INTERNATIONAL")//
                .setDesignator("YUDO")//
                .setLocationIndicatorICAO(Optional.of("YUDO"))//
                .setReferencePoint(ElevatedPointImpl.builder()//
                        .setCoordinates(Arrays.asList(12.34, -12.34))//
                        .setElevationUom("M")//
                        .setElevationValue(12)//
                        .setVerticalDatum(Optional.of("EGM_96"))//
                        .setCrs(CoordinateReferenceSystemImpl.wgs84())//
                        .build())//
                .build();
    }

    private TAFBaseForecast createBaseForecast() {
        return TAFBaseForecastImpl.builder()//
                .setPrevailingVisibility(NumericMeasureImpl.of(9000.0, "m"))//
                .setSurfaceWind(SurfaceWindImpl.builder()//
                        .setVariableDirection(false)//
                        .setMeanWindDirection(NumericMeasureImpl.of(130.0, "deg"))//
                        .setMeanWindSpeed(NumericMeasureImpl.of(5.0, "m/s"))//
                        .build())//
                .setCloud(CloudForecastImpl.builder()//
                        .setLayers(Collections.singletonList(CloudLayerImpl.builder()//
                                .setAmount(AviationCodeListUser.CloudAmount.BKN)//
                                .setBase(NumericMeasureImpl.of(2000.0, "[ft_i]"))//
                                .build()))//
                        .build())//
                .build();
    }
}
