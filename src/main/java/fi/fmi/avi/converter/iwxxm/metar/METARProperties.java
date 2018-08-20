package fi.fmi.avi.converter.iwxxm.metar;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.model.AviationCodeListUser;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;

/**
 * Created by rinne on 25/07/2018.
 */
public class METARProperties extends AbstractPropertyContainer<MeteorologicalAerodromeObservationReportType> {

    public enum Name {
        STATUS(AviationCodeListUser.MetarStatus.class),//
        SPECI(Boolean.class),//
        AUTOMATED(Boolean.class),//
        OBSERVATION(OMObservationProperties.class),//
        TREND_FORECAST(OMObservationProperties.class),//
        SNOW_CLOSURE(Boolean.class),//
        TREND_NO_SIGNIFICANT_CHANGES(Boolean.class),//
        REPORT_METADATA(GenericReportProperties.class);

        private Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public METARProperties(final MeteorologicalAerodromeObservationReportType metar) {
        super(metar);
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
