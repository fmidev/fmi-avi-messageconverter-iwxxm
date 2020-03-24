package fi.fmi.avi.converter.iwxxm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.PolygonsGeometry;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
import fi.fmi.avi.model.immutable.PolygonsGeometryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpaceWeatherIWXXMParserTest extends DOMParsingTestBase {
    @Autowired
    private AviMessageConverter converter;

    private String getInput(String fileName) throws IOException {
        InputStream is = null;
        try {
            String path = System.getProperty("user.dir") + "/src/test/resources/fi/fmi/avi/converter/iwxxm/" + fileName;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("could not locate file as resource or using path " + path);
            }
            Objects.requireNonNull(is);

            return IOUtils.toString(is, "UTF-8");
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    @Test
    public void testParser_A2_3() throws Exception {
        String input = getInput("spacewx-A2-3.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);

        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConvertedMessage().isPresent());

        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON", swx.getIssuingCenterName());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType().get());
        assertEquals("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_MOD", swx.getPhenomena().get(0));

        SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegion().get().get(0);
        assertEquals("http://codes.wmo.int/49-2/SpaceWxLocation/HNH", region.getLocationIndicator().get());

        PhenomenonGeometryWithHeight phenomenon = region.getGeographiclocation().get();
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00:00Z") , phenomenon.getTime().get().getCompleteTime().get());
        assertEquals(Arrays.asList(-180.0, 90.0), ((PolygonsGeometry)phenomenon.getGeometry().get().getGeoGeometry().get()).getPolygons().get(0));
    }

    @Test
    public void testParser_A2_4() throws Exception {
        String input = getInput("spacewx-A2-4.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        for(ConversionIssue con : result.getConversionIssues()) {
            //System.out.println(con.getCause());
            System.out.println(con.getMessage());
        }
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConvertedMessage().isPresent());

        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON", swx.getIssuingCenterName());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.FORECAST, swx.getAnalyses().get(0).getAnalysisType().get());
        assertEquals("http://codes.wmo.int/49-2/SpaceWxPhenomena/RADIATION_MOD", swx.getPhenomena().get(0));

        assertTrue(swx.getAnalyses().get(4).isNoPhenomenaExpected());

        SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegion().get().get(0);
        assertEquals("http://codes.wmo.int/49-2/SpaceWxLocation/HNH", region.getLocationIndicator().get());

        PhenomenonGeometryWithHeight phenomenon = region.getGeographiclocation().get();
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00:00Z") , phenomenon.getTime().get().getCompleteTime().get());
        assertEquals(Arrays.asList(-180.0, 90.0), ((PolygonsGeometry)phenomenon.getGeometry().get().getGeoGeometry().get()).getPolygons().get(0));
    }

    @Test
    public void testParser_A2_5() throws Exception {
        String input = getInput("spacewx-A2-5.xml");

        ConversionResult<SpaceWeatherAdvisory> result = converter.convertMessage(input, IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, ConversionHints.EMPTY);
        for(ConversionIssue issue : result.getConversionIssues()) {
            System.out.print(issue.getMessage());
        }
        assertTrue( "No issues should have been found", result.getConversionIssues().isEmpty());
        assertTrue(result.getConvertedMessage().isPresent());

        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals("DONLON", swx.getIssuingCenterName());
        assertEquals(2016, swx.getAdvisoryNumber().getYear());
        assertEquals(2, swx.getAdvisoryNumber().getSerialNumber());
        assertEquals(ZonedDateTime.parse("2016-11-08T00:00Z"), swx.getIssueTime().get().getCompleteTime().get());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, swx.getAnalyses().get(0).getAnalysisType().get());
        assertEquals("http://codes.wmo.int/49-2/SpaceWxPhenomena/HF_COM_SEV", swx.getPhenomena().get(0));

        SpaceWeatherRegion region = swx.getAnalyses().get(0).getRegion().get().get(0);
        assertEquals("http://codes.wmo.int/49-2/SpaceWxLocation/DAYLIGHT_SIDE", region.getLocationIndicator().get());

        PhenomenonGeometryWithHeight phenomenon = region.getGeographiclocation().get();
        assertEquals(ZonedDateTime.parse("2016-11-08T01:00:00Z") , phenomenon.getTime().get().getCompleteTime().get());

        assertEquals(Arrays.asList(-16.6392, 160.9368), ((CircleByCenterPoint)phenomenon.getGeometry().get().getGeoGeometry().get()).getCoordinates());
        assertEquals(new Double(5409.75), ((CircleByCenterPoint)phenomenon.getGeometry().get().getGeoGeometry().get()).getRadius().getValue());
        assertEquals("[nmi_i]", ((CircleByCenterPoint)phenomenon.getGeometry().get().getGeoGeometry().get()).getRadius().getUom());
    }
}
