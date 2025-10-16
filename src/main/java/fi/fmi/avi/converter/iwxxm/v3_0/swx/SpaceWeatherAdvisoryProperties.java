package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.amd79.AdvisoryNumber;
import fi.fmi.avi.model.swx.amd79.IssuingCenter;
import fi.fmi.avi.model.swx.amd79.NextAdvisory;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherPhenomenon;

import java.util.List;

public class SpaceWeatherAdvisoryProperties extends AbstractPropertyContainer {

    public SpaceWeatherAdvisoryProperties() {
    }

    public enum Name implements PropertyName {
        REPORT_METADATA(GenericReportProperties.class),
        ISSUE_TIME(PartialOrCompleteTimeInstant.class),
        ISSUING_CENTER(IssuingCenter.class),
        ADVISORY_NUMBER(AdvisoryNumber.class),
        REPLACE_ADVISORY_NUMBER(AdvisoryNumber.class),
        PHENOMENA(SpaceWeatherPhenomenon.class),
        ANALYSES(SpaceWeatherAnalysisProperties.class),
        REMARKS(List.class),
        NEXT_ADVISORY(NextAdvisory.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }
}
