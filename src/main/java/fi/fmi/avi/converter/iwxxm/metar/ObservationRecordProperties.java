package fi.fmi.avi.converter.iwxxm.metar;

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
import icao.iwxxm21.MeteorologicalAerodromeObservationRecordType;

/**
 * Created by rinne on 25/07/2018.
 */
public class ObservationRecordProperties extends AbstractPropertyContainer<MeteorologicalAerodromeObservationRecordType> {

    public enum Name {
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

        private Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public ObservationRecordProperties(final MeteorologicalAerodromeObservationRecordType record) {
        super(record);
    }

    @Override
    protected Class<?> getAcceptedType(final Object key) {
        if (Name.class.isAssignableFrom(key.getClass())) {
            return ((Name) key).getAcceptedType();
        } else {
            throw new IllegalArgumentException("Key must be of type " + Name.class.getCanonicalName());
        }
    }
}
