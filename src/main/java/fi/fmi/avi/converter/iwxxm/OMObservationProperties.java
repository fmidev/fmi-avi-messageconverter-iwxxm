package fi.fmi.avi.converter.iwxxm;

import net.opengis.om20.OMObservationType;

import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.GeoPosition;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import wmo.metce2013.ProcessType;

/**
 * Container class for properties parsed from an IWXXM OMObservationType.
 */
public class OMObservationProperties extends AbstractPropertyContainer<OMObservationType> {

    public enum Name {
        TYPE(String.class),
        PHENOMENON_TIME(PartialOrCompleteTime.class),
        RESULT_TIME(PartialOrCompleteTimeInstant.class),
        VALID_TIME(PartialOrCompleteTimePeriod.class),
        PROCEDURE(ProcessType.class),
        OBSERVED_PROPERTY(String.class),
        AERODROME(Aerodrome.class), SAMPLING_POINT(GeoPosition.class), RESULT(Object.class);

        private Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public OMObservationProperties(final OMObservationType baseFct) {
        super(baseFct);
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
