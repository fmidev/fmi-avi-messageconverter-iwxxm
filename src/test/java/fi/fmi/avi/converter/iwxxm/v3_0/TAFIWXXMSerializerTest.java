package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMSerializerTest {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void confTest() throws IOException {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING));
    }

    @Test
    public void conversionTest() throws IOException {
        TAF input = getTafObject();

        final ConversionResult<String> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, ConversionHints.EMPTY);

        checkConversionIssues(result.getConversionIssues());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        Assert.assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void serializeA51Taf() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        TAF input = createTafMessage();

        final ConversionResult<String> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, ConversionHints.EMPTY);

        checkConversionIssues(result.getConversionIssues());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        //System.out.println(result.getConvertedMessage().get());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(result.getConvertedMessage().get())));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        XPathExpression expr = xpath.compile("/TAF/issueTime/TimeInstant/timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-15T18:00:00Z", expResult);

        expr = xpath.compile("/TAF/validPeriod/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T00:00:00Z", expResult);

        expr = xpath.compile("/TAF/validPeriod/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("DONLON/INTERNATIONAL", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/@srsDimension");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/@axisLabels");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("Lat Long", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/@srsName");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/pos");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12.34 -12.34", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/elevation/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("M", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/elevation");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12.0", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/ARP/ElevatedPoint/verticalDatum");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("EGM_96", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("9000.0", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("130.0", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("5.0", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/baseForecast/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        //Change forecasts
        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("BECOMING", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T06:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1500.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/cloudType/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[1]/MeteorologicalAerodromeForecast/weather/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/306/4678/TSRA", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("TEMPORARY_FLUCTUATIONS", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("170.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("6.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/windGustSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/windGustSpeed");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("12.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/SCT", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/cloudType/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/SigConvectiveCloudType/CB", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[2]/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("TEMPORARY_FLUCTUATIONS", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T08:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/TAF/changeForecast[2]/MeteorologicalAerodromeForecast/prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/@changeIndicator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("FROM", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T12:30:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/phenomenonTime/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/prevailingVisibility/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/prevailingVisibility");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("1000.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/prevailingVisibilityOperator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("ABOVE", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/@variableWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("false", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("deg", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindDirection");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("150.0", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/surfaceWind/AerodromeSurfaceWindForecast/meanWindSpeed/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("m/s", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/amount/@href");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("http://codes.wmo.int/49-2/CloudAmountReportedAtAerodrome/BKN", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base/@uom");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("[ft_i]", expResult);

        expr = xpath.compile("/TAF/changeForecast[3]/MeteorologicalAerodromeForecast/cloud/AerodromeCloudForecast/layer[1]/CloudLayer/base");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2000.0", expResult);
    }

    @Test
    public void serializeA52Taf() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        TAF input = createTafMessageWithCancellation();

        final ConversionResult<String> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, ConversionHints.EMPTY);

        checkConversionIssues(result.getConversionIssues());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        //System.out.println(result.getConvertedMessage().get());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(result.getConvertedMessage().get())));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        XPathExpression expr = xpath.compile("/TAF/issueTime/TimeInstant/timePosition");
        Object expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T15:00:00Z", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/designator");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/name");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("DONLON/INTERNATIONAL", expResult);

        expr = xpath.compile("/TAF/aerodrome/AirportHeliport/timeSlice/AirportHeliportTimeSlice/locationIndicatorICAO");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("YUDO", expResult);

        expr = xpath.compile("/TAF/cancelledReportValidPeriod/TimePeriod/beginPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T00:00:00Z", expResult);

        expr = xpath.compile("/TAF/cancelledReportValidPeriod/TimePeriod/endPosition");
        expResult = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals("2012-08-16T18:00:00Z", expResult);
    }

    private void checkConversionIssues(List<ConversionIssue> issues) {
        if (!issues.isEmpty()) {
            for (ConversionIssue issue : issues) {
                System.out.println("******************");
                System.out.println("Severity: " + issue.getSeverity());
                System.out.println(issue.getMessage());
                System.out.println("******************");
                fail();
            }
        }
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
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        final InputStream is = fi.fmi.avi.converter.iwxxm.v2_1.TAFIWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            return om.readValue(is, TAFImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }

    private TAF createTafMessageWithCancellation() {
        TAFImpl.Builder tafBuilder = TAFImpl.builder();
        tafBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 15, 0, 0, 0, ZoneId.of("Z"))));

        tafBuilder.setAerodrome(createAeroDrome());

        PartialOrCompleteTimePeriod cancelPeriod = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 0, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        tafBuilder.setCancelledReportValidPeriod(cancelPeriod);

        tafBuilder.setCancelMessage(true);

        tafBuilder.setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT);

        return tafBuilder.build();
    }

    private TAF createTafMessage() {
        TAFImpl.Builder tafBuilder = TAFImpl.builder();
        tafBuilder.setReportStatus(AviationWeatherMessage.ReportStatus.NORMAL);
        tafBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 15, 18, 0, 0, 0, ZoneId.of("Z"))));

        tafBuilder.setValidityTime(PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 0, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build());

        tafBuilder.setAerodrome(createAeroDrome());
        tafBuilder.setBaseForecast(createBaseForecast());

        List<TAFChangeForecast> forecasts = new ArrayList<>();

        PartialOrCompleteTimePeriod t1 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 6, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 8, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        CloudForecast f1 = CloudForecastImpl.builder()
                .setLayers(Arrays.asList(CloudLayerImpl.builder()
                        .setAmount(AviationCodeListUser.CloudAmount.SCT)
                        .setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(1500d).build())
                        .setCloudType(AviationCodeListUser.CloudType.CB)
                        .build(), CloudLayerImpl.builder()
                        .setAmount(AviationCodeListUser.CloudAmount.BKN)
                        .setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(2000d).build())
                        .build()))
                .build();

        //Weather
        List<Weather> w1 = Arrays.asList(WeatherImpl.builder().setCode("TSRA").build());

        forecasts.add(createChangeForecast(t1, AviationCodeListUser.TAFChangeIndicator.BECOMING, f1, null, null, null, w1));

        PartialOrCompleteTimePeriod t2 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 8, 0, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 12, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        CloudForecast f2 = CloudForecastImpl.builder()
                .setLayers(Arrays.asList(CloudLayerImpl.builder()
                        .setAmount(AviationCodeListUser.CloudAmount.SCT)
                        .setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(1000d).build())
                        .setCloudType(AviationCodeListUser.CloudType.CB)
                        .build(), CloudLayerImpl.builder()
                        .setAmount(AviationCodeListUser.CloudAmount.BKN)
                        .setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(2000d).build())
                        .build()))
                .build();

        SurfaceWind w2 = SurfaceWindImpl.builder()
                .setVariableDirection(false)
                .setMeanWindDirection(NumericMeasureImpl.builder().setUom("deg").setValue(170d).build())
                .setMeanWindSpeed(NumericMeasureImpl.builder().setUom("m/s").setValue(6d).build())
                .setWindGust(NumericMeasureImpl.builder().setUom("m/s").setValue(12d).build())
                .build();

        forecasts.add(createChangeForecast(t2, AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS, f2,
                NumericMeasureImpl.builder().setUom("m").setValue(1000d).build(), null, w2, null));

        PartialOrCompleteTimePeriod t3 = PartialOrCompleteTimePeriod.builder()
                .setStartTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 12, 30, 0, 0, ZoneId.of("Z"))))
                .setEndTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2012, 8, 16, 18, 0, 0, 0, ZoneId.of("Z"))))
                .build();

        CloudForecast f3 = CloudForecastImpl.builder()
                .setLayers(Arrays.asList(CloudLayerImpl.builder()
                        .setAmount(AviationCodeListUser.CloudAmount.BKN)
                        .setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(2000d).build())
                        .build()))
                .build();

        SurfaceWind w3 = SurfaceWindImpl.builder()
                .setVariableDirection(false)
                .setMeanWindDirection(NumericMeasureImpl.builder().setUom("deg").setValue(150d).build())
                .setMeanWindSpeed(NumericMeasureImpl.builder().setUom("m/s").setValue(4d).build())
                .build();

        forecasts.add(
                createChangeForecast(t3, AviationCodeListUser.TAFChangeIndicator.FROM, f3, NumericMeasureImpl.builder().setUom("m").setValue(1000d).build(), 0,
                        w3, null));

        tafBuilder.setChangeForecasts(forecasts);

        return tafBuilder.build();
    }

    private Aerodrome createAeroDrome() {
        AerodromeImpl.Builder builder = AerodromeImpl.builder();
        builder.setName("DONLON/INTERNATIONAL");
        builder.setDesignator("YUDO");
        builder.setLocationIndicatorICAO(Optional.of("YUDO"));

        ElevatedPointImpl.Builder point = ElevatedPointImpl.builder();
        point.setCoordinates(Arrays.asList(12.34, -12.34));
        point.setElevationUom("M");
        point.setElevationValue(12);
        point.setVerticalDatum(Optional.of("EGM_96"));
        builder.setReferencePoint(point.build());
        return builder.build();
    }

    private TAFBaseForecast createBaseForecast() {
        TAFBaseForecastImpl.Builder builder = TAFBaseForecastImpl.builder();
        builder.setPrevailingVisibility(NumericMeasureImpl.builder().setUom("m").setValue(9000d).build());

        SurfaceWindImpl.Builder wind = SurfaceWindImpl.builder();
        wind.setVariableDirection(false);
        wind.setMeanWindDirection(NumericMeasureImpl.builder().setUom("deg").setValue(130d).build());
        wind.setMeanWindSpeed(NumericMeasureImpl.builder().setUom("m/s").setValue(5d).build());
        builder.setSurfaceWind(wind.build());

        CloudLayerImpl.Builder cloud = CloudLayerImpl.builder();
        cloud.setAmount(AviationCodeListUser.CloudAmount.BKN);
        cloud.setBase(NumericMeasureImpl.builder().setUom("[ft_i]").setValue(2000d).build());
        builder.setCloud(CloudForecastImpl.builder().setLayers(Arrays.asList(cloud.build())).build());

        return builder.build();
    }

    private TAFChangeForecast createChangeForecast(PartialOrCompleteTimePeriod timePeriod, AviationCodeListUser.TAFChangeIndicator indicator,
            CloudForecast forecast, NumericMeasure prevailingVisibility, Integer visbilityOperator, SurfaceWind wind, List<Weather> weather) {
        TAFChangeForecastImpl.Builder builder = TAFChangeForecastImpl.builder();
        if (timePeriod != null) {
            builder.setPeriodOfChange(timePeriod);
        }
        if (indicator != null) {
            builder.setChangeIndicator(indicator);
        }
        if (forecast != null) {
            builder.setCloud(forecast);
        }
        if (prevailingVisibility != null) {
            builder.setPrevailingVisibility(prevailingVisibility);
        }
        if (wind != null) {
            builder.setSurfaceWind(wind);
        }
        if (visbilityOperator != null) {
            builder.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.fromInt(visbilityOperator));
        }

        if (weather != null && weather.size() > 0) {
            builder.setForecastWeather(weather);
        }

        return builder.build();
    }
}
