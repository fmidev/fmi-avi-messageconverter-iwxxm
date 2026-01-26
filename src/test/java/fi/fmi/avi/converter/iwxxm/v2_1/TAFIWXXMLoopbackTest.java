package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.taf.TAF;
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

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.junit.Assert.*;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMLoopbackTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testTAFParsingAndSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO));
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));

        final Document toValidate = readDocumentFromResource("taf-A5-1.xml");
        final ConversionResult<TAF> result = converter.convertMessage(toValidate, IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, ConversionHints.EMPTY);
        final TAF taf = assertConversionResult(result).isSuccessful();

        final ConversionResult<Document> result2 = converter.convertMessage(taf, IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM);
        final Document outputDoc = assertConversionResult(result2).isSuccessful();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        final Element output = outputDoc.getDocumentElement();
        final Element input = toValidate.getDocumentElement();
        assertNotNull(output);

        XPathExpression expr = xpath.compile("/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/gml:timePosition");
        assertEquals("issueTime does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:beginPosition");
        assertEquals("validTime begin position does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/gml:endPosition");
        assertEquals("validTime end position does not match", expr.evaluate(output), expr.evaluate(input));

        //Base forecast:

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Base forecast type does not match", expr.evaluate(output), expr.evaluate(input));

        //Temporals:

        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:phenomenonTime[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id)]) = 1");
        assertEquals("Base forecast phenomenonTime does not refer to msg validTime", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:resultTime[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id)]) = 1");
        assertEquals("Base forecast resultTime does not refer to msg issueTime", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:validTime[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id)]) = 1");
        assertEquals("Base forecast validTime does not refer to msg validTime", expr.evaluate(output), expr.evaluate(input));

        //Procedure:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/gml:description");
        assertEquals("Process description does not match", expr.evaluate(output), expr.evaluate(input));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Observed properties does not match", expr.evaluate(output), expr.evaluate(input));

        //Aerodrome FOI (samplingFeature):

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:designator");
        assertEquals("Airport designator does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:name");
        assertEquals("Airport name does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:locationIndicatorICAO");
        assertEquals("Airport ICAO code does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation");
        assertEquals("Airport elevation value does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm"
                + ":AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm:fieldElevation/@uom");
        assertEquals("Airport elevation unit does not match", expr.evaluate(output), expr.evaluate(input));

        //Sampling point:

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/@srsName");
        assertEquals("Sampling point srsName does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature/sams:shape/gml:Point/gml:pos");
        assertEquals("Sampling point position value does not match", expr.evaluate(output), expr.evaluate(input));

        //Forecast properties:

        //CAVOK:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertEquals("CAVOK does not match", expr.evaluate(output), expr.evaluate(input));

        //Wind:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertEquals("Variable wind not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertEquals("Mean wind direction uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertEquals("Mean wind direction does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)), 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertEquals("Mean wind speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertEquals("Mean wind speed does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)), 0.00001);
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertEquals("Wind gust speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertEquals("Visibility does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)), 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Visibility uom does not match", expr.evaluate(output), expr.evaluate(input));

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Cloud layer count does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Cloud layer 1 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Cloud layer 1 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertEquals("Cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)), 0.00001);

        //Change forecast 1:

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 1 type does not match", expr.evaluate(output), expr.evaluate(input));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");
        assertEquals("Change forecast 1 phenomenonTime begin pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");
        assertEquals("Change forecast 1 phenomenonTime end pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:resultTime[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id)]) = 1");
        assertEquals("Change forecast 1 resultTime does not refer to msg issueTime", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:validTime[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id)]) = 1");
        assertEquals("Change forecast 1 validTime does not refer to msg validTime", expr.evaluate(output), expr.evaluate(input));

        //Procedure:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:procedure[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/@gml:id)]) = 1");
        assertEquals("Change forecast 1 Procedure does not refer to base forecast procedure", expr.evaluate(output), expr.evaluate(input));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 1 Observed properties does not match", expr.evaluate(output), expr.evaluate(input));

        //FOI reference:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:featureOfInterest[@xlink:href = concat('#', /iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/@gml:id)]) = 1");
        assertEquals("Change forecast 1 FOI reference does not point to base forecast FOI", expr.evaluate(output), expr.evaluate(input));

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 1 change indicator does not match", expr.evaluate(output), expr.evaluate(input));

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@cloudAndVisibilityOK");
        assertEquals("Change forecast 1 CAVOK does not match", expr.evaluate(output), expr.evaluate(input));

        //Forecast properties:

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Change Forecast 1 cloud layer count does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 1 cloud layer 1 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 1 cloud layer 1 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm:AerodromeCloudForecast/iwxxm:layer[1]/iwxxm"
                        + ":CloudLayer/iwxxm:base");
        assertEquals("Change Forecast 1 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertEquals("Change Forecast 1 cloud layer 1 type does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 1 cloud layer 2 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 1 cloud layer 2 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[1]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertEquals("Change Forecast 1 cloud layer 2 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        //Change forecast 2:

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 2 type does not match", expr.evaluate(output), expr.evaluate(input));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");

        assertEquals("Change forecast 2 phenomenonTime begin pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");

        assertEquals("Change forecast 2 phenomenonTime end pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:resultTime[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id)]) = 1");
        assertEquals("Change forecast 2 resultTime does not refer to msg issueTime", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:validTime[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id)]) = 1");
        assertEquals("Change forecast 2 validTime does not refer to msg validTime", expr.evaluate(output), expr.evaluate(input));

        //Procedure:
        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:procedure[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/@gml:id)]) = 1");
        assertEquals("Change forecast 1 Procedure does not refer to base forecast procedure", expr.evaluate(output), expr.evaluate(input));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 2 Observed properties does not match", expr.evaluate(output), expr.evaluate(input));

        //FOI reference:
        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:featureOfInterest[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/@gml:id)]) = 1");
        assertEquals("Change forecast 2 FOI reference does not point to base forecast FOI", expr.evaluate(output), expr.evaluate(input));

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 2 change indicator does not match", expr.evaluate(output), expr.evaluate(input));

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord" + "/@cloudAndVisibilityOK");
        assertEquals("Change forecast 2 CAVOK does not match", expr.evaluate(output), expr.evaluate(input));

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertEquals("Change forecast 2 Visibility does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Change forecast 2 visibility uom does not match", expr.evaluate(output), expr.evaluate(input));

        //Wind:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/@variableWindDirection");
        assertEquals("Change forecast 2 variable wind not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection/@uom");
        assertEquals("Change forecast 2 mean wind direction uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindDirection");
        assertEquals("Change forecast 2 mean wind direction does not match", Double.parseDouble(expr.evaluate(output)),
                Double.parseDouble(expr.evaluate(input)), 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed/@uom");
        assertEquals("Change forecast 2 mean wind speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:meanWindSpeed");
        assertEquals("Change forecast 2 mean wind speed does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm:AerodromeSurfaceWindForecast"
                        + "/iwxxm:windGustSpeed/@uom");
        assertEquals("Change forecast 2 wind gust speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        //Weather:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:weather[1]" + "/@xlink:href");
        assertEquals("Change forecast 2 weather 1 does not match", expr.evaluate(output), expr.evaluate(input));

        //Clouds:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Change Forecast 2 cloud layer count does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 1 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 2 cloud layer 1 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertEquals("Change Forecast 2 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:cloudType/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 1 type does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 2 cloud layer 2 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 2 cloud layer 2 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[2]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertEquals("Change Forecast 2 cloud layer 2 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        //Change forecast 3:

        //Type:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:type/@xlink:href");
        assertEquals("Change forecast 3 type does not match", expr.evaluate(output), expr.evaluate(input));

        //Temporals:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition");

        assertEquals("Change forecast 3 phenomenonTime begin pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition");

        assertEquals("Change forecast 3 phenomenonTime end pos does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:resultTime[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:issueTime/gml:TimeInstant/@gml:id)]) = 1");
        assertEquals("Change forecast 3 resultTime does not refer to msg issueTime", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[2]/om:OM_Observation/om:validTime[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:validTime/gml:TimePeriod/@gml:id)]) = 1");
        assertEquals("Change forecast 3 validTime does not refer to msg validTime", expr.evaluate(output), expr.evaluate(input));

        //Procedure:
        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:procedure[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:procedure/metce:Process/@gml:id)]) = 1");
        assertEquals("Change forecast 3 Procedure does not refer to base forecast procedure", expr.evaluate(output), expr.evaluate(input));

        //Observed property:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:observedProperty/@xlink:href");
        assertEquals("Change forecast 3 Observed properties does not match", expr.evaluate(output), expr.evaluate(input));

        //FOI reference:
        expr = xpath.compile("count(/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:featureOfInterest[@xlink:href = concat('#', "
                + "/iwxxm:TAF/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/@gml:id)]) = 1");
        assertEquals("Change forecast 3 FOI reference does not point to base forecast FOI", expr.evaluate(output), expr.evaluate(input));

        //Change indicator:
        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/@changeIndicator");
        assertEquals("Change forecast 3 change indicator does not match", expr.evaluate(output), expr.evaluate(input));

        //CAVOK:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord" + "/@cloudAndVisibilityOK");
        assertEquals("Change forecast 3 CAVOK does not match", expr.evaluate(output), expr.evaluate(input));

        //Forecast properties:

        //Visibility:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility");
        assertEquals("Change forecast 3 Visibility does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:prevailingVisibility/@uom");
        assertEquals("Change forecast 3 visibility uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm"
                + ":prevailingVisibilityOperator");
        assertEquals("Change forecast 3 visibility operator does not match", expr.evaluate(output), expr.evaluate(input));

        //Wind:
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/@variableWindDirection");
        assertEquals("Change forecast 3 variable wind not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/iwxxm:meanWindDirection/@uom");
        assertEquals("Change forecast 3 mean wind direction uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/iwxxm:meanWindDirection");
        assertEquals("Change forecast 3 mean wind direction does not match", Double.parseDouble(expr.evaluate(output)),
                Double.parseDouble(expr.evaluate(input)), 0.00001);

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/iwxxm:meanWindSpeed/@uom");
        assertEquals("Change forecast 3 mean wind speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/iwxxm:meanWindSpeed");
        assertEquals("Change forecast 3 mean wind speed does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);
        expr = xpath.compile(
                "/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:surfaceWind/iwxxm"
                        + ":AerodromeSurfaceWindForecast" + "/iwxxm:windGustSpeed/@uom");
        assertEquals("Change forecast 3 wind gust speed uom does not match", expr.evaluate(output), expr.evaluate(input));

        //Cloud:
        expr = xpath.compile(
                "count(/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                        + ":AerodromeCloudForecast/iwxxm:layer)");
        assertEquals("Change Forecast 3 cloud layer count does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:amount/@xlink:href");
        assertEquals("Change Forecast 3 cloud layer 1 amount does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base/@uom");
        assertEquals("Change Forecast 3 cloud layer 1 base uom does not match", expr.evaluate(output), expr.evaluate(input));

        expr = xpath.compile("/iwxxm:TAF/iwxxm:changeForecast[3]/om:OM_Observation/om:result/iwxxm:MeteorologicalAerodromeForecastRecord/iwxxm:cloud/iwxxm"
                + ":AerodromeCloudForecast/iwxxm:layer[1]/iwxxm" + ":CloudLayer/iwxxm:base");
        assertEquals("Change Forecast 3 cloud layer 1 base does not match", Double.parseDouble(expr.evaluate(output)), Double.parseDouble(expr.evaluate(input)),
                0.00001);

    }
}