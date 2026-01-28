package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.GenericAviationWeatherMessage.Format;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static fi.fmi.avi.converter.iwxxm.generic.GenericMessageAssertion.assertMessage;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericBulletinIWXXMParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner;

    @Test
    public void testScanner() throws Exception {
        final BulletinProperties properties = new BulletinProperties();
        iwxxmGenericBulletinScanner.collectBulletinProperties(readDocumentFromResource("taf/iwxxm-21-taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertThat(properties.contains(BulletinProperties.Name.HEADING)).isTrue();
        assertThat(properties.contains(BulletinProperties.Name.MESSAGE)).isTrue();
    }

    @Test
    public void testParserWithTAF() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-21-taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).isSuccessful();

        assertFirstMessage(result);
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml");
    }

    @Test
    public void testParserWithSIGMET() throws Exception {
        final Document input = readDocumentFromResource("sigmet/iwxxm-21-sigmet-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUSO");

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.SIGMET)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-25T16:00Z")
                .hasValidityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                .hasLocationIndicators(expectedIndicators);
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LYXX31YUDO251600_C_YUDO_201208251600--.xml");
    }

    @Test
    public void testParserWithSIGMET30() throws Exception {
        final Document input = readDocumentFromResource("sigmet/iwxxm-30-sigmet-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUSO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.SIGMET)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-10T12:00Z")
                .hasValidityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                .hasLocationIndicators(expectedIndicators);
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LYXX31YUDO251600_C_YUDO_201208251600--.xml");
    }

    @Test
    public void testParserWithTAF30() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-30-taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T18:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LYXX31YUDO251600_C_YUDO_201208251600--.xml");
    }

    @Test
    public void testParserWithTAF30Cancellation() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-30-taf-cancellation-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.AMENDMENT)
                .hasIssueTime("2012-08-16T15:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LYXX31YUDO251600_C_YUDO_201208251600--.xml");
    }

    @Test
    public void testParserWithAlternateCollectNamespacePrefix() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-30-taf-bulletin-namespaces-collect-alt-prefix.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertThat(bulletin.getHeading()).isNotNull();

        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T18:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");
        assertThat(result.getConvertedMessage().get().getHeading().getOriginalCollectIdentifier())
                .hasValue("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml");
    }

    @Test
    public void testParserWithUnknownMessageType() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-2025-2-unknown-message-type-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertThatConversionResult(result).hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Unknown message type");
    }

    @Test
    public void testParserWithNilTAF() throws Exception {
        final Document input = readDocumentFromResource("taf/iwxxm-2025-2-taf-bulletin-with-nil.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertThat(bulletin.getMessages()).hasSize(2);

        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T18:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO")
                .isNotNil();

        assertMessage(bulletin.getMessages().get(1))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T18:00Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "EFHA")
                .isNil();
    }

    @Test
    public void testParserWithNilMETAR() throws Exception {
        final Document input = readDocumentFromResource("metar/iwxxm-2025-2-metar-bulletin-with-nil.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final GenericMeteorologicalBulletin bulletin = assertThatConversionResult(result).isSuccessful().getMessage();
        assertThat(bulletin.getMessages()).hasSize(2);

        assertMessage(bulletin.getMessages().get(0))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.METAR)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-22T16:30Z")
                .hasObservationTime("2012-08-22T16:30Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO")
                .isNotNil();

        assertMessage(bulletin.getMessages().get(1))
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2025_2_NAMESPACE)
                .hasMessageType(MessageType.METAR)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-22T16:30Z")
                .hasObservationTime("2012-08-22T16:30Z")
                .hasNoValidityPeriod()
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "EFHA")
                .isNil();
    }
}
