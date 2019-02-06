package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

/**
 * Container class for properties parsed from an IWXXM TAFType.
 *
 */
public class TAFProperties extends AbstractPropertyContainer {

    public enum Name {
        REPORT_METADATA(GenericReportProperties.class),
        ISSUE_TIME(PartialOrCompleteTimeInstant.class),
        VALID_TIME(PartialOrCompleteTimePeriod.class),
        BASE_FORECAST(OMObservationProperties.class),
        CHANGE_FORECAST(OMObservationProperties.class),
        PREV_REPORT_AERODROME(Aerodrome.class),
        PREV_REPORT_VALID_TIME(PartialOrCompleteTimePeriod.class),
        STATUS(AviationCodeListUser.TAFStatus.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public TAFProperties() {
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
