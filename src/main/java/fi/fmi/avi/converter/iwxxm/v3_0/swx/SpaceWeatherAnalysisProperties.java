package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;

public class SpaceWeatherAnalysisProperties extends AbstractPropertyContainer {

    public SpaceWeatherAnalysisProperties() {
    }

    public enum Name implements AbstractPropertyContainer.PropertyName {
        ANALYSIS_TIME(PartialOrCompleteTimeInstant.class),//
        ANALYSIS_TYPE(SpaceWeatherAdvisoryAnalysis.Type.class),//
        REGION(SpaceWeatherRegionProperties.class),//
        NO_PHENOMENON_EXPECTED(Boolean.class),//
        NO_INFORMATION_AVAILABLE(Boolean.class);

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
