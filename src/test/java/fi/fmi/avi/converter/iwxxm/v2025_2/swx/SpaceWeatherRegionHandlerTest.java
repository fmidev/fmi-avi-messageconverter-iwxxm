package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.VerticalLimitsImpl;
import fi.fmi.avi.model.swx.amd82.*;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAnalysisImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherIntensityAndRegionImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherRegionImpl;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpaceWeatherRegionHandlerTest {

    private static SpaceWeatherRegion createRegion(final List<Double> coordinates) {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setExteriorRingPositions(coordinates)
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .build();
        final AirspaceVolume volume = AirspaceVolumeImpl.builder()
                .setHorizontalProjection(polygon)
                .withVerticalLimits(VerticalLimitsImpl.none())
                .build();
        return SpaceWeatherRegionImpl.builder()
                .setAirSpaceVolume(volume)
                .build();
    }

    private static SpaceWeatherAdvisoryAnalysis createAnalysis(final SpaceWeatherRegion... regions) {
        final SpaceWeatherIntensityAndRegion intensityAndRegion = SpaceWeatherIntensityAndRegionImpl.builder()
                .setIntensity(Intensity.MODERATE)
                .addAllRegions(Arrays.asList(regions))
                .build();
        return SpaceWeatherAdvisoryAnalysisImpl.builder()
                .setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION)
                .setTime(PartialOrCompleteTimeInstant.builder().buildPartial())
                .addAllIntensityAndRegions(Collections.singletonList(intensityAndRegion))
                .build();
    }

    @Test
    public void testSingleRegionInSingleAnalysis() {
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));
        final SpaceWeatherAdvisoryAnalysis analysis = createAnalysis(region);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Collections.singletonList(analysis));
        final List<SpaceWeatherRegionHandler.RegionId> regionList = handler.getRegionList(0);

        assertThat(regionList).hasSize(1);
        assertThat(regionList.get(0).isDuplicate()).isFalse();
    }

    @Test
    public void testMultipleDistinctRegionsInSingleAnalysis() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(15.0, 25.0, 35.0, 45.0, 55.0, 65.0));
        final SpaceWeatherAdvisoryAnalysis analysis = createAnalysis(region1, region2);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Collections.singletonList(analysis));
        final List<SpaceWeatherRegionHandler.RegionId> regionList = handler.getRegionList(0);

        assertThat(regionList).hasSize(2);
        assertThat(regionList.get(0).isDuplicate()).isFalse();
        assertThat(regionList.get(1).isDuplicate()).isFalse();
        assertThat(regionList.get(0).getId()).isNotEqualTo(regionList.get(1).getId());
    }

    @Test
    public void testDuplicateRegionsAcrossAnalyses() {
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));
        final SpaceWeatherAdvisoryAnalysis analysis1 = createAnalysis(region);
        final SpaceWeatherAdvisoryAnalysis analysis2 = createAnalysis(region);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Arrays.asList(analysis1, analysis2));
        final List<SpaceWeatherRegionHandler.RegionId> regionList1 = handler.getRegionList(0);
        final List<SpaceWeatherRegionHandler.RegionId> regionList2 = handler.getRegionList(1);

        assertThat(regionList1).hasSize(1);
        assertThat(regionList2).hasSize(1);
        assertThat(regionList1.get(0).isDuplicate()).isFalse();
        assertThat(regionList2.get(0).isDuplicate()).isTrue();
        assertThat(regionList1.get(0).getId()).isEqualTo(regionList2.get(0).getId());
    }

    @Test
    public void testCoordinateRoundingMakesDuplicates() {
        // These coordinates should become identical after rounding
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.4, 20.3, 30.6, 40.2, 50.0, 60.1));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(10.3, 20.4, 31.0, 40.3, 50.4, 60.2));
        final SpaceWeatherAdvisoryAnalysis analysis1 = createAnalysis(region1);
        final SpaceWeatherAdvisoryAnalysis analysis2 = createAnalysis(region2);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Arrays.asList(analysis1, analysis2));
        final List<SpaceWeatherRegionHandler.RegionId> regionList1 = handler.getRegionList(0);
        final List<SpaceWeatherRegionHandler.RegionId> regionList2 = handler.getRegionList(1);

        assertThat(regionList1.get(0).isDuplicate()).isFalse();
        assertThat(regionList2.get(0).isDuplicate()).isTrue();
        assertThat(regionList1.get(0).getId()).isEqualTo(regionList2.get(0).getId());
    }

    @Test
    public void testConsecutiveDuplicateCoordinatesRemoved() {
        // Coordinates that become duplicates after rounding should be removed
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.4, 20.3, 9.9, 20.2, 30.4, 40.5));
        final SpaceWeatherAdvisoryAnalysis analysis = createAnalysis(region);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Collections.singletonList(analysis));
        final List<SpaceWeatherRegionHandler.RegionId> regionList = handler.getRegionList(0);

        assertThat(regionList).hasSize(1);
        final SpaceWeatherRegion roundedRegion = regionList.get(0).getRegion();
        final PolygonGeometry polygon = (PolygonGeometry) roundedRegion.getAirSpaceVolume()
                .get().getHorizontalProjection().get();

        assertThat(polygon.getExteriorRingPositions()).containsExactly(10.0, 20.0, 30.0, 41.0);
    }

    @Test
    public void testRegionWithoutPolygonGeometryNotRounded() {
        final CircleByCenterPoint circle = CircleByCenterPointImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .setCenterPointCoordinates(Arrays.asList(15.31, -140.56))
                .setRadius(NumericMeasureImpl.builder()
                        .setUom("km")
                        .setValue(10100.0)
                        .build())
                .build();
        final AirspaceVolume volume = AirspaceVolumeImpl.builder()
                .setHorizontalProjection(circle)
                .withVerticalLimits(VerticalLimitsImpl.none())
                .build();
        final SpaceWeatherRegion region = SpaceWeatherRegionImpl.builder()
                .setLocationIndicator(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE)
                .setAirSpaceVolume(volume)
                .build();
        final SpaceWeatherAdvisoryAnalysis analysis = createAnalysis(region);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Collections.singletonList(analysis));
        final List<SpaceWeatherRegionHandler.RegionId> regionList = handler.getRegionList(0);

        assertThat(regionList).hasSize(1);
        final SpaceWeatherRegion resultRegion = regionList.get(0).getRegion();
        assertThat(resultRegion.getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE);
        assertThat(resultRegion.getAirSpaceVolume()).isPresent();

        final CircleByCenterPoint resultCircle = (CircleByCenterPoint) resultRegion.getAirSpaceVolume()
                .get().getHorizontalProjection().get();
        assertThat(resultCircle.getCenterPointCoordinates()).containsExactly(15.31, -140.56);
        assertThat(resultCircle.getRadius().getValue()).isEqualTo(10100.0);
    }

    @Test
    public void testMultipleAnalysesWithMixedRegions() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(50.0, 60.0, 70.0, 80.0));

        final SpaceWeatherAdvisoryAnalysis analysis1 = createAnalysis(region1, region2);
        final SpaceWeatherAdvisoryAnalysis analysis2 = createAnalysis(region1);

        final SpaceWeatherRegionHandler handler = new SpaceWeatherRegionHandler(Arrays.asList(analysis1, analysis2));

        final List<SpaceWeatherRegionHandler.RegionId> regionList1 = handler.getRegionList(0);
        final List<SpaceWeatherRegionHandler.RegionId> regionList2 = handler.getRegionList(1);

        assertThat(regionList1).hasSize(2);
        assertThat(regionList2).hasSize(1);

        assertThat(regionList1.get(0).isDuplicate()).isFalse();
        assertThat(regionList1.get(1).isDuplicate()).isFalse();

        assertThat(regionList2.get(0).isDuplicate()).isTrue();
        assertThat(regionList2.get(0).getId()).isEqualTo(regionList1.get(0).getId());
    }

}

