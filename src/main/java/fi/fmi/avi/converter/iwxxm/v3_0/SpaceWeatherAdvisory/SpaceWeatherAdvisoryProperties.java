package fi.fmi.avi.converter.iwxxm.v3_0.SpaceWeatherAdvisory;

import java.util.List;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;

public class SpaceWeatherAdvisoryProperties extends AbstractPropertyContainer {

    public SpaceWeatherAdvisoryProperties() {
    }

    public enum Name implements PropertyName {
        ISSUE_TIME(PartialOrCompleteTimeInstant.class),
        ISSUING_CENTER_NAME(String.class),
        ADVISORY_NUMBER(AdvisoryNumber.class),
        REPLACE_ADVISORY_NUMBER(AdvisoryNumber.class),
        STATUS(Enum.class),
        PHENOMENA(List.class),
        ANALYSES(List.class),
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
