package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.print.attribute.HashDocAttributeSet;
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
public class GenericBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    private Document getBulletinDocument(final String filename) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream inputStream = GenericBulletinParserTest.class.getResourceAsStream(filename)) {
            return documentBuilder.parse(inputStream);
        }
    }

    @Test
    public void testScanner() throws Exception {
        final BulletinProperties properties = new BulletinProperties();
        final IWXXMGenericBulletinScanner scanner = new IWXXMGenericBulletinScanner();
        scanner.collectBulletinProperties(this.getBulletinDocument("taf-bulletin.xml"), properties, ConversionHints.EMPTY);
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

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get().getMessages().get(0);

        assertEquals("2012-08-25T16:00Z",
                msg.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2012-08-25T16:00Z",
                msg.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getStartTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2012-08-25T22:00Z",
                msg.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getEndTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expecteMap = new HashMap<>();
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUDO");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUCC");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUCC");

        assertEquals(expecteMap, msg.getLocationIndicators());

    }

    @Test
    public void testParserWithSIGMET30() throws Exception {
        final Document input = this.getBulletinDocument("sigmet-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get().getMessages().get(0);

        assertEquals("2012-08-10T12:00Z",
                msg.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2012-08-10T12:00Z",
                msg.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getStartTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        assertEquals("2012-08-10T16:00Z",
                msg.getValidityTime().flatMap(PartialOrCompleteTimePeriod::getEndTime).flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(ZonedDateTime::toString).orElse(null));

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expecteMap = new HashMap<>();
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, "YUSO");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, "YUDD");
        expecteMap.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, "YUDD");

        assertEquals(expecteMap, msg.getLocationIndicators());

    }

    @Test
    public void testParserWithIWXXM30() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);

        //check validtime
        assertEquals("2012-08-16T00:00Z", message.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", message.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        assertEquals("2012-08-15T18:00Z",
                message.getIssueTime().map(PartialOrCompleteTimeInstant::getCompleteTime).map(Optional::get).map(ZonedDateTime::toString).orElse(null));

        //check aerodrome
        assertEquals("YUDO", message.getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME));

        //check status
        assertEquals(AviationWeatherMessage.ReportStatus.NORMAL, message.getReportStatus());
        assertTrue(message.getMessageType().isPresent());
        assertEquals(MessageType.TAF, message.getMessageType().get());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());

    }

    @Test
    public void testParserWithIWXXM30Cancellation() throws Exception {
        final Document input = this.getBulletinDocument("iwxxm-30-cancellation-bulletin.xml");
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage(input,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage message = result.getConvertedMessage().get().getMessages().get(0);
        assertEquals("2012-08-16T00:00Z", message.getValidityTime().get().getStartTime().get().getCompleteTime().get().toString());
        assertEquals("2012-08-16T18:00Z", message.getValidityTime().get().getEndTime().get().getCompleteTime().get().toString());

        assertEquals("2012-08-16T15:00Z",
                message.getIssueTime().map(PartialOrCompleteTimeInstant::getCompleteTime).map(Optional::get).map(ZonedDateTime::toString).orElse(null));

        assertEquals("YUDO", message.getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME));

        assertEquals(AviationWeatherMessage.ReportStatus.AMENDMENT, message.getReportStatus());
        assertEquals(MessageType.TAF, message.getMessageType().get());
        assertEquals(GenericAviationWeatherMessage.Format.IWXXM, message.getMessageFormat());
    }
}
