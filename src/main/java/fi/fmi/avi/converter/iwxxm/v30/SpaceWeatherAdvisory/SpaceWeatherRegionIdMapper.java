package fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;

public class SpaceWeatherRegionIdMapper {

    private final List<RegionId> regionList = new ArrayList<>();

    public SpaceWeatherRegionIdMapper(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        createIdMap(analyses);
    }

    public List<RegionId> getRegionList(final int analysisNumber) {
        List<RegionId> list = new ArrayList<>();
        for (RegionId region : regionList) {
            if (region.getAnalysisNumber() == analysisNumber) {
                list.add(region);
            }
        }
        return list;
    }

    private void createIdMap(final List<SpaceWeatherAdvisoryAnalysis> analyses) {
        for (int i = 0; i < analyses.size(); i++) {
            SpaceWeatherAdvisoryAnalysis analysis = analyses.get(i);
            if (analysis.getRegion().isPresent()) {
                for (int a = 0; a < analysis.getRegion().get().size(); a++) {
                    SpaceWeatherRegion region = analysis.getRegion().get().get(a);
                    regionList.add(new RegionId(region, i, a));
                }
            }
        }

        for (RegionId r : regionList) {
            if (isEmpty(r.getId())) {
                r.setId(SpaceWeatherIWXXMSerializer.UUID_PREFIX + UUID.randomUUID().toString());
                for (RegionId r2 : regionList) {
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

    public class RegionId {
        private SpaceWeatherRegion region;
        private String id;
        private int analysisNumber;
        private int regionNumber;
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
