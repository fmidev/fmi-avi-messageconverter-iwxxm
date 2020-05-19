package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.NextAdvisory;

public class SpaceWeatherAdvisoryProperties extends AbstractPropertyContainer {

    public SpaceWeatherAdvisoryProperties() {
    }

    public enum Name implements PropertyName {
        ISSUE_TIME(PartialOrCompleteTimeInstant.class),
        ISSUING_CENTER_NAME(String.class),
        ADVISORY_NUMBER(AdvisoryNumber.class),
        REPLACE_ADVISORY_NUMBER(AdvisoryNumber.class),
        PHENOMENA(String.class),
        ANALYSES(SpaceWeatherAnalysisProperties.class),
        REMARKS(String.class),
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
