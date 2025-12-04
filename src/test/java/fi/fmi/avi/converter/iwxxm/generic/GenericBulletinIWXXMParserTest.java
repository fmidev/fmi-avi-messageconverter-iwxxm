package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.IWXXM_2_1_NAMESPACE;
import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.IWXXM_3_0_NAMESPACE;
import static fi.fmi.avi.converter.iwxxm.generic.GenericMessageAssertion.assertFirstMessage;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericBulletinIWXXMParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner;

    private Document getBulletinDocument(final String filename) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (final InputStream inputStream = GenericBulletinIWXXMParserTest.class.getResourceAsStream(filename)) {
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testScanner() throws Exception {
        final BulletinProperties properties = new BulletinProperties();
        iwxxmGenericBulletinScanner.collectBulletinProperties(this.getBulletinDocument("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
        assertThat(properties.contains(BulletinProperties.Name.HEADING)).isTrue();
        assertThat(properties.contains(BulletinProperties.Name.MESSAGE)).isTrue();
    }

    @Test
    public void testParserWithTAF() throws Exception {
        final Document input = this.getBulletinDocument("taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertThat(result.getConversionIssues()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
    }

    @Test
    public void testParserWithSIGMET() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUSO");

        assertFirstMessage(result)
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_2_1_NAMESPACE)
                .hasMessageType(MessageType.SIGMET)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-25T16:00Z")
                .hasValidityPeriod("2012-08-25T16:00Z", "2012-08-25T22:00Z")
                .hasLocationIndicators(expectedIndicators);
    }

    @Test
    public void testParserWithSIGMET30() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        final Map<LocationIndicatorType, String> expectedIndicators = new HashMap<>();
        expectedIndicators.put(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUSO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDO");
        expectedIndicators.put(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");

        assertFirstMessage(result)
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.SIGMET)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-10T12:00Z")
                .hasValidityPeriod("2012-08-10T12:00Z", "2012-08-10T16:00Z")
                .hasLocationIndicators(expectedIndicators);
    }

    @Test
    public void testParserWithTAF30() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        assertFirstMessage(result)
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.NORMAL)
                .hasIssueTime("2012-08-15T18:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");
    }

    @Test
    public void testParserWithTAF30Cancellation() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-cancellation-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);

        assertFirstMessage(result)
                .hasFormat(Format.IWXXM)
                .hasNamespace(IWXXM_3_0_NAMESPACE)
                .hasMessageType(MessageType.TAF)
                .hasReportStatus(ReportStatus.AMENDMENT)
                .hasIssueTime("2012-08-16T15:00Z")
                .hasValidityPeriod("2012-08-16T00:00Z", "2012-08-16T18:00Z")
                .hasLocationIndicator(LocationIndicatorType.AERODROME, "YUDO");
    }
}
