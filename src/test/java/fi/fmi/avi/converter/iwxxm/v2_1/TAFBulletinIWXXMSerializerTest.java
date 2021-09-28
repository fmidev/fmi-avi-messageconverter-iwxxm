package fi.fmi.avi.converter.iwxxm.v2_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinIWXXMSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    private TAFBulletin getTAFBulletin(final String... fileNames) throws IOException {
        final List<TAF> tafs = new ArrayList<>();
        for (final String fName : fileNames) {
            final TAF t = readFromJSON(fName);
            final AerodromeImpl.Builder airportBuilder = AerodromeImpl.builder()
                    .setDesignator("EETN")
                    .setName("Tallinn Airport")
                    .setFieldElevationValue(40.0)
                    .setLocationIndicatorICAO("EETN")
                    .setReferencePoint(ElevatedPointImpl.builder()//
                            .setCrs(CoordinateReferenceSystemImpl.wgs84())//
                            .addCoordinates(24.8325, 59.413333)//
                            .setElevationValue(40.0)//
                            .setElevationUom("m")//
                            .build());
            final TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
            tafBuilder.setAerodrome(airportBuilder.build())
                    .withCompleteIssueTime(YearMonth.of(2017, 7))
                    .withCompleteForecastTimes(YearMonth.of(2017, 7), 30, 11, ZoneId.of("Z"));
            tafs.add(tafBuilder.build());
        }

        final TAFBulletinImpl.Builder bulletinBuilder = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)//
                        .setType(BulletinHeading.Type.NORMAL)//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(31)//
                        .setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2017, 7, 30, 11, 15, 0, 0, ZoneId.of("Z"))))//
                        .build())
                .setTimeStamp(ZonedDateTime.of(2017, 7, 30, 11, 15, 0, 0, ZoneId.of("Z")))
                .addTimeStampFields(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY);
        bulletinBuilder.addAllMessages(tafs);
        return bulletinBuilder.build();
    }

    @Test
    public void testTAFBulletinStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        final TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        final ConversionResult<String> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void testTAFBulletinDOMSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));
        final TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        final ConversionResult<Document> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        final Element docElement = result.getConvertedMessage().map(Document::getDocumentElement).orElse(null);
        assertNotNull(docElement);

        XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        final String bulletinId = expr.evaluate(docElement);
        assertEquals("A_LTFI31EFKL301115_C_EFKL_2017073011----.xml", bulletinId);

        expr = xpath.compile("count(/collect:MeteorologicalBulletin/collect:meteorologicalInformation)");
        assertEquals(2, Integer.parseInt(expr.evaluate(docElement)));

        expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation[1]/iwxxm:TAF"
                + "/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature"
                + "/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice" + "/aixm:ARP/aixm:ElevatedPoint/gml:pos");

        assertEquals("24.8325 59.413333", expr.evaluate(docElement));
    }

    protected TAF readFromJSON(final String fileName) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (InputStream inputStream = TAFBulletinIWXXMSerializerTest.class.getResourceAsStream(fileName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, TAFImpl.class);
            } else {
                throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
            }
        }
    }
}
