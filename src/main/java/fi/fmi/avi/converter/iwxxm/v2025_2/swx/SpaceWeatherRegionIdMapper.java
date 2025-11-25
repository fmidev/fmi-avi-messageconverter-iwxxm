package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherIntensityAndRegion;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SpaceWeatherRegionIdMapper {

    private final List<RegionId> regionList = new ArrayList<>();

    public SpaceWeatherRegionIdMapper(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        createIdMap(analyses);
    }

    public List<RegionId> getRegionList(final int analysisNumber) {
        final List<RegionId> list = new ArrayList<>();
        for (final RegionId region : regionList) {
            if (region.getAnalysisNumber() == analysisNumber) {
                list.add(region);
            }
        }
        return list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    private void createIdMap(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        for (int analysisIndex = 0; analysisIndex < analyses.size(); analysisIndex++) {
            final SpaceWeatherAdvisoryAnalysis analysis = analyses.get(analysisIndex);
            int regionIndex = 0;
            for (final SpaceWeatherIntensityAndRegion intensityAndRegion : analysis.getIntensityAndRegions()) {
                for (final SpaceWeatherRegion region : intensityAndRegion.getRegions()) {
                    regionList.add(new RegionId(region, analysisIndex, regionIndex++));
                }
            }
        }

        for (final RegionId r : regionList) {
            if (isEmpty(r.getId())) {
                r.setId(IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID());
                for (final RegionId r2 : regionList) {
                    if (isEmpty(r2.getId()) && r2.getRegion().equals(r.getRegion())) {
                        r2.setId(r.getId());
                        r2.setDuplicate(true);
                    }
                }
            }
        }
    }

    private boolean isEmpty(final String string) {
        return string == null || string.trim().equals("");
    }

    public static class RegionId {
        private final SpaceWeatherRegion region;
        private final int analysisNumber;
        private final int regionNumber;

        private String id;
        private boolean duplicate;

        public RegionId(final SpaceWeatherRegion region, final int analysisNumber, final int regionNumber) {
            this.region = region;
            this.analysisNumber = analysisNumber;
            this.regionNumber = regionNumber;
            this.id = "";
        }

        public int getAnalysisNumber() {
            return analysisNumber;
        }

        public int getRegionNumber() {
            return regionNumber;
        }

        public boolean isDuplicate() {
            return duplicate;
        }

        public void setDuplicate(final boolean duplicate) {
            this.duplicate = duplicate;
        }

        public SpaceWeatherRegion getRegion() {
            return region;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

}
