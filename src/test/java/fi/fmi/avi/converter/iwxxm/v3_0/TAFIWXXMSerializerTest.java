package fi.fmi.avi.converter.iwxxm.v3_0;

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
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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
        assertThatConversionResult(result).isSuccessful();
    }

    @Test
    public void serializeA51Taf() throws XPathExpressionException {
        final TAF input = createTafMessage();
        final ConversionResult<Document> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM, ConversionHints.EMPTY);
        final Document doc = assertThatConversionResult(result).isSuccessful().getMessage();

        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());

        XPathExpression expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-15T18:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T00:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T18:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("YUDO");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("DONLON/INTERNATIONAL");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("YUDO");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsDimension");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@axisLabels");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("Lat Lon");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/@srsName");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://www.opengis.net/def/crs/EPSG/0/4326");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/gml:pos");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("12.34 -12.34");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("M");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:elevation");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("12");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint/aixm:verticalDatum");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("EGM_96");

        //BASE FORECAST
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("9000.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("130.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m/s");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("5.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30"
                + ":layer/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2000.0");

        //Change forecasts
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("BECOMING");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T06:00:00Z");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T08:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("1500.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:cloudType/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2000.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/306/4678/TSRA");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("TEMPORARY_FLUCTUATIONS");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T08:00:00Z");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T12:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("1000.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("false");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30"
                + ":AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("170.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m/s");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("6.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:windGustSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m/s");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:windGustSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("12.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("1000.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:cloudType/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2000.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("TEMPORARY_FLUCTUATIONS");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T08:00:00Z");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T12:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("1000.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("FROM");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T12:30:00Z");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T18:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("1000.0");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibilityOperator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("ABOVE");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("150.0");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast/iwxxm30:meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("m/s");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast"
                + "/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2000.0");
    }

    @Test
    public void serializeA52Taf() throws XPathExpressionException {
        final TAF input = createTafMessageWithCancellation();
        final ConversionResult<Document> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM, ConversionHints.EMPTY);
        final Document doc = assertThatConversionResult(result).isSuccessful().getMessage();

        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());

        XPathExpression expr = xpath.compile("/iwxxm30:TAF/iwxxm30:issueTime/gml:TimeInstant/gml:timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T15:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("YUDO");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("DONLON/INTERNATIONAL");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("YUDO");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:cancelledReportValidPeriod/gml:TimePeriod/gml:beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T00:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:cancelledReportValidPeriod/gml:TimePeriod/gml:endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertThat(expResult).isEqualTo("2012-08-16T18:00:00Z");
    }

    @Test
    public void testTAFDOMSerialization() throws Exception {
        Assert.assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM));
        final TAF t = getTafObject();
        final ConversionResult<Document> result = converter.convertMessage(t, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM);

        final Document doc = assertThatConversionResult(result).isSuccessful().getMessage();

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
        assertThat(expr.evaluate(docElement)).as("issueTime does not match").isEqualTo("2012-08-30T11:30:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:beginPosition");
        assertThat(expr.evaluate(docElement)).as("validTime begin position does not match").isEqualTo("2017-07-30T12:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:validPeriod/gml:TimePeriod/gml:endPosition");
        assertThat(expr.evaluate(docElement)).as("validTime end position does not match").isEqualTo("2017-07-31T12:00:00Z");

        //Base forecast: EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z

        //Temporals:

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/@xlink:href");
        String timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Base forecast phenomenonTime does not refer to msg validTime").isEqualTo("#" + validTimeId);
