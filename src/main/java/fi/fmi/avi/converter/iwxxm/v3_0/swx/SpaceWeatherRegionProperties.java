package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.swx.amd79.AirspaceVolume;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion;

public class SpaceWeatherRegionProperties extends AbstractPropertyContainer {
    public SpaceWeatherRegionProperties() {
    }

    public enum Name implements AbstractPropertyContainer.PropertyName {
        LOCATION_INDICATOR(SpaceWeatherRegion.SpaceWeatherLocation.class),//
        AIRSPACE_VOLUME(AirspaceVolume.class),//
        NIL_REASON(String.class),//
        TAC(String.class);

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
