package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;

public class SpaceWeatherRegionProperties extends AbstractPropertyContainer {
    public SpaceWeatherRegionProperties() {
    }

    public enum Name implements AbstractPropertyContainer.PropertyName {
        LOCATION_INDICATOR(String.class), PHENOMENON_LOCATION(PhenomenonGeometryWithHeight.class), NIL_REASON(String.class);

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
