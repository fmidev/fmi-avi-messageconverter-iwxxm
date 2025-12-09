package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.VerticalLimitsImpl;
import fi.fmi.avi.model.swx.amd82.AirspaceVolume;
import fi.fmi.avi.model.swx.amd82.Intensity;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherRegionImpl;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpaceWeatherIntensityAndRegionHandlerTest {

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

    @Test
    public void testSingleRegionIdAssignment() {
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment assignment = handler.registerRegion(region);

        assertThat(assignment.isNew()).isTrue();
        assertThat(assignment.getId()).isNotNull();
    }

    @Test
    public void testMultipleDistinctRegions() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(15.0, 25.0, 35.0, 45.0, 55.0, 65.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment assignment1 = handler.registerRegion(region1);
        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment assignment2 = handler.registerRegion(region2);

        assertThat(assignment1.isNew()).isTrue();
        assertThat(assignment2.isNew()).isTrue();
        assertThat(assignment1.getId()).isNotEqualTo(assignment2.getId());
    }

    @Test
    public void testDuplicateIntensityAndRegion() {
        // Same intensity and same region should result in duplicate
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment assignment1 = handler.register(Intensity.MODERATE, Collections.singletonList(region));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment assignment2 = handler.register(Intensity.MODERATE, Collections.singletonList(region));

        assertThat(assignment1.isNew()).isTrue();
        assertThat(assignment2.isNew()).isFalse();
        assertThat(assignment1.getId()).isEqualTo(assignment2.getId());
    }

    @Test
    public void testSameRegionDifferentIntensity() {
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment1 = handler.register(Intensity.MODERATE, Collections.singletonList(region));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment2 = handler.register(Intensity.SEVERE, Collections.singletonList(region));

        assertThat(iarAssignment1.isNew()).isTrue();
        assertThat(iarAssignment2.isNew()).isTrue();
        assertThat(iarAssignment1.getId()).isNotEqualTo(iarAssignment2.getId());

        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment regionAssignment1 = handler.registerRegion(region);
        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment regionAssignment2 = handler.registerRegion(region);

        assertThat(regionAssignment1.isNew()).isTrue();
        assertThat(regionAssignment2.isNew()).isFalse();
        assertThat(regionAssignment1.getId()).isEqualTo(regionAssignment2.getId());
    }

    @Test
    public void testCoordinateRoundingMakesDuplicates() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.4, 20.3, 30.6, 40.2, 50.0, 60.1));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(10.3, 20.4, 31.0, 40.3, 50.4, 60.2));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment1 = handler.register(Intensity.MODERATE, Collections.singletonList(region1));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment2 = handler.register(Intensity.MODERATE, Collections.singletonList(region2));

        assertThat(iarAssignment1.isNew()).isTrue();
        assertThat(iarAssignment2.isNew()).isFalse();
        assertThat(iarAssignment1.getId()).isEqualTo(iarAssignment2.getId());
    }

    @Test
    public void testConsecutiveDuplicateCoordinatesRemoved() {
        final SpaceWeatherRegion region = createRegion(Arrays.asList(10.4, 20.3, 9.9, 20.2, 30.4, 40.5));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
        final SpaceWeatherRegion normalized = handler.registerRegion(region).getRegion();

        final PolygonGeometry polygon = (PolygonGeometry) normalized.getAirSpaceVolume()
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

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
        final SpaceWeatherRegion normalized = handler.registerRegion(region).getRegion();

        assertThat(normalized.getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE);
        assertThat(normalized.getAirSpaceVolume()).isPresent();

        final CircleByCenterPoint resultCircle = (CircleByCenterPoint) normalized.getAirSpaceVolume()
                .get().getHorizontalProjection().get();
        assertThat(resultCircle.getCenterPointCoordinates()).containsExactly(15.31, -140.56);
        assertThat(resultCircle.getRadius().getValue()).isEqualTo(10100.0);
    }

    @Test
    public void testMultipleRegionsWithPartialDuplication() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(50.0, 60.0, 70.0, 80.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment1 = handler.register(Intensity.MODERATE, Arrays.asList(region1, region2));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment2 = handler.register(Intensity.MODERATE, Collections.singletonList(region1));

        assertThat(iarAssignment1.isNew()).isTrue();
        assertThat(iarAssignment2.isNew()).isTrue();
        assertThat(iarAssignment1.getId()).isNotEqualTo(iarAssignment2.getId());
    }

    @Test
    public void testMultipleDistinctIntensityAndRegions() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(50.0, 60.0, 70.0, 80.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment1 = handler.register(Intensity.MODERATE, Collections.singletonList(region1));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iarAssignment2 = handler.register(Intensity.SEVERE, Collections.singletonList(region2));

        assertThat(iarAssignment1.isNew()).isTrue();
        assertThat(iarAssignment2.isNew()).isTrue();
        assertThat(iarAssignment1.getId()).isNotEqualTo(iarAssignment2.getId());
    }

    @Test
    public void testMultipleIntensityAndRegionsDuplicated() {
        final SpaceWeatherRegion region1 = createRegion(Arrays.asList(10.0, 20.0, 30.0, 40.0));
        final SpaceWeatherRegion region2 = createRegion(Arrays.asList(50.0, 60.0, 70.0, 80.0));

        final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iar1First = handler.register(Intensity.MODERATE, Collections.singletonList(region1));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iar2First = handler.register(Intensity.SEVERE, Collections.singletonList(region2));

        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iar1Second = handler.register(Intensity.MODERATE, Collections.singletonList(region1));
        final SpaceWeatherIntensityAndRegionHandler.IdAssignment iar2Second = handler.register(Intensity.SEVERE, Collections.singletonList(region2));

        assertThat(iar1First.isNew()).isTrue();
        assertThat(iar2First.isNew()).isTrue();

        assertThat(iar1Second.isNew()).isFalse();
        assertThat(iar2Second.isNew()).isFalse();

        assertThat(iar1Second.getId()).isEqualTo(iar1First.getId());
        assertThat(iar2Second.getId()).isEqualTo(iar2First.getId());
    }

}
