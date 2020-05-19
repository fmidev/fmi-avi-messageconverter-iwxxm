package fi.fmi.avi.converter.iwxxm.v3_0.SpaceWeatherAdvisory;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AirspaceVolume;

public class SpaceWeatherRegionProperties extends AbstractPropertyContainer {
    public SpaceWeatherRegionProperties() {
    }

    public enum Name implements AbstractPropertyContainer.PropertyName {
        LOCATION_INDICATOR(String.class), AIRSPACE_VOLUME(AirspaceVolume.class),
        NIL_REASON(String.class), TAC(String.class);

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
