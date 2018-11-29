package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.TAFBulletinHeading;
import fi.fmi.avi.model.taf.immutable.TAFBulletinHeadingImpl;
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
        List<TAF> tafs = new ArrayList<>();
        for (String fName : fileNames) {
            TAF t = readFromJSON(fName);
            AerodromeImpl.Builder airportBuilder = new AerodromeImpl.Builder().setDesignator("EETN")
                    .setName("Tallinn Airport")
                    .setFieldElevationValue(40.0)
                    .setLocationIndicatorICAO("EETN")
                    .setReferencePoint(new GeoPositionImpl.Builder().setCoordinateReferenceSystemId("http://www.opengis.net/def/crs/EPSG/0/4326")
                            .setCoordinates(new Double[] { 24.8325, 59.413333 })
                            .setElevationValue(40.0)
                            .setElevationUom("m")
                            .build());
            TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
            tafBuilder.setAerodrome(airportBuilder.build())
                    .withCompleteIssueTime(YearMonth.of(2017, 7))
                    .withCompleteForecastTimes(YearMonth.of(2017, 7), 30, 11, ZoneId.of("Z"));
            tafs.add(tafBuilder.build());
        }

        TAFBulletinImpl.Builder bulletinBuilder = new TAFBulletinImpl.Builder()//
                .setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(2017, 7, 30, 11, 15, 0, 0, ZoneId.of("Z"))))//
                .setHeading(new TAFBulletinHeadingImpl.Builder()//
                        .setType(TAFBulletinHeading.Type.NORMAL)//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(31)//
                        .setValidLessThan12Hours(false)//
                        .build());
        bulletinBuilder.addAllMessages(tafs);
        return bulletinBuilder.build();
    }

    @Test
    public void testTAFBulletinStringSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING));
        TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        ConversionResult<String> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        assertTrue(result.getConvertedMessage().isPresent());
        assertNotNull(result.getConvertedMessage().get());
    }

    @Test
    public void testTAFBulletinDOMSerialization() throws Exception {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM));
        TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        ConversionResult<Document> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        Element docElement = result.getConvertedMessage().map(Document::getDocumentElement).orElse(null);
        assertNotNull(docElement);

        XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        String bulletinId = expr.evaluate(docElement);
        assertEquals("A_FTFI31EFKL_C_EFKL_201707301115--.xml", bulletinId);

        expr = xpath.compile("count(/collect:MeteorologicalBulletin/collect:meteorologicalInformation)");
        assertTrue(2 == Integer.parseInt(expr.evaluate(docElement)));

        expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation[1]/iwxxm:TAF"
                + "/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature"
                + "/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice" + "/aixm:ARP/aixm:ElevatedPoint/gml:pos");

        assertEquals("24.8325 59.413333", expr.evaluate(docElement));
    }

    protected TAF readFromJSON(String fileName) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        InputStream is = TAFBulletinIWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            return om.readValue(is, TAFImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }
}