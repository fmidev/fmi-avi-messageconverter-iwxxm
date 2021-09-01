package fi.fmi.avi.converter.iwxxm.generic;

import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.IWXXM_2_1_NAMESPACE;
import static fi.fmi.avi.converter.iwxxm.IWXXMConverterTests.IWXXM_3_0_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

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
        assertTrue(properties.contains(BulletinProperties.Name.HEADING));
        assertTrue(properties.contains(BulletinProperties.Name.MESSAGE));
    }

    @Test
    public void testParserWithTAF() throws Exception {
        final Document input = this.getBulletinDocument("taf-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    public void testParserWithSIGMET() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        final GenericAviationWeatherMessage msg = result.getConvertedMessage().get().getMessages().get(0);
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, msg.getMessageFormat());
        assertEquals(IWXXM_2_1_NAMESPACE, msg.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SIGMET, msg.getMessageType().orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, msg.getReportStatus());

        assertEquals("2012-08-25T16:00Z", msg.getIssueTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ZonedDateTime::toString)//
                .orElse(null));
        assertEquals("2012-08-25T16:00Z", msg.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));
        assertEquals("2012-08-25T22:00Z", msg.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expecteMap = new HashMap<>();
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUSO");
        assertEquals(expecteMap, msg.getLocationIndicators());
    }

    @Test
    public void testParserWithSIGMET30() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        final GenericAviationWeatherMessage msg = result.getConvertedMessage().get().getMessages().get(0);
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, msg.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, msg.getXMLNamespace().orElse(null));
        assertEquals(MessageType.SIGMET, msg.getMessageType().orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, msg.getReportStatus());

        assertEquals("2012-08-10T12:00Z", msg.getIssueTime()//
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                .map(ZonedDateTime::toString)//
                .orElse(null));
        assertEquals("2012-08-10T12:00Z", msg.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));
        assertEquals("2012-08-10T16:00Z", msg.getValidityTime()
                .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .map(ZonedDateTime::toString)
                .orElse(null));

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expecteMap = new HashMap<>();
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUSO");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDO");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");

        assertEquals(expecteMap, msg.getLocationIndicators());
    }

    @Test
    public void testParserWithTAF30() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TAF, message.getMessageType().orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());

        //check validtime
        assertEquals("2012-08-16T00:00Z", message.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", message.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        assertEquals("2012-08-15T18:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        //check aerodrome
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndiactors, message.getLocationIndicators());
    }

    @Test
    public void testParserWithTAF30Cancellation() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-cancellation-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        final GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
        assertEquals(IWXXM_3_0_NAMESPACE, message.getXMLNamespace().orElse(null));
        assertEquals(MessageType.TAF, message.getMessageType().orElse(null));
        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, message.getReportStatus());

        assertEquals("2012-08-16T00:00Z", message.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", message.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        assertEquals("2012-08-16T15:00Z",
                message.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndiactors = Collections.singletonMap(
                GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "YUDO");
        assertEquals(expectedIndiactors, message.getLocationIndicators());
    }
}
