package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

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
            for (int a = 0; a < analysis.getRegions().size(); a++) {
                SpaceWeatherRegion region = analysis.getRegions().get(a);
                regionList.add(new RegionId(region, i, a));
            }
        }

        for (RegionId r : regionList) {
            if (isEmpty(r.getId())) {
                r.setId(IWXXMConverterBase.UUID_PREFIX + UUID.randomUUID().toString());
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

    public static class RegionId {
        private final SpaceWeatherRegion region;
        private String id;
        private final int analysisNumber;
        private final int regionNumber;
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
