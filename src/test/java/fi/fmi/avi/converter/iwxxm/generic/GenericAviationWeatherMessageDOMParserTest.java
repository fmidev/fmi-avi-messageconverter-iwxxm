package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage.Format;
import fi.fmi.avi.model.MessageType;
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
import static fi.fmi.avi.model.MessageType.*;
import static java.util.Objects.requireNonNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

@RunWith(Parameterized.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericAviationWeatherMessageDOMParserTest implements IWXXMConverterTests {

    private final GenericAviationWeatherMessageDOMParserTestCase testCase;

    @Autowired
    private AviMessageConverter converter;

    public GenericAviationWeatherMessageDOMParserTest(final GenericAviationWeatherMessageDOMParserTestCase testCase) throws Exception {
        this.testCase = testCase;
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameters(name = "{0}")
    public static Collection<GenericAviationWeatherMessageDOMParserTestCase> testCases() {
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

                taf().fileName("iwxxm-30-taf-A5-1.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .build(),

                taf().fileName("iwxxm-2021-2-taf-A5-1.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .build(),

                taf().fileName("iwxxm-2023-1-taf-A5-1.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .build(),

                taf().fileName("iwxxm-2025-2-taf-A5-1.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .build(),

                // TAF error cases
                taf().fileName("iwxxm-2025-2-taf-invalid-issue-time.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "issue time")
                        .build(),

                taf().fileName("iwxxm-2025-2-taf-invalid-validity-time.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "valid time")
                        .build(),

                taf().fileName("iwxxm-2025-2-taf-missing-aerodrome.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "location indicator")
                        .build(),

                taf().fileName("iwxxm-2025-2-taf-namespaces-alt-prefix.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-15T18:00Z")
                        .validityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                        .aerodrome("YUDO")
                        .build(),

                taf().fileName("iwxxm-2025-2-taf-A5-2.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-16T15:00:00Z")
                        .validityPeriod("2012-08-16T00:00:00Z", "2012-08-16T18:00:00Z")
                        .aerodrome("YUDO")
                        .reportStatus(ReportStatus.AMENDMENT)
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

                sigmet().fileName("iwxxm-30-sigmet-A6-2-TC.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                        .sigmetLocationIndicators("YUCC", "YUCC", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-2021-2-sigmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2012-08-10T12:00Z")
                        .validityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUSO")
                        .build(),

                sigmet().fileName("iwxxm-2021-2-sigmet-A6-2-TC.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                        .sigmetLocationIndicators("YUCC", "YUCC", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-2023-1-sigmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2012-08-10T12:00Z")
                        .validityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUSO")
                        .build(),

                sigmet().fileName("iwxxm-2023-1-sigmet-A6-2-TC.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                        .sigmetLocationIndicators("YUCC", "YUCC", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-2025-2-sigmet-A6-1a-TS.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-10T12:00Z")
                        .validityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                        .sigmetLocationIndicators("YUDD", "YUDD", "YUSO")
                        .build(),

                sigmet().fileName("iwxxm-2025-2-sigmet-A6-2-TC.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-25T16:00Z")
                        .validityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                        .sigmetLocationIndicators("YUCC", "YUCC", "YUDO")
                        .build(),

                sigmet().fileName("iwxxm-2025-2-sigmet-VA-EGGX.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2018-07-25T16:00:00Z")
                        .validityPeriod("2018-07-25T16:00Z", "2018-07-25T22:00Z")
                        .sigmetLocationIndicators("EGGX", "EGGX", "EGRR")
                        .build(),

                // SIGMET error cases
                sigmet().fileName("iwxxm-2025-2-sigmet-missing-mwo.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-10T12:00Z")
                        .validityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "ORIGINATING_METEOROLOGICAL_WATCH_OFFICE")
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
                        .observationTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-30-metar-A3-1.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .observationTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-2021-2-metar-A3-1.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .observationTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-2023-1-metar-A3-1.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .observationTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-2025-2-metar-A3-1.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .observationTime("2012-08-22T16:25Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                metar().fileName("iwxxm-2025-2-metar-invalid-observation-time.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-22T16:30Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "observation time")
                        .build(),

                // SPECI tests
                speci().fileName("iwxxm-21-speci-A3-2.xml")
                        .namespace(IWXXM_2_1_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .observationTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                speci().fileName("iwxxm-30-speci-A3-2.xml")
                        .namespace(IWXXM_3_0_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .observationTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                speci().fileName("iwxxm-2021-2-speci-A3-2.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .observationTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                speci().fileName("iwxxm-2023-1-speci-A3-2.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .observationTime("2012-08-15T11:15Z")
                        .noValidityPeriod()
                        .aerodrome("YUDO")
                        .build(),

                speci().fileName("iwxxm-2025-2-speci-A3-2.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2012-08-15T11:15Z")
                        .observationTime("2012-08-15T11:10Z")
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

                swx().fileName("iwxxm-2021-2-spacewx-A2-3.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2016-11-08T01:00Z")
                        .noValidityPeriod()
                        .issuingCentre("DONLON")
                        .build(),

                swx().fileName("iwxxm-2023-1-spacewx-A2-3.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
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

                // Space Weather Advisory error cases
                swx().fileName("iwxxm-2025-2-spacewx-missing-issuing-centre.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2016-11-08T01:00Z")
                        .noValidityPeriod()
                        .expectedIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "ISSUING_CENTRE")
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

                tca().fileName("iwxxm-2021-2-tc-advisory-A2-2.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2004-09-25T16:00:00Z")
                        .noValidityPeriod()
                        .issuingCentre("YUFO")
                        .build(),

                tca().fileName("iwxxm-2023-1-tc-advisory-A2-2.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2004-09-25T16:00:00Z")
                        .noValidityPeriod()
                        .issuingCentre("YUFO")
                        .build(),

                tca().fileName("iwxxm-2025-2-tc-advisory-A2-2.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2004-09-25T19:00:00Z")
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
                        .build(),

                vaa().fileName("iwxxm-2021-2-va-advisory-A2-1.xml")
                        .namespace(IWXXM_2021_2_NAMESPACE)
                        .issueTime("2008-09-23T01:30:00Z")
                        .noValidityPeriod()
                        .issuingCentre("TOKYO")
                        .build(),

                vaa().fileName("iwxxm-2023-1-va-advisory-A2-1.xml")
                        .namespace(IWXXM_2023_1_NAMESPACE)
                        .issueTime("2008-09-23T01:30:00Z")
                        .noValidityPeriod()
                        .issuingCentre("TOKYO")
                        .build(),

                vaa().fileName("iwxxm-2025-2-va-advisory-A7-2.xml")
                        .namespace(IWXXM_2025_2_NAMESPACE)
                        .issueTime("2024-09-23T01:30:00Z")
                        .noValidityPeriod()
                        .issuingCentre("TOKYO")
                        .build()
        );
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder taf() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(MessageType.TAF)
                .hints(ConversionHints.TAF);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder sigmet() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(MessageType.SIGMET)
                .hints(ConversionHints.SIGMET);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder airmet() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(MessageType.AIRMET)
                .hints(ConversionHints.AIRMET);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder metar() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(MessageType.METAR)
                .hints(ConversionHints.METAR);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder speci() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(MessageType.SPECI)
                .hints(ConversionHints.SPECI);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder swx() {
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(SPACE_WEATHER_ADVISORY)
                .hints(ConversionHints.SPACE_WEATHER_ADVISORY);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder tca() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, TROPICAL_CYCLONE_ADVISORY);
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(TROPICAL_CYCLONE_ADVISORY)
                .hints(hints);
    }

    private static GenericAviationWeatherMessageDOMParserTestCase.Builder vaa() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, VOLCANIC_ASH_ADVISORY);
        return GenericAviationWeatherMessageDOMParserTestCase.builder()
                .messageType(VOLCANIC_ASH_ADVISORY)
                .hints(hints);
    }

    private String getResourcePath(final String fileName, final MessageType messageType) {
        return messageType.name().toLowerCase() + "/" + fileName;
    }

    private Document readDocumentFromResource(final String name, final MessageType messageType) throws Exception {
        final String resourcePath = getResourcePath(name, messageType);
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            requireNonNull(inputStream, resourcePath);
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testDOMParsing() throws Exception {
        final Document input = readDocumentFromResource(testCase.getFileName(), testCase.getMessageType());

        final ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(input,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, testCase.getHints());

        final GenericMessageAssertion assertion = assertMessage(result);

        if (testCase.expectsIssues()) {
            assertion.hasIssues();
            for (final GenericAviationWeatherMessageDOMParserTestCase.ExpectedIssue expectedIssue : testCase.getExpectedIssues()) {
                if (expectedIssue.hasMessageSubstring()) {
                    assertion.hasIssue(expectedIssue.getSeverity(), expectedIssue.getType(), expectedIssue.getMessageSubstring());
                } else {
                    assertion.hasIssue(expectedIssue.getSeverity(), expectedIssue.getType());
                }
            }
        } else {
            assertion.hasNoIssues();
        }

        assertion.isPresent()
                .hasFormat(Format.IWXXM)
                .hasNamespace(testCase.getNamespace())
                .hasMessageType(testCase.getMessageType());

        if (testCase.hasReportStatus()) {
            assertion.hasReportStatus(testCase.getReportStatus());
        }

        if (testCase.hasIssueTime()) {
            assertion.hasIssueTime(testCase.getIssueTime());
        }

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

        if (testCase.hasObservationTime()) {
            assertion.hasObservationTime(testCase.getObservationTime());
        } else {
            assertion.hasNoObservationTime();
        }

        if (!testCase.getLocationIndicators().isEmpty()) {
            assertion.hasLocationIndicators(testCase.getLocationIndicators());
        }

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(readResourceToString(getResourcePath(testCase.getFileName(), testCase.getMessageType())),
                result.getConvertedMessage().get().getOriginalMessage());
    }
}