/*
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:validTime/@xlink:href");
        timeRef = expr.evaluate(docElement);
        assertThat(timeRef).as("Base forecast validTime does not refer to msg validTime").isEqualTo("#" + validTimeId);
*/

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        assertThat(expr.evaluate(docElement)).as("Airport designator does not match").isEqualTo("EETN");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        assertThat(expr.evaluate(docElement)).as("Airport designator does not match").isEqualTo("EETN");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        assertThat(expr.evaluate(docElement)).as("Airport name does not match").isEqualTo("TALLINN AIRPORT");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:interpretation");
        assertThat(expr.evaluate(docElement)).as("Airport name does not match").isEqualTo("SNAPSHOT");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/aixm:elevation");
        assertThat(expr.evaluate(docElement)).as("Airport elevation value does not match").isEqualTo("40");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/aixm:elevation/@uom");
        assertThat(expr.evaluate(docElement)).as("Airport elevation value does not match").isEqualTo("M");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint" + "/@srsName");
        assertThat(expr.evaluate(docElement)).as("Airport reference point srsName does not match").isEqualTo("http://www.opengis.net/def/crs/EPSG/0/4326");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/@srsDimension");
        assertThat(expr.evaluate(docElement)).as("Airport reference point srsName does not match").isEqualTo("2");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint"
                + "/@axisLabels");
        assertThat(expr.evaluate(docElement)).as("Airport reference point srsName does not match").isEqualTo("Lat Lon");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:aerodrome/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:ARP/aixm:ElevatedPoint" + "/gml:pos");
        assertThat(expr.evaluate(docElement)).as("Airport reference point position value does not match").isEqualTo("24.8325 59.413333");

        //Wind:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertThat(expr.evaluate(docElement)).as("Variable wind not match").isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection/@uom");
        assertThat(expr.evaluate(docElement)).as("Mean wind direction uom does not match").isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Mean wind direction does not match").isCloseTo(140.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Mean wind speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Mean wind speed does not match").isCloseTo(16.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Wind gust speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Wind gust speed does not match").isCloseTo(26.0, within(0.00001));

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Visibility does not match").isCloseTo(8000.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Visibility uom does not match").isEqualTo("m");

        //iwxxm30:clouds:
        expr = xpath.compile(
                "count(/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer)");
        assertThat(expr.evaluate(docElement)).as("Cloud layer count does not match").isEqualTo("2");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Cloud layer 1 base does not match").isCloseTo(1000.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[2"
                        + "]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Cloud layer 1 base does not match").isCloseTo(1500.0, within(0.00001));

        //Temperatures:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperature/@uom");
        assertThat(expr.evaluate(docElement)).as("Max air temperature uom does not match").isEqualTo("degC");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperature");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Max air temperature does not match").isCloseTo(-2.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:maximumAirTemperatureTime");
        assertThat(expr.evaluate(docElement)).as("Max air temperature time does not match").isEqualTo("2017-07-30T15:00:00Z");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/"
                + "iwxxm30:temperature/iwxxm30:AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperature/@uom");
        assertThat(expr.evaluate(docElement)).as("Min air temperature uom does not match").isEqualTo("degC");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperature");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Min air temperature does not match").isCloseTo(-10.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:baseForecast/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:temperature/iwxxm30"
                + ":AerodromeAirTemperatureForecast/iwxxm30:minimumAirTemperatureTime");
        assertThat(expr.evaluate(docElement)).as("Min air temperature time does not match").isEqualTo("2017-07-31T03:00:00Z");

        //Change forecast 1: TEMPO 3012/3018 3000 RADZ BR OVC004

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-30T12:00:00Z").as("Change forecast 1 phenomenonTime begin pos does not match").isEqualTo(timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-30T18:00:00Z").as("Change forecast 1 phenomenonTime end pos does not match").isEqualTo(timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 change indicator does not match").isEqualTo("TEMPORARY_FLUCTUATIONS");

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 CAVOK does not match").isEqualTo("false");

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 1 Visibility does not match").isCloseTo(3000.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 visibility uom does not match").isEqualTo("m");

        //Weather:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[1]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 weather 1 does not match").isEqualTo("http://codes.wmo.int/306/4678/RADZ");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[2]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 1 weather 2 does not match").isEqualTo("http://codes.wmo.int/306/4678/BR");

        //clouds:
        expr = xpath.compile("count(/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer)");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer count does not match").isEqualTo("1");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30:CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/OVC");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30"
                        + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 1 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[1]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30:AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30"
                        + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 1 cloud layer 1 base does not match").isCloseTo(400.0, within(0.00001));

        //Change forecast 2: BECMG 3018/3020 BKN008 SCT015CB

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-30T18:00:00Z").as("Change forecast 2 phenomenonTime begin pos does not match").isEqualTo(timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-30T20:00:00Z").as("Change forecast 2 phenomenonTime end pos does not match").isEqualTo(timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 2 change indicator does not match").isEqualTo("BECOMING");

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //iwxxm30:clouds:
        expr = xpath.compile("count(/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer)");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer count does not match").isEqualTo("2");

        //BKN008
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 2 cloud layer 1 base does not match").isCloseTo(800.0, within(0.00001));

        //SCT015CB
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 2 cloud layer 2 base does not match").isCloseTo(1500.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[2]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:cloudType/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 2 cloud layer 2 type does not match").isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        //Change forecast 3: TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-31T02:00:00Z").as("Change forecast 3 phenomenonTime begin pos does not match").isEqualTo(timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml:endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-31T12:00:00Z").as("Change forecast 3 phenomenonTime end pos does not match").isEqualTo(timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 change indicator does not match").isEqualTo("TEMPORARY_FLUCTUATIONS");

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Visibility:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 3 Visibility does not match").isCloseTo(3000.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:prevailingVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 visibility uom does not match").isEqualTo("m");

        //Weather:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:weather[1]" + "/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change forecast 3 weather 1 does not match").isEqualTo("http://codes.wmo.int/306/4678/SHRASN");

        //iwxxm30:cloud:

        //BKN006
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 1 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 1 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[1]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 3 cloud layer 1 base does not match").isCloseTo(600.0, within(0.00001));

        //BKN015CB
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:amount/@xlink:href");

        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 amount does not match").isEqualTo("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN");
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 base uom does not match").isEqualTo("[ft_i]");

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:base");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 3 cloud layer 2 base does not match").isCloseTo(1500.0, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[3]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:layer[2]/iwxxm30" + ":CloudLayer/iwxxm30:cloudType/@xlink:href");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 3 cloud layer 2 type does not match").isEqualTo("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB");

        //Change forecast 4: BECMG 3104/3106 21016G30KT VV001=

        //Temporals:
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":beginPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-31T04:00:00Z").as("Change forecast 4 phenomenonTime begin pos does not match").isEqualTo(timeRef);

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:phenomenonTime/gml:TimePeriod/gml" + ":endPosition");
        timeRef = expr.evaluate(docElement);
        assertThat("2017-07-31T06:00:00Z").as("Change forecast 4 phenomenonTime end pos does not match").isEqualTo(timeRef);

        //Change indicator:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/@changeIndicator");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 change indicator does not match").isEqualTo("BECOMING");

        //CAVOK:
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/@cloudAndVisibilityOK");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 CAVOK does not match").isEqualTo("false");

        //Forecast properties:

        //Wind: 21016G30KT
        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 variable wind not match").isEqualTo("false");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Mean wind direction uom does not match").isEqualTo("deg");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindDirection");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Mean wind direction does not match").isCloseTo(210.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Mean wind speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:meanWindSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Mean wind speed does not match").isCloseTo(16.0, within(0.00001));

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed/@uom");
        assertThat(expr.evaluate(docElement)).as("Change forecast 4 Wind gust speed uom does not match").isEqualTo("[kn_i]");

        expr = xpath.compile(
                "/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:surfaceWind/iwxxm30:AerodromeSurfaceWindForecast"
                        + "/iwxxm30:windGustSpeed");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change forecast 4 Wind gust speed does not match").isCloseTo(30.0, within(0.00001));

        //VV001
        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:verticalVisibility");
        assertThat(Double.parseDouble(expr.evaluate(docElement))).as("Change Forecast 4 cloud vertical visibility value does not match").isCloseTo(100, within(0.00001));

        expr = xpath.compile("/iwxxm30:TAF/iwxxm30:changeForecast[4]/iwxxm30:MeteorologicalAerodromeForecast/iwxxm30:cloud/iwxxm30"
                + ":AerodromeCloudForecast/iwxxm30:verticalVisibility/@uom");
        assertThat(expr.evaluate(docElement)).as("Change Forecast 4 cloud vertical visibility uom does not match").isEqualTo("[ft_i]");

    }

    private TAF getTafObject() throws IOException {
        final TAF t = readFromJSON("taf12.json", TAFImpl.class);
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
