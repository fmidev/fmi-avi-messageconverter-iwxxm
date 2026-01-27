package fi.fmi.avi.converter.iwxxm.v3_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.amd79.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.*;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMParserTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    private static void assertContainsNoEmptyRegions(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        final List<String> emptyRegions = new ArrayList<>();
        for (int analysisIndex = 0; analysisIndex < analyses.size(); analysisIndex++) {
            final List<SpaceWeatherRegion> regions = analyses.get(analysisIndex).getRegions();
            for (int regionIndex = 0; regionIndex < regions.size(); regionIndex++) {
                final SpaceWeatherRegion region = regions.get(regionIndex);
                if (isEmpty(region)) {
                    emptyRegions.add("analysis[" + analysisIndex + "]/region[" + regionIndex + "]");
                }
            }
        }
        assertTrue("Empty analysis regions: " + emptyRegions, emptyRegions.isEmpty());
    }

    private static boolean isEmpty(final SpaceWeatherRegion region) {
        return Arrays.stream(SpaceWeatherRegion.class.getMethods())//
                .filter(method -> method.getParameterCount() == 0//
                        && Optional.class.isAssignableFrom(method.getReturnType())//
                        && method.getName().startsWith("get"))//
                .map(method -> {
                    try {
                        final Optional<?> optional = (Optional<?>) method.invoke(region);
                        //noinspection OptionalAssignedToNull
                        return optional == null ? Optional.empty() : optional;
                    } catch (final IllegalAccessException | InvocationTargetException e) {
                        throw new AssertionError("Unable to invoke " + method + ": " + e.getMessage());
                    }
                })//
                .noneMatch(Optional::isPresent);
    }

    @Test
    public void testParser_A2_3() throws Exception {
        final String input = readResourceToString("spacewx-A2-3.xml");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        final SpaceWeatherAdvisoryAmd79 swx = assertThatConversionResult(result).isSuccessful().getMessage();
        assertEquals("DONLON", swx.getIssuingCenter().getName().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD"), swx.getPhenomena().get(0));

        assertContainsNoEmptyRegions(swx.getAnalyses());
        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/HNH"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final PolygonGeometry geometry = (PolygonGeometry) airspaceVolume.getHorizontalProjection().get();
        assertEquals(Optional.of(CoordinateReferenceSystemImpl.wgs84()), CoordinateReferenceSystemImpl.immutableCopyOf(geometry.getCrs()));
        assertEquals(Arrays.asList(90.0, -180.0, 60.0, -180.0, 60.0, 180.0, 90.0, 180.0, 90.0, -180.0), geometry.getExteriorRingPositions());
        assertFalse(airspaceVolume.getUpperLimitReference().isPresent());
        assertFalse(airspaceVolume.getUpperLimit().isPresent());

        assertEquals(NextAdvisory.Type.NO_FURTHER_ADVISORIES, swx.getNextAdvisory().getTimeSpecifier());
        assertFalse("getNextAdvisory().getTime()", swx.getNextAdvisory().getTime().isPresent());
    }

    @Test
    public void testParser_remark_parsing() throws Exception {
        final String input = readResourceToString("spacewx-A2-4.xml");
        final List<String> expected = Arrays.asList("RADIATION", "LVL", "EXCEEDED", "100", "PCT", "OF", "BACKGROUND", "LVL", "AT", "FL350", "AND", "ABV.",
                "THE", "CURRENT", "EVENT", "HAS", "PEAKED", "AND", "LVL", "SLW", "RTN", "TO", "BACKGROUND", "LVL.", "SEE", "WWW.SPACEWEATHERPROVIDER.WEB");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);

        final SpaceWeatherAdvisoryAmd79 swx = assertThatConversionResult(result).isSuccessful().getMessage();
        assertTrue(swx.getRemarks().isPresent());
        assertEquals(26, swx.getRemarks().get().size());
        assertEquals(expected, swx.getRemarks().get());
    }

    @Test
    public void testParser_A2_4() throws Exception {
        final String input = readResourceToString("spacewx-A2-4.xml");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);
        final SpaceWeatherAdvisoryAmd79 swx = assertThatConversionResult(result).isSuccessful().getMessage();
        assertEquals("DONLON", swx.getIssuingCenter().getName().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.FORECAST, swx.getAnalyses().get(1).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/RADIATION_MOD"), swx.getPhenomena().get(0));

        assertTrue(swx.getAnalyses().get(4).getNilPhenomenonReason().isPresent());
        assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED, swx.getAnalyses().get(4).getNilPhenomenonReason().get());

        assertContainsNoEmptyRegions(swx.getAnalyses());
        final SpaceWeatherRegion region0 = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/HNH"),
                region0.getLocationIndicator().get());

        final AirspaceVolume region0AirspaceVolume = region0.getAirSpaceVolume().get();
        assertTrue(region0AirspaceVolume.getHorizontalProjection().isPresent());
        final PolygonGeometry geometry = (PolygonGeometry) region0AirspaceVolume.getHorizontalProjection().get();
        assertEquals(Optional.of(CoordinateReferenceSystemImpl.wgs84()), CoordinateReferenceSystemImpl.immutableCopyOf(geometry.getCrs()));
        assertEquals(Arrays.asList(90.0, -180.0, 60.0, -180.0, 60.0, 180.0, 90.0, 180.0, 90.0, -180.0), geometry.getExteriorRingPositions());
        assertEquals(340.0, region0AirspaceVolume.getLowerLimit().map(NumericMeasure::getValue).orElse(Double.NaN), 0.001);
        assertEquals("FL", region0AirspaceVolume.getLowerLimit().map(NumericMeasure::getUom).orElse(null));
        assertEquals("STD", region0AirspaceVolume.getLowerLimitReference().orElse(null));
        assertFalse("getUpperLimit", region0AirspaceVolume.getUpperLimit().isPresent());
        assertFalse("getUpperLimitReference", region0AirspaceVolume.getUpperLimitReference().isPresent());

        final AirspaceVolume region1AirspaceVolume = swx.getAnalyses().get(0).getRegions().get(1).getAirSpaceVolume().get();
        assertEquals(Optional.of(CoordinateReferenceSystemImpl.wgs84()), CoordinateReferenceSystemImpl.immutableCopyOf(geometry.getCrs()));
        assertEquals(250, region1AirspaceVolume.getLowerLimit().map(NumericMeasure::getValue).orElse(Double.NaN), 0.001);
        assertEquals("FL", region1AirspaceVolume.getLowerLimit().map(NumericMeasure::getUom).orElse(null));
        assertEquals("SFC", region1AirspaceVolume.getLowerLimitReference().orElse(null));
        assertEquals(350.0, region1AirspaceVolume.getUpperLimit().map(NumericMeasure::getValue).orElse(Double.NaN), 0.001);
        assertEquals("FL", region1AirspaceVolume.getUpperLimit().map(NumericMeasure::getUom).orElse(null));
        assertEquals("STD", region1AirspaceVolume.getUpperLimitReference().orElse(null));

        assertEquals(NextAdvisory.Type.NEXT_ADVISORY_BY, swx.getNextAdvisory().getTimeSpecifier());
        assertTrue("getNextAdvisory().getTime()", swx.getNextAdvisory().getTime().isPresent());

        final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    @Test
    public void testParser_A2_5() throws Exception {
        final String input = readResourceToString("spacewx-A2-5.xml");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);
        final SpaceWeatherAdvisoryAmd79 swx = assertThatConversionResult(result).isSuccessful().getMessage();
        assertEquals("DONLON", swx.getIssuingCenter().getName().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_SEV"), swx.getPhenomena().get(0));

        assertContainsNoEmptyRegions(swx.getAnalyses());
        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/DAYLIGHT_SIDE"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final CircleByCenterPoint geometry = (CircleByCenterPoint) airspaceVolume.getHorizontalProjection().get();
        assertEquals(Optional.of(CoordinateReferenceSystemImpl.wgs84()), CoordinateReferenceSystemImpl.immutableCopyOf(geometry.getCrs()));
        assertEquals(Arrays.asList(-16.64, 160.94), geometry.getCenterPointCoordinates());
        final NumericMeasure gnm = NumericMeasureImpl.builder().setUom("km").setValue(10100.0).build();
        assertEquals(gnm, geometry.getRadius());
        assertFalse(airspaceVolume.getUpperLimitReference().isPresent());
        assertFalse(airspaceVolume.getUpperLimit().isPresent());

        assertEquals(NextAdvisory.Type.NEXT_ADVISORY_AT, swx.getNextAdvisory().getTimeSpecifier());
        assertTrue("getNextAdvisory().getTime()", swx.getNextAdvisory().getTime().isPresent());
    }

    @Test
    public void testParser_daylight_side() throws Exception {
        final String input = readResourceToString("spacewx-daylight-side.xml");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                ConversionHints.EMPTY);
        final SpaceWeatherAdvisoryAmd79 swx = assertThatConversionResult(result).isSuccessful().getMessage();
        assertEquals("ACFJ", swx.getIssuingCenter().getName().get());
        assertEquals("OTHER:SWXC", swx.getIssuingCenter().getType().get());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType());
        assertEquals(SpaceWeatherPhenomenon.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD"), swx.getPhenomena().get(0));

        assertContainsNoEmptyRegions(swx.getAnalyses());
        final SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue("http://codes.wmo.int/49-2/SpaceWxLocation/DAYLIGHT_SIDE"),
                region.getLocationIndicator().get());

        final AirspaceVolume airspaceVolume = region.getAirSpaceVolume().get();
        assertTrue(airspaceVolume.getHorizontalProjection().isPresent());
        final CircleByCenterPoint geometry = (CircleByCenterPoint) airspaceVolume.getHorizontalProjection().get();
        assertEquals(Optional.of(CoordinateReferenceSystemImpl.wgs84()), CoordinateReferenceSystemImpl.immutableCopyOf(geometry.getCrs()));
        assertEquals(Arrays.asList(-16.64, 160.94), geometry.getCenterPointCoordinates());
        final NumericMeasure gnm = NumericMeasureImpl.builder().setUom("km").setValue(10100.0).build();
        assertEquals(gnm, geometry.getRadius());
        assertFalse(airspaceVolume.getUpperLimitReference().isPresent());
        assertFalse(airspaceVolume.getUpperLimit().isPresent());

        assertEquals(NextAdvisory.Type.NO_FURTHER_ADVISORIES, swx.getNextAdvisory().getTimeSpecifier());
    }

    @Test
    public void testParser_illegal_nextAdvisory_indeterminatePosition() throws IOException {
        final String input = readResourceToString("spacewx-A2-4.xml");
        for (final String illegalIndeterminatePosition : Arrays.asList("now", "unknown")) {
            final String illegalInput = input.replace("indeterminatePosition=\"before\"", "indeterminatePosition=\"" + illegalIndeterminatePosition + "\"");
            final ConversionResult<SpaceWeatherAdvisoryAmd79> result = converter.convertMessage(illegalInput, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO,
                    ConversionHints.EMPTY);
            assertThatConversionResult(result).hasIssueContaining(illegalIndeterminatePosition.toUpperCase(Locale.US));
        }
    }

}
