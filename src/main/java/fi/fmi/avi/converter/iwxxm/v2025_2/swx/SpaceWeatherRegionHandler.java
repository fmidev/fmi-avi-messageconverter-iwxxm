package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherRegionImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps space weather regions across multiple analyses to unique identifiers for IWXXM serialization.
 *
 * <p>
 * This mapper handles the deduplication of regions that appear in multiple analyses. Polygon geometry coordinates
 * are rounded to the nearest integer before comparison to ensure regions that become identical after rounding are
 * properly deduplicated. When the same region appears in different analyses, the first occurrence gets a unique ID,
 * while subsequent occurrences refer to that ID.
 * </p>
 */
public class SpaceWeatherRegionHandler {

    private final List<RegionId> regionList;

    /**
     * Constructs a mapper by processing all analyses and assigning unique IDs to distinct regions.
     *
     * @param analyses the list of space weather advisory analyses containing regions to map
     */
    public SpaceWeatherRegionHandler(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        final Map<SpaceWeatherRegion, String> regionToId = new HashMap<>();
        final List<RegionId> result = new ArrayList<>();

        for (int analysisIndex = 0; analysisIndex < analyses.size(); analysisIndex++) {
            final int index = analysisIndex;
            analyses.get(analysisIndex).getIntensityAndRegions()
                    .forEach(intensityAndRegion -> intensityAndRegion.getRegions()
                            .forEach(region -> {
                                final SpaceWeatherRegion roundedRegion = roundPolygonGeometryCoordinates(region);
                                final boolean isDuplicate = regionToId.containsKey(roundedRegion);
                                final String id = regionToId.computeIfAbsent(roundedRegion,
                                        r -> IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID());
                                result.add(new RegionId(roundedRegion, index, id, isDuplicate));
                            }));
        }

        this.regionList = Collections.unmodifiableList(result);
    }

    private static SpaceWeatherRegion roundPolygonGeometryCoordinates(final SpaceWeatherRegion region) {
        return region.getAirSpaceVolume()
                .flatMap(volume -> volume.getHorizontalProjection()
                        .filter(geom -> geom instanceof PolygonGeometry)
                        .map(geom -> (PolygonGeometry) geom)
                        .map(polygon -> (SpaceWeatherRegion) SpaceWeatherRegionImpl.Builder.from(region)
                                .setAirSpaceVolume(AirspaceVolumeImpl.Builder.from(volume)
                                        .setHorizontalProjection(PolygonGeometryImpl.Builder.from(polygon)
                                                .setExteriorRingPositions(polygon.getExteriorRingPositions().stream()
                                                        .map(value -> (double) Math.round(value))
                                                        .collect(Collectors.toList()))
                                                .build())
                                        .build())
                                .build()))
                .orElse(region);
    }

    /**
     * Retrieves the list of regions with assigned IDs for a specific analysis.
     *
     * @param analysisNumber index of the analysis
     * @return a list of region id mappings for the specified analysis
     */
    public List<RegionId> getRegionList(final int analysisNumber) {
        return regionList.stream()
                .filter(region -> region.analysisNumber == analysisNumber)
                .collect(Collectors.toList());
    }

    public static class RegionId {
        private final SpaceWeatherRegion region;
        private final int analysisNumber;
        private final String id;
        private final boolean duplicate;

        RegionId(final SpaceWeatherRegion region, final int analysisNumber, final String id, final boolean duplicate) {
            this.region = region;
            this.analysisNumber = analysisNumber;
            this.id = id;
            this.duplicate = duplicate;
        }

        public SpaceWeatherRegion getRegion() {
            return region;
        }

        public String getId() {
            return id;
        }

        public boolean isDuplicate() {
            return duplicate;
        }
    }
}