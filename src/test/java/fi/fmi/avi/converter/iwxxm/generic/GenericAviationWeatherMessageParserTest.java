package fi.fmi.avi.converter.iwxxm.generic;

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
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.fmi.avi.converter.iwxxm.generic.GenericMessageAssertion.assertMessage;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public final class GenericAviationWeatherMessageParserTest extends XMLTestCase implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private Document readDocumentFromResource(final String name) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(name)) {
            requireNonNull(inputStream, name);
            return readDocument(inputStream);
        }
    }

    private Document readDocumentFromString(final String xmlString) throws Exception {
        try (final InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            return readDocument(inputStream);
        }
    }

    private Document readDocument(final InputStream inputStream) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputStream);
    }

    @Test
    public void specificationTest() {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO)).isTrue();
    }

    @Test
    public void namespacesAreCopiedFromCollectToMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-collect.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-collect.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    private void testNamespaceDeclarations(final String bulletinResourceName, final String expectedResultResourceName)
            throws Exception {
        final String input = readResourceToString(bulletinResourceName);

        final ConversionResult<GenericMeteorologicalBulletin> result = converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_STRING_TO_GENERIC_BULLETIN_POJO);
        assertThat(result.getConversionIssues()).isEmpty();

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        final List<String> messages = result.getConvertedMessage()//
                .map(bulletin -> bulletin.getMessages().stream()//
                        .map(GenericAviationWeatherMessage::getOriginalMessage)//
                        .collect(Collectors.toList()))//
                .orElse(Collections.emptyList());
        assertThat(messages).hasSize(1);

        final Document expectedMessage1 = readDocumentFromResource(expectedResultResourceName);
        final Document actualDocument = readDocumentFromString(messages.get(0));
        assertXMLIdentical(XMLUnit.compareXML(expectedMessage1, actualDocument), true);
    }

    @Test
    public void namespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-message.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-message.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void extraNamespacesAreRetainedInMessage() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-extra.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-extra.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void namespacesAreMergedIfNeeded() throws Exception {
        final String bulletinResourceName = "iwxxm-30-taf-bulletin-namespaces-mixed.xml";
        final String expectedResultResourceName = "iwxxm-30-taf-namespaces-mixed.xml";
        testNamespaceDeclarations(bulletinResourceName, expectedResultResourceName);
    }

    @Test
    public void taf21StringMessageTest() throws Exception {
        final String input = readResourceToString("iwxxm-21-taf.xml");

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .isTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2017-07-30T11:30Z")
                .hasValidityPeriod("2017-07-30T12:00Z", "2017-07-31T12:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "EETN");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(input, result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void taf21DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-21-taf.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .isTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2017-07-30T11:30Z")
                .hasValidityPeriod("2017-07-30T12:00Z", "2017-07-31T12:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "EETN");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void taf21CancelDOMTest() throws Exception {
        final String fileName = "iwxxm-21-taf-cancel.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.AMENDMENT)
                .hasIssueTime("2012-08-16T15:00Z")
                .hasValidityPeriod("2012-08-16T15:00Z", "2012-08-17T00:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void taf21MissingDOMTest() throws Exception {
        final String fileName = "iwxxm-21-taf-missing.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.TAF;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-16T15:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDA");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void sigmetDOMTest() throws Exception {
        assertSIGMET21Xml("iwxxm-21-sigmet.xml");
    }

    @Test
    public void sigmetWithEmptyBulletinIdTest() throws Exception {
        assertSIGMET21Xml("iwxxm-21-sigmet-with-empty-translationid.xml");
    }

    private void assertSIGMET21Xml(final String fileName) throws Exception {
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SIGMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUSO");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.SIGMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-25T16:00Z")
                .hasValidityPeriod("2012-08-25T16:00:00Z", "2012-08-25T22:00:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void airmet21DOMTest() throws Exception {
        final String fileName = "iwxxm-21-airmet.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.AIRMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2014-05-15T15:20Z")
                .hasValidityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void airmet30MDOMTest() throws Exception {
        final String fileName = "iwxxm-30-airmet.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.AIRMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2014-05-15T15:20Z")
                .hasValidityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void airmet20212MDOMTest() throws Exception {
        final String fileName = "iwxxm-2021-2-airmet-A6-1a-TS.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2021_2_NAMESPACE)
                .hasMessageType(MessageType.AIRMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2014-05-15T15:20Z")
                .hasValidityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void airmet20231MDOMTest() throws Exception {
        final String fileName = "iwxxm-2023-1-airmet-A6-1a-TS.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2023_1_NAMESPACE)
                .hasMessageType(MessageType.AIRMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2014-05-15T15:20Z")
                .hasValidityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void airmet20252MDOMTest() throws Exception {
        final String fileName = "iwxxm-2025-2-airmet-A6-1a-TS.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.AIRMET;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDD");

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.AIRMET)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2014-05-15T15:20Z")
                .hasValidityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                .hasLocationIndicators(expectedIndicators);

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void metar21DOMTest() throws Exception {
        final String fileName = "iwxxm-21-metar.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.METAR;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.METAR)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-22T16:30Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void metar30DOMTest() throws Exception {
        final String fileName = "iwxxm-30-metar.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.METAR;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.METAR)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-22T16:30Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void speci21DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-21-speci-A3-2.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPECI;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.SPECI)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T11:15Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void speci30DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-30-speci-A3-2.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPECI;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.SPECI)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T11:15Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void swx30DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-30-spacewx-A2-3.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPACE_WEATHER_ADVISORY;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.SPACE_WEATHER_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.AMENDMENT)
                .hasIssueTime("2016-11-08T01:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "DONLON");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void swx20252DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-2025-2-spacewx-A7-3.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = ConversionHints.SPACE_WEATHER_ADVISORY;
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.SPACE_WEATHER_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2020-11-08T01:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "DONLON");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void tca21DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-21-tc-advisory-A2-2.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TROPICAL_CYCLONE_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.TROPICAL_CYCLONE_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2004-09-25T16:00:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "YUFO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void tca30DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-30-tc-advisory-A2-2.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TROPICAL_CYCLONE_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TROPICAL_CYCLONE_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.CORRECTION)
                .hasIssueTime("2004-09-25T16:00:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "YUFO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void vaa21DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-21-va-advisory-A2-1.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.VOLCANIC_ASH_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.VOLCANIC_ASH_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2008-09-23T01:30:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "TOKYO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

    @Test
    public void vaa30DOMMessageTest() throws Exception {
        final String fileName = "iwxxm-30-va-advisory-A2-1.xml";
        final Document input = readDocumentFromResource(fileName);

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.VOLCANIC_ASH_ADVISORY);
        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, hints);

        assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.VOLCANIC_ASH_ADVISORY)
                .isNotTranslated()
                .hasReportStatus(ReportStatus.CORRECTION)
                .hasIssueTime("2008-09-23T01:30:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE, "TOKYO");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(fileName), result.getConvertedMessage().get().getOriginalMessage());
    }

}


