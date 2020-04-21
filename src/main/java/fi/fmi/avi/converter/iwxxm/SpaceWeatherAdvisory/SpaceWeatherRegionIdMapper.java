package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;

public class SpaceWeatherRegionIdMapper {

    private static final List<RegionId> regionList = new ArrayList<>();

    public static RegionId getRegionId(int analysis, int region) {
        for(RegionId item : regionList) {
            if(item.getAnalysisNumber() == analysis && item.getRegionNumber() == region) {
                return item;
            }
        }
        return null;
    }


    public static void createIdMap(List<SpaceWeatherAdvisoryAnalysis> analyses) {
        for(int i = 0; i < analyses.size(); i++) {
            SpaceWeatherAdvisoryAnalysis analysis = analyses.get(i);
            if(analysis.getRegion().isPresent()) {
                for(int a = 0; a < analysis.getRegion().get().size(); a++) {
                    SpaceWeatherRegion region = analysis.getRegion().get().get(a);
                    regionList.add(new RegionId(region, i, a));
                }
            }
        }

        for(RegionId r : regionList) {
            if(r.getId().equals("")) {
                r.setId("uuid." + UUID.randomUUID().toString());
                for(RegionId r2: regionList) {
                    if(r2.getId().equals("") && r2.getS().equals(r.getS())) {
                        r2.setId(r.getId());
                        r2.setDuplicate(true);
                    }
                }
            }
        }
    }

    public static class RegionId {
        private SpaceWeatherRegion s;
        private String id;
        private int analysisNumber;
        private int regionNumber;

        public RegionId(SpaceWeatherRegion s, int analysisNumber, int regionNumber ) {
            this.s = s;
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

        private boolean duplicate;

        public SpaceWeatherRegion getS() {
            return s;
        }

        public void setS(final SpaceWeatherRegion s) {
            this.s = s;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

}
