package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.amd82.AirspaceVolume;
import fi.fmi.avi.model.swx.amd82.Intensity;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherRegionImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assigns unique IDs to space weather regions and intensity and region combinations, returning existing IDs for
 * duplicates to enable XML references.
 * <p>
 * Polygon coordinates are rounded to nearest integer before comparison to ensure regions that become identical
 * after rounding are properly deduplicated.
 * </p>
 */
public class SpaceWeatherIntensityAndRegionHandler {

    private final Map<IntensityAndRegionsKey, String> intensityAndRegionIds = new HashMap<>();
    private final Map<SpaceWeatherRegion, String> regionIds = new HashMap<>();

    private static SpaceWeatherRegion roundCoordinates(final SpaceWeatherRegion region) {
        return region.getAirSpaceVolume()
                .flatMap(vol -> vol.getHorizontalProjection()
                        .filter(geom -> geom instanceof PolygonGeometry)
                        .map(geom -> roundPolygonCoordinates(region, vol, (PolygonGeometry) geom)))
                .orElse(region);
    }

    private static SpaceWeatherRegion roundPolygonCoordinates(final SpaceWeatherRegion region,
                                                              final AirspaceVolume airspaceVolume,
                                                              final PolygonGeometry polygon) {
        final List<Double> rounded = roundAndDeduplicate(polygon.getExteriorRingPositions());
        return SpaceWeatherRegionImpl.Builder.from(region)
                .setAirSpaceVolume(AirspaceVolumeImpl.Builder.from(airspaceVolume)
                        .setHorizontalProjection(PolygonGeometryImpl.Builder.from(polygon)
                                .setExteriorRingPositions(rounded)
                                .build())
                        .build())
                .build();
    }

    private static List<Double> roundAndDeduplicate(final List<Double> positions) {
        final List<Double> result = new ArrayList<>();
        double prevLat = Double.NaN;
        double prevLon = Double.NaN;

        for (int i = 0; i + 1 < positions.size(); i += 2) {
            final double lat = Math.round(positions.get(i));
            final double lon = Math.round(positions.get(i + 1));
            if (lat != prevLat || lon != prevLon) {
                result.add(lat);
                result.add(lon);
                prevLat = lat;
                prevLon = lon;
            }
        }
        return result;
    }

    private static <K> IdAssignment register(final K key, final Map<K, String> registry) {
        final String existing = registry.get(key);
        if (existing != null) {
            return new IdAssignment(existing, false);
        }
        final String newId = IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID();
        registry.put(key, newId);
        return new IdAssignment(newId, true);
    }

    /**
     * Registers an intensity + regions combination, returning the ID and whether it's new.
     */
    public IdAssignment register(final Intensity intensity, final List<SpaceWeatherRegion> regions) {
        return register(new IntensityAndRegionsKey(intensity, normalizeRegions(regions)), intensityAndRegionIds);
    }

    /**
     * Registers a single region, returning the ID, whether it's new, and the normalized region.
     */
    public RegionIdAssignment registerRegion(final SpaceWeatherRegion region) {
        final SpaceWeatherRegion normalized = roundCoordinates(region);
        final IdAssignment assignment = register(normalized, regionIds);
        return new RegionIdAssignment(assignment.id, assignment.isNew, normalized);
    }

    private List<SpaceWeatherRegion> normalizeRegions(final List<SpaceWeatherRegion> regions) {
        return regions.stream()
                .map(SpaceWeatherIntensityAndRegionHandler::roundCoordinates)
                .collect(Collectors.toList());
    }

    /**
     * Result of assigning an ID to an intensity + regions combination.
     */
    public static class IdAssignment {
        private final String id;
        private final boolean isNew;

        private IdAssignment(final String id, final boolean isNew) {
            this.id = id;
            this.isNew = isNew;
        }

        public String getId() {
            return id;
        }

        public boolean isNew() {
            return isNew;
        }
    }

    /**
     * Result of assigning an ID to a region, includes the normalized region.
     */
    public static class RegionIdAssignment extends IdAssignment {
        private final SpaceWeatherRegion region;

        private RegionIdAssignment(final String id, final boolean isNew, final SpaceWeatherRegion region) {
            super(id, isNew);
            this.region = region;
        }

        public SpaceWeatherRegion getRegion() {
            return region;
        }
    }

    private static final class IntensityAndRegionsKey {
        private final Intensity intensity;
        private final List<SpaceWeatherRegion> regions;

        IntensityAndRegionsKey(final Intensity intensity, final List<SpaceWeatherRegion> regions) {
            this.intensity = intensity;
            this.regions = regions;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IntensityAndRegionsKey)) {
                return false;
            }
            final IntensityAndRegionsKey that = (IntensityAndRegionsKey) o;
            return intensity == that.intensity && Objects.equals(regions, that.regions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(intensity, regions);
        }
    }
}
