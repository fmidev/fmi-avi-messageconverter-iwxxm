package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps space weather regions across multiple analyses to unique identifiers for IWXXM serialization.
 *
 * <p>
 * This mapper handles the deduplication of regions that appear in multiple analyses. When the same region appears in
 * different analyses, the first occurrence gets a unique ID, while subsequent occurrences refer to that ID.
 * </p>
 */
public class SpaceWeatherRegionIdMapper {

    private final List<RegionId> regionList;

    /**
     * Constructs a mapper by processing all analyses and assigning unique IDs to distinct regions.
     *
     * @param analyses the list of space weather advisory analyses containing regions to map
     */
    public SpaceWeatherRegionIdMapper(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        final Map<SpaceWeatherRegion, String> regionToId = new HashMap<>();
        final List<RegionId> result = new ArrayList<>();

        for (int analysisIndex = 0; analysisIndex < analyses.size(); analysisIndex++) {
            final int index = analysisIndex;
            analyses.get(analysisIndex).getIntensityAndRegions()
                    .forEach(intensityAndRegion -> intensityAndRegion.getRegions()
                            .forEach(region -> {
                                final boolean isDuplicate = regionToId.containsKey(region);
                                final String id = regionToId.computeIfAbsent(region,
                                        r -> IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID());
                                result.add(new RegionId(region, index, id, isDuplicate));
                            }));
        }

        this.regionList = Collections.unmodifiableList(result);
    }

    /**
     * Retrieves the list of regions with assigned IDs for a specific analysis.
     *
     * @param analysisNumber the zero-based index of the analysis
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