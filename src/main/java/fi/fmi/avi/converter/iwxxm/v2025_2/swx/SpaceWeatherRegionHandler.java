package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherRegionImpl;

import java.util.*;

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

    private final Map<Integer, List<RegionId>> regionsByAnalysis;

    /**
     * Constructs a mapper by processing all analyses and assigning unique IDs to distinct regions.
     *
     * @param analyses the list of space weather advisory analyses containing regions to map
     */
    public SpaceWeatherRegionHandler(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        final Map<SpaceWeatherRegion, String> regionToId = new HashMap<>();
        final Map<Integer, List<RegionId>> result = new HashMap<>();

        for (int analysisIndex = 0; analysisIndex < analyses.size(); analysisIndex++) {
            final List<RegionId> analysisRegions = new ArrayList<>();

            analyses.get(analysisIndex).getIntensityAndRegions()
                    .forEach(intensityAndRegion -> intensityAndRegion.getRegions()
                            .forEach(region -> {
                                final SpaceWeatherRegion roundedRegion = roundPolygonGeometryCoordinates(region);
                                final boolean isDuplicate = regionToId.containsKey(roundedRegion);
                                final String id = regionToId.computeIfAbsent(roundedRegion,
                                        r -> IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID());
                                analysisRegions.add(new RegionId(roundedRegion, id, isDuplicate));
                            }));

            result.put(analysisIndex, Collections.unmodifiableList(analysisRegions));
        }

        this.regionsByAnalysis = Collections.unmodifiableMap(result);
    }

    private static SpaceWeatherRegion roundPolygonGeometryCoordinates(final SpaceWeatherRegion region) {
        return region.getAirSpaceVolume()
                .flatMap(volume -> volume.getHorizontalProjection()
                        .filter(geom -> geom instanceof PolygonGeometry)
                        .map(geom -> (PolygonGeometry) geom)
                        .<SpaceWeatherRegion>map(polygon -> {
                            final List<Double> positions = polygon.getExteriorRingPositions();
                            final List<Double> roundedPositions = roundAndRemoveConsecutiveDuplicates(positions);
                            return SpaceWeatherRegionImpl.Builder.from(region)
                                    .setAirSpaceVolume(AirspaceVolumeImpl.Builder.from(volume)
                                            .setHorizontalProjection(PolygonGeometryImpl.Builder.from(polygon)
                                                    .setExteriorRingPositions(roundedPositions)
                                                    .build())
                                            .build())
                                    .build();
                        }))
                .orElse(region);
    }

    private static List<Double> roundAndRemoveConsecutiveDuplicates(final List<Double> positions) {
        final List<Double> result = new ArrayList<>();
        Double lastLat = null;
        Double lastLon = null;

        for (int i = 0; i < positions.size() - 1; i += 2) {
            final double roundedLat = Math.round(positions.get(i));
            final double roundedLon = Math.round(positions.get(i + 1));

            if (lastLat == null || lastLat != roundedLat || lastLon != roundedLon) {
                result.add(roundedLat);
                result.add(roundedLon);
                lastLat = roundedLat;
                lastLon = roundedLon;
            }
        }
        return result;
    }

    /**
     * Retrieves the list of regions with assigned IDs for a specific analysis.
     *
     * @param analysisNumber index of the analysis
     * @return a list of region id mappings for the specified analysis
     */
    public List<RegionId> getRegionList(final int analysisNumber) {
        return regionsByAnalysis.getOrDefault(analysisNumber, Collections.emptyList());
    }

    public static class RegionId {
        private final SpaceWeatherRegion region;
        private final String id;
        private final boolean duplicate;

        RegionId(final SpaceWeatherRegion region, final String id, final boolean duplicate) {
            this.region = region;
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