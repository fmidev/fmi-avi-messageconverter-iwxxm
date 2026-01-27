package fi.fmi.avi.converter.iwxxm.v2_1;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertConversionResult;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rinne on 19/07/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinIWXXMSerializerTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private TAFBulletin getTAFBulletin(final String... fileNames) throws IOException {
        final List<TAF> tafs = new ArrayList<>();
        for (final String fName : fileNames) {
            final TAF t = readFromJSON(fName, TAFImpl.class);
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
        assertThat(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING)).isTrue();
        final TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        final ConversionResult<String> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING);
        assertConversionResult(result).successfullyConverted();
    }

    @Test
    public void testTAFBulletinDOMSerialization() throws Exception {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM)).isTrue();
        final TAFBulletin tb = getTAFBulletin("taf12.json", "taf1.json");
        final ConversionResult<Document> result = converter.convertMessage(tb, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM);
        final Document doc = assertConversionResult(result).successfullyConverted();

        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        final NamespaceContext ctx = new IWXXMNamespaceContext();
        xpath.setNamespaceContext(ctx);

        final Element docElement = doc.getDocumentElement();

        XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
        final String bulletinId = expr.evaluate(docElement);
        assertThat(bulletinId).isEqualTo("A_LTFI31EFKL301115_C_EFKL_2017073011----.xml");

        expr = xpath.compile("count(/collect:MeteorologicalBulletin/collect:meteorologicalInformation)");
        assertThat(Integer.parseInt(expr.evaluate(docElement))).isEqualTo(2);

        expr = xpath.compile("/collect:MeteorologicalBulletin/collect:meteorologicalInformation[1]/iwxxm:TAF"
                + "/iwxxm:baseForecast/om:OM_Observation/om:featureOfInterest/sams:SF_SpatialSamplingFeature"
                + "/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice[1]/aixm:AirportHeliportTimeSlice" + "/aixm:ARP/aixm:ElevatedPoint/gml:pos");

        assertThat(expr.evaluate(docElement)).isEqualTo("24.8325 59.413333");
    }
}
