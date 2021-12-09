package fi.fmi.avi.converter.iwxxm.generic;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.Format;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public final class GenericAviationWeatherMessageParserTest extends XMLTestCase implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertPartialOrCompletePeriod(final String start, final String end, final Optional<PartialOrCompleteTimePeriod> actualPeriod) {
        assertEquals("startTime", ZonedDateTime.parse(start), actualPeriod//
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null));
        assertEquals("endTime", ZonedDateTime.parse(end), actualPeriod//
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .orElse(null));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertPartialOrCompleteTime(final String expected, final Optional<PartialOrCompleteTimeInstant> actualTime) {
        assertEquals(ZonedDateTime.parse(expected), actualTime.flatMap(PartialOrCompleteTimeInstant::getCompleteTime).orElse(null));
    }

    private Document readDocumentFromResource(final String name) throws ParserConfigurationException, IOException, SAXException {
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            requireNonNull(inputStream, name);
            return readDocument(inputStream);
        }
    }

    private Document readDocumentFromString(final String xmlString) throws ParserConfigurationException, IOException, SAXException {
        try (InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            return readDocument(inputStream);
        }
    }

    private Document readDocument(final InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputStream);
    }

    @Test
    public void specificationTest() {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO));
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO));
    }

    @Test
    public void namespacesAreCopiedFromCollectToMessage() throws IOException, SAXException, ParserConfigurationException {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-collect.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-collect.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    private void testNamespaceDeclarations(final String bulletinResourceName, final String expectedResultResourceName)
            throws IOException, ParserConfigurationException, SAXException {
        final String input = readResourceToString(bulletinResourceName);

        final ConversionResult<GenericMeteorologicalBulletin> result = converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_STRING_TO_GENERIC_BULLETIN_POJO);
        assertEquals(Collections.emptyList(), result.getConversionIssues());

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        final List<String> messages = result.getConvertedMessage()//
                .map(bulletin -> bulletin.getMessages().stream()//
                        .map(GenericAviationWeatherMessage::getOriginalMessage)//
                        .collect(Collectors.toList()))//
                .orElse(Collections.emptyList());
        assertEquals(1, messages.size());

        final Document expectedMessage1 = readDocumentFromResource(expectedResultResourceName);
        final Document actualDocument = readDocumentFromString(messages.get(0));
        assertXMLIdentical(XMLUnit.compareXML(expectedMessage1, actualDocument), true);
    }

    @Test
    public void namespacesAreRetainedInMessage() throws IOException, SAXException, ParserConfigurationException {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-message.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-message.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void extraNamespacesAreRetainedInMessage() throws IOException, ParserConfigurationException, SAXException {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-extra.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-extra.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void namespacesAreMergedIfNeeded() throws IOException, SAXException, ParserConfigurationException {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-mixed.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-mixed.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void taf21StringMessageTest() throws IOException, SAXException {
        final String input = readResourceToString("taf.xml");

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);

        final GenericAviationWeatherMessage message = assertTAF21Message(result);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(input, message.getOriginalMessage());

    }

    @Test
    public void taf21DOMMessageTest() throws ParserConfigurationException, IOException, SAXException {
        final String fileName = "taf.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final GenericAviationWeatherMessage message = assertTAF21Message(result);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    private GenericAviationWeatherMessage assertTAF21Message(final ConversionResult<GenericAviationWeatherMessage> result) {
        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TAF, message.getMessageType().orElse(null));
        assertTrue(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2017-07-30T11:30Z", message.getIssueTime());
        assertPartialOrCompletePeriod("2017-07-30T12:00Z", "2017-07-31T12:00Z", message.getValidityTime());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.AERODROME, "EETN");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        return message;
    }

    @Test
    public void taf21CancelDOMTest() throws Exception {
        final String fileName = "iwxxm-21-taf-cancel.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TAF, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.AMENDMENT, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-16T15:00Z", message.getIssueTime());
        assertPartialOrCompletePeriod("2012-08-16T15:00Z", "2012-08-17T00:00Z", message.getValidityTime());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void taf21MissingDOMTest() throws Exception {
        final String fileName = "iwxxm-21-taf-missing.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TAF, message.getMessageType().orElse(null));
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-16T15:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.AERODROME, "YUDA");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void sigmetDOMTest() throws ParserConfigurationException, IOException, SAXException {
        assertSIGMET21Xml("sigmet.xml");
    }

    @Test
    public void sigmetWithEmptyBulletinIdTest() throws ParserConfigurationException, IOException, SAXException {
        assertSIGMET21Xml("sigmet-with-empty-translationid.xml");
    }

    private void assertSIGMET21Xml(final String fileName) throws ParserConfigurationException, IOException, SAXException {
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SIGMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SIGMET, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-25T16:00Z", message.getIssueTime());
        assertPartialOrCompletePeriod("2012-08-25T16:00:00Z", "2012-08-25T22:00:00Z", message.getValidityTime());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUSO");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void airmet21DOMTest() throws Exception {
        final String fileName = "iwxxm-21-airmet.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertTrue(result.getConvertedMessage().isPresent());
        assertEquals(result.getConversionIssues(), Collections.emptyList());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.AIRMET.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertFalse(message.getIssueTime().isPresent());
        assertPartialOrCompletePeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z", message.getValidityTime());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void airmet30MDOMTest() throws Exception {
        final String fileName = "iwxxm-30-airmet.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertTrue(result.getConvertedMessage().isPresent());
        assertEquals(result.getConversionIssues(), Collections.emptyList());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.AIRMET.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2014-05-15T15:20Z", message.getIssueTime());
        assertPartialOrCompletePeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z", message.getValidityTime());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void metar21DOMTest() throws ParserConfigurationException, IOException, SAXException {
        final String fileName = "iwxxm-21-metar.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.METAR;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.METAR, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-22T16:30Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());

    }

    @Test
    public void metar30DOMTest() throws Exception {
        final String fileName = "iwxxm-30-metar.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.METAR;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.METAR.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-22T16:30Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void speci21DOMMessageTest() throws Exception {
        final String fileName = "speci-A3-2-21.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPECI;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SPECI, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-15T11:15Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void speci30DOMMessageTest() throws Exception {
        final String fileName = "speci-A3-2-30.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPECI;

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SPECI, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2012-08-15T11:15Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void swxDOMMessageTest() throws Exception {
        final String fileName = "spacewx-A2-3.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPACE_WEATHER_ADVISORY;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SPACE_WEATHER_ADVISORY.toString(), message.getMessageType().map(MessageType::toString).orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.AMENDMENT, message.getReportStatus());
        assertPartialOrCompleteTime("2016-11-08T01:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.ISSUING_CENTRE, "DONLON");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void tca21DOMMessageTest() throws Exception {
        final String fileName = "tc-advisory-A2-2-21.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TROPICAL_CYCLONE_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TROPICAL_CYCLONE_ADVISORY, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2004-09-25T16:00:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.ISSUING_CENTRE, "YUFO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void tca30DOMMessageTest() throws Exception {
        final String fileName = "tc-advisory-A2-2-30.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TROPICAL_CYCLONE_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TROPICAL_CYCLONE_ADVISORY, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.CORRECTION, message.getReportStatus());
        assertPartialOrCompleteTime("2004-09-25T16:00:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.ISSUING_CENTRE, "YUFO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void vaa21DOMMessageTest() throws Exception {
        final String fileName = "va-advisory-A2-1-21.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.VOLCANIC_ASH_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.VOLCANIC_ASH_ADVISORY, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.NORMAL, message.getReportStatus());
        assertPartialOrCompleteTime("2008-09-23T01:30:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.ISSUING_CENTRE, "TOKYO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }

    @Test
    public void vaa30DOMMessageTest() throws Exception {
        final String fileName = "va-advisory-A2-1-30.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.VOLCANIC_ASH_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertEquals(result.getConversionIssues(), Collections.emptyList());
        assertTrue(result.getConvertedMessage().isPresent());
        final GenericAviationWeatherMessage message = result.getConvertedMessage().get();

        assertEquals(Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.VOLCANIC_ASH_ADVISORY, message.getMessageType().orElse(null));
        assertFalse(message.isTranslated());
        assertEquals(ReportStatus.CORRECTION, message.getReportStatus());
        assertPartialOrCompleteTime("2008-09-23T01:30:00Z", message.getIssueTime());
        assertFalse(message.getValidityTime().isPresent());
        final Map<LocationIndicatorType, String> expectedIndicators = Collections.singletonMap(LocationIndicatorType.ISSUING_CENTRE, "TOKYO");
        assertEquals(expectedIndicators, message.getLocationIndicators());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), message.getOriginalMessage());
    }
}
