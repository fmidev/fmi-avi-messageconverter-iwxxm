package fi.fmi.avi.converter.iwxxm.generic;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
public class GenericAviationWeatherMessageParserTest extends XMLTestCase implements IWXXMConverterTests {

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
        final String input = readResourceToString("taf.xml");

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertTrue(message.isTranslated());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals("2017-07-30T11:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2017-07-30T12:00Z", message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));
        assertEquals("2017-07-31T12:00Z", message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(

                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EETN");

        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(input, message.getOriginalMessage());

    }

    @Test
    public void tafDOMMessageTest() throws Exception {
        final String fileName = "taf.xml";
        final Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "TAF");

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertTrue(message.isTranslated());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals("2017-07-30T11:30Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EETN");
        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void sigmetDOMTest() throws Exception {
        assertSigmetXml("sigmet.xml");
    }

    @Test
    public void sigmetWithEmptyBulletinIdTest() throws Exception {
        assertSigmetXml("sigmet-with-empty-translationid.xml");
    }

    private void assertSigmetXml(final String fileName) throws Exception {
        final Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "TropicalCycloneSIGMET");

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.SIGMET.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals("2012-08-25T16:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUCC");
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUSO");
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");

        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void metar21DOMTest() throws Exception {
        String fileName = "iwxxm-21-metar.xml";
        Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

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

    @Test
    public void metar30DOMTest() throws Exception {
        String fileName = "iwxxm-30-metar.xml";
        Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

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

    @Test
    public void taf21CancelDOMTest() throws Exception {
        String fileName = "iwxxm-21-taf-cancel.xml";
        Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "METAR");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertTrue(result.getConvertedMessage().isPresent());
        assertTrue(result.getConversionIssues().isEmpty());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, message.getReportStatus());

        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals("2012-08-16T15:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2012-08-16T15:00Z", message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));
        assertEquals("2012-08-17T00:00Z", message.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void taf21MissingDOMTest() throws Exception {
        String fileName = "iwxxm-21-taf-missing.xml";
        Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "METAR");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertTrue(result.getConvertedMessage().isPresent());
        assertTrue(result.getConversionIssues().isEmpty());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.TAF.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());

        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals("2012-08-16T15:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = new HashMap<>();
        expectedIndiactors.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDA");
        assertEquals(expectedIndiactors, message.getLocationIndicators());
        assertFalse(message.getValidityTime().isPresent());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void swxDOMMessageTest() throws Exception {
        String fileName = "spacewx-A2-3.xml";
        Document input = readDocument(GenericAviationWeatherMessageParserTest.class, fileName);

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, "SPACE_WEATHER_ADVISORY");

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(MessageType.SPACE_WEATHER_ADVISORY.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertEquals(false, message.isTranslated());
        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, message.getReportStatus());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals("2016-11-08T01:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE, "DONLON");
        assertEquals(expectedIndiactors, message.getLocationIndicators());

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }
}
