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
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static fi.fmi.avi.converter.iwxxm.generic.GenericMessageAssertion.assertMessage;
import static java.util.Objects.requireNonNull;

@RunWith(Parameterized.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericAviationWeatherMessageDOMParserTest extends XMLTestCase implements IWXXMConverterTests {

    private final GenericAviationWeatherMessageParserDOMTestCase testCase;

    @Autowired
    private AviMessageConverter converter;

    public GenericAviationWeatherMessageDOMParserTest(final GenericAviationWeatherMessageParserDOMTestCase testCase) throws Exception {
        this.testCase = testCase;
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameters(name = "{0}")
    public static Collection<GenericAviationWeatherMessageParserDOMTestCase> testCases() {
        return Arrays.asList(
                // TAF tests
                taf().fileName("iwxxm-21-taf.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .translated()
                        .issueTime("2017-07-30T11:30Z")
                        .validityPeriod("2017-07-30T12:00Z", "2017-07-31T12:00Z")
                        .aerodrome("EETN")
                        .build(),

                taf().fileName("iwxxm-21-taf-cancel.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .reportStatus(ReportStatus.AMENDMENT)
                        .issueTime("2012-08-16T15:00Z")
                        .validityPeriod("2012-08-16T15:00Z", "2012-08-17T00:00Z")
                        .aerodrome("YUDO")
                        .build(),

                taf().fileName("iwxxm-21-taf-missing.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-16T15:00Z")
                        .noValidityPeriod()
                        .aerodrome("YUDA")
                        .build(),

                // SIGMET tests
                sigmet().fileName("iwxxm-21-sigmet.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00:00Z", "2012-08-25T22:00:00Z")
                        .sigmetLocationIndicators("YUCC", "YUSO", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-21-sigmet-with-empty-translationid.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00:00Z", "2012-08-25T22:00:00Z")
                        .sigmetLocationIndicators("YUCC", "YUSO", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-30-sigmet-A6-1a-TS.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-10T12:00Z")
                        .validityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUSO")
                        .build(),

                // AIRMET tests
                airmet().fileName("iwxxm-21-airmet.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2014-05-15T15:20Z")
                        .validityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                        .sigmetLocationIndicators("YUDD", "YUCC", "YUDD")
                        .build(),

                airmet().fileName("iwxxm-30-airmet.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2014-05-15T15:20Z")
                        .validityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                        .sigmetLocationIndicators("YUDD", "YUCC", "YUDD")
                        .build(),

                airmet().fileName("iwxxm-2021-2-airmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2014-05-15T15:20Z")
                        .validityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                        .sigmetLocationIndicators("YUDD", "YUCC", "YUDD")
                        .build(),

                airmet().fileName("iwxxm-2023-1-airmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2014-05-15T15:20Z")
                        .validityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUDD")
                        .build(),

                airmet().fileName("iwxxm-2025-2-airmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2014-05-15T15:20Z")
                        .validityPeriod("2014-05-15T15:20Z", "2014-05-15T18:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUDD")
                        .build(),

                // METAR tests
                metar().fileName("iwxxm-21-metar.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-30-metar.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                // SPECI tests
                speci().fileName("iwxxm-21-speci-A3-2.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                speci().fileName("iwxxm-30-speci-A3-2.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                // Space Weather Advisory tests
                swx().fileName("iwxxm-30-spacewx-A2-3.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .reportStatus(ReportStatus.AMENDMENT)
                        .issueTime("2016-11-08T01:00Z")
                        .noValidityPeriod()
                        .issuingCentre("DONLON")
                        .build(),

                swx().fileName("iwxxm-2025-2-spacewx-A7-3.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2020-11-08T01:00Z")
                        .noValidityPeriod()
                        .issuingCentre("DONLON")
                        .build(),

                // Tropical Cyclone Advisory tests
                tca().fileName("iwxxm-21-tc-advisory-A2-2.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2004-09-25T16:00:00Z")
                        .noValidityPeriod()
                        .issuingCentre("YUFO")
                        .build(),

                tca().fileName("iwxxm-30-tc-advisory-A2-2.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .reportStatus(ReportStatus.CORRECTION)
                        .issueTime("2004-09-25T16:00:00Z")
                        .noValidityPeriod()
                        .issuingCentre("YUFO")
                        .build(),

                // Volcanic Ash Advisory tests
                vaa().fileName("iwxxm-21-va-advisory-A2-1.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2008-09-23T01:30:00Z")
                        .noValidityPeriod()
                        .issuingCentre("TOKYO")
                        .build(),

                vaa().fileName("iwxxm-30-va-advisory-A2-1.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .reportStatus(ReportStatus.CORRECTION)
                        .issueTime("2008-09-23T01:30:00Z")
                        .noValidityPeriod()
                        .issuingCentre("TOKYO")
                        .build()
        );
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder taf() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.TAF)
                .hints(ConversionHints.TAF);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder sigmet() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.SIGMET)
                .hints(ConversionHints.SIGMET);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder airmet() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.AIRMET)
                .hints(ConversionHints.AIRMET);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder metar() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.METAR)
                .hints(ConversionHints.METAR);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder speci() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.SPECI)
                .hints(ConversionHints.SPECI);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder swx() {
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.SPACE_WEATHER_ADVISORY)
                .hints(ConversionHints.SPACE_WEATHER_ADVISORY);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder tca() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TROPICAL_CYCLONE_ADVISORY);
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.TROPICAL_CYCLONE_ADVISORY)
                .hints(hints);
    }

    private static GenericAviationWeatherMessageParserDOMTestCase.Builder vaa() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.VOLCANIC_ASH_ADVISORY);
        return GenericAviationWeatherMessageParserDOMTestCase.builder()
                .messageType(MessageType.VOLCANIC_ASH_ADVISORY)
                .hints(hints);
    }

    private Document readDocumentFromResource(final String name) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(name)) {
            requireNonNull(inputStream, name);
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testDOMParsing() throws Exception {
        final Document input = readDocumentFromResource(testCase.getFileName());

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, testCase.getHints());

        final GenericMessageAssertion assertion = assertMessage(result)
                .hasNoIssues()
                .isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(testCase.getNamespace())
                .hasMessageType(testCase.getMessageType())
                .hasReportStatus(testCase.getReportStatus())
                .hasIssueTime(testCase.getIssueTime());

        if (testCase.isTranslated()) {
            assertion.isTranslated();
        } else {
            assertion.isNotTranslated();
        }

        if (testCase.hasValidityPeriod()) {
            assertion.hasValidityPeriod(testCase.getValidityStart(), testCase.getValidityEnd());
        } else {
            assertion.hasNoValidityPeriod();
        }

        if (!testCase.getLocationIndicators().isEmpty()) {
            if (testCase.getLocationIndicators().size() == 1
                    && testCase.getLocationIndicators().containsKey(LocationIndicatorType.AERODROME)) {
                assertion.hasLocationIndicator(LocationIndicatorType.AERODROME,
                        testCase.getLocationIndicators().get(LocationIndicatorType.AERODROME));
            } else if (testCase.getLocationIndicators().size() == 1
                    && testCase.getLocationIndicators().containsKey(LocationIndicatorType.ISSUING_CENTRE)) {
                assertion.hasLocationIndicator(LocationIndicatorType.ISSUING_CENTRE,
                        testCase.getLocationIndicators().get(LocationIndicatorType.ISSUING_CENTRE));
            } else {
                assertion.hasLocationIndicators(testCase.getLocationIndicators());
            }
        }

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(testCase.getFileName()),
                result.getConvertedMessage().get().getOriginalMessage());
    }
}

