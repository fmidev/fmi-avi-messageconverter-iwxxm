package fi.fmi.avi.converter.iwxxm.v3_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.DOMParsingTestBase;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMParserTest extends DOMParsingTestBase {
    @Autowired
    private AviMessageConverter converter;

    private String getInput(final String fileName) throws IOException {
        InputStream is = null;
        try {
            is = SpaceWeatherIWXXMParserTest.class.getResourceAsStream(fileName);
            Objects.requireNonNull(is);

            return IOUtils.toString(is, "UTF-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void testParser_A2_3() throws Exception {
        final String input = getInput("spacewx-A2-3.xml");

        final ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        printIssues(result.getConversionIssues());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON WEATHER FORECAST CENTER", swx.getIssuingCenter().getName().get());
        assertEquals("DONLON", swx.getIssuingCenter().getDesignator().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD"), swx.getPhenomena().get(0));

        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/HNH"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final PolygonGeometry geometry = (PolygonGeometry) airspaceVolume.getHorizontalProjection().get();
        assertEquals(BigInteger.valueOf(2), geometry.getSrsDimension().get());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", geometry.getSrsName().get());
        assertEquals(Arrays.asList(-180.0, 90.0, -180.0, 60.0, 180.0, 60.0, 180.0, 90.0, -180.0, 90.0), geometry.getExteriorRingPositions());
        assertFalse(airspaceVolume.getUpperLimitReference().isPresent());
        assertFalse(airspaceVolume.getUpperLimit().isPresent());
    }

    @Test
    public void testParser_A2_4() throws Exception {
        final String input = getInput("spacewx-A2-4.xml");

        final ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);
        printIssues(result.getConversionIssues());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON WEATHER FORECAST CENTER", swx.getIssuingCenter().getName().get());
        assertEquals("DONLON", swx.getIssuingCenter().getDesignator().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.FORECAST, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/RADIATION_MOD"), swx.getPhenomena().get(0));

        assertTrue(swx.getAnalyses().get(4).getNilPhenomenonReason().isPresent());
        assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED, swx.getAnalyses().get(4).getNilPhenomenonReason().get());

        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/HNH"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final PolygonGeometry geometry = (PolygonGeometry) airspaceVolume.getHorizontalProjection().get();
        assertEquals(BigInteger.valueOf(2), geometry.getSrsDimension().get());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", geometry.getSrsName().get());
        assertEquals("STD", airspaceVolume.getUpperLimitReference().get());
        assertEquals(Arrays.asList(-180.0, 90.0, -180.0, 60.0, 180.0, 60.0, 180.0, 90.0, -180.0, 90.0), geometry.getExteriorRingPositions());
        final NumericMeasure upperLimit = airspaceVolume.getUpperLimit().get();
        assertEquals("FL", upperLimit.getUom());
        assertEquals(Double.valueOf(350), upperLimit.getValue());

        final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        System.out.println(OBJECT_MAPPER.writeValueAsString(swx));
    }

    @Test
    public void testParser_A2_5() throws Exception {
        final String input = getInput("spacewx-A2-5.xml");

        final ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);
        printIssues(result.getConversionIssues());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON WEATHER FORECAST CENTER", swx.getIssuingCenter().getName().get());
        assertEquals("DONLON", swx.getIssuingCenter().getDesignator().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_SEV"), swx.getPhenomena().get(0));

        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/DAYLIGHT_SIDE"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final CircleByCenterPoint geometry = (CircleByCenterPoint) airspaceVolume.getHorizontalProjection().get();
        assertEquals(BigInteger.valueOf(2), geometry.getSrsDimension().get());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", geometry.getSrsName().get());
        assertEquals(Arrays.asList(-16.6392, 160.9368), geometry.getCenterPointCoordinates());
        final NumericMeasure gnm = NumericMeasureImpl.builder().setUom("[nmi_i]").setValue(5409.75).build();
        assertEquals(gnm, geometry.getRadius());
        assertFalse(airspaceVolume.getUpperLimitReference().isPresent());
        assertFalse(airspaceVolume.getUpperLimit().isPresent());
    }

    private void printIssues(final List<ConversionIssue> issues) {
        if (issues.size() > 0) {
            for (final ConversionIssue issue : issues) {
                System.out.println(issue.getMessage());
            }
        }
        assertTrue("No issues should have been found", issues.isEmpty());
    }

}
