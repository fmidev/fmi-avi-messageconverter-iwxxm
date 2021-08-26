package fi.fmi.avi.converter.iwxxm.generic;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericWeatherMessageParserTest extends XMLTestCase implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private static Document readDocument(final Class<?> clz, final String name) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream inputStream = clz.getResourceAsStream(name)) {
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void specificationTest() {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO));
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO));
    }

    @Test
    public void tafStringMessageTest() throws Exception {
        String input = readResourceToString("taf.xml");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals("2017-07-30T11:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2017-07-30T12:00Z",
                message.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getStartTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));
        assertEquals("2017-07-31T12:00Z",
                message.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getEndTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EETN");

        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(input, message.getOriginalMessage());

    }

    @Test
    public void tafDOMMessageTest() throws Exception {
        String fileName = "taf.xml";
        Document input = readDocument(GenericWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "TAF");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals("2017-07-30T11:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EETN");
        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void sigmetDOMTest() throws Exception {
        String fileName = "sigmet.xml";
        Document input = readDocument(GenericWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "TropicalCycloneSIGMET");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.SIGMET.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals("2012-08-25T16:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUCC");
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUSO");
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");

        assertEquals(expectedIndiactors, message.getLocationIndicators());


        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    //TODO: add assertions
    @Test
    public void metar21DOMTest() throws Exception {
        String fileName = "iwxxm-21-metar.xml";
        Document input = readDocument(GenericWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "METAR");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.METAR.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());

        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals("2012-08-22T16:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndiactors, message.getLocationIndicators());


        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());

    }

    //TODO: add assertions
    @Test
    public void metar30DOMTest() throws Exception {
        String fileName = "iwxxm-30-metar.xml";
        Document input = readDocument(GenericWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "METAR");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.METAR.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());

        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals("2012-08-22T16:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndiactors, message.getLocationIndicators());


        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }
}
