package fi.fmi.avi.converter.iwxxm.v21.metar;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.metar.HorizontalVisibility;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.SeaState;
import fi.fmi.avi.model.metar.WindShear;

/**
 * Created by rinne on 25/07/2018.
 */
public class ObservationRecordProperties extends AbstractPropertyContainer {

    public enum Name implements PropertyName {
        CLOUD_AND_VISIBILITY_OK(Boolean.class),
        SURFACE_WIND(ObservedSurfaceWind.class),
        VISIBILITY(HorizontalVisibility.class),
        RUNWAY_VISUAL_RANGE(RunwayVisualRange.class),
        PRESENT_WEATHER(Weather.class),
        CLOUD(ObservedClouds.class),
        AIR_TEMPERATURE(NumericMeasure.class),
        DEWPOINT_TEMPERATURE(NumericMeasure.class),
        QNH(NumericMeasure.class),
        RECENT_WEATHER(Weather.class),
        WIND_SHEAR(WindShear.class),
        SEA_STATE(SeaState.class),
        RUNWAY_STATE(RunwayState.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public ObservationRecordProperties() {
    }

}
