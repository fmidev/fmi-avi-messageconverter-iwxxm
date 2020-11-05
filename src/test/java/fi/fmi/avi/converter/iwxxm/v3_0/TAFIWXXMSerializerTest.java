package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.DoubleToIntFunction;

import org.apache.commons.io.IOExceptionWithCause;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMSerializerTest {
    @Autowired
    private AviMessageConverter converter;



    @Test
    public void confTest() throws IOException {
        assertTrue(converter.isSpecificationSupported(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING));
    }

    @Test
    public void conversionTest() throws IOException {
        TAF input = getTAF();

        final ConversionResult<String> result = converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING,
                ConversionHints.EMPTY);

        checkConversionIssues(result.getConversionIssues());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());


    }

    private void checkConversionIssues(List<ConversionIssue> issues) {
        if(!issues.isEmpty()) {
            for(ConversionIssue issue : issues) {
                System.out.println("******************");
                System.out.println("Severity: " + issue.getSeverity());
                System.out.println(issue.getMessage());
                System.out.println("******************");
                fail();
            }
        }
    }


    private TAF getTAF() throws IOException {
        final TAF t = readFromJSON("taf12.json");
        final AerodromeImpl.Builder airportBuilder = AerodromeImpl.builder()
                .setDesignator("EETN")
                .setName("Tallinn Airport")
                .setFieldElevationValue(40.0)
                .setFieldElevationUom("FT")
                .setLocationIndicatorICAO("EETN")
                .setReferencePoint(ElevatedPointImpl.builder()
                        .setCrs(CoordinateReferenceSystemImpl.wgs84())
                        .addCoordinates(24.8325, 59.413333)
                        .setElevationValue(40.0)
                        .setElevationUom("m")
                        .build());
        final TAFImpl.Builder tafBuilder = TAFImpl.immutableCopyOf(t).toBuilder();
        tafBuilder.setAerodrome(airportBuilder.build())
                .withCompleteIssueTime(YearMonth.of(2012, 8))
                .withCompleteForecastTimes(YearMonth.of(2017, 7), 30, 11, ZoneId.of("Z"))
                .setTranslatedTAC("EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z\n" + "TEMPO 3012/3018 3000 RADZ BR OVC004\n"
                        + "BECMG 3018/3020 BKN008 SCT015CB\n" + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" + "BECMG 3104/3106 21016G30KT=")
                .setTranslationTime(ZonedDateTime.now());

        return tafBuilder.build();
    }

    protected TAF readFromJSON(final String fileName) throws IOException {
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        final InputStream is = fi.fmi.avi.converter.iwxxm.v2_1.TAFIWXXMSerializerTest.class.getResourceAsStream(fileName);
        if (is != null) {
            return om.readValue(is, TAFImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
    }
}
