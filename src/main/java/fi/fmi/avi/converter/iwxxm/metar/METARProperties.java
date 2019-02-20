package fi.fmi.avi.converter.iwxxm.metar;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.model.AviationCodeListUser;

/**
 * Created by rinne on 25/07/2018.
 */
public class METARProperties extends AbstractPropertyContainer {

    public enum Name implements PropertyName {
        STATUS(AviationCodeListUser.MetarStatus.class),//
        SPECI(Boolean.class),//
        AUTOMATED(Boolean.class),//
        OBSERVATION(OMObservationProperties.class),//
        TREND_FORECAST(OMObservationProperties.class),//
        SNOW_CLOSURE(Boolean.class),//
        TREND_NO_SIGNIFICANT_CHANGES(Boolean.class),//
        REPORT_METADATA(GenericReportProperties.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public METARProperties() {
    }
}
