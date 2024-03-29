package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.List;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class TAFProperties extends AbstractPropertyContainer {

    public TAFProperties() {
    }

    public enum Name implements PropertyName {

        ISSUE_TIME(PartialOrCompleteTimeInstant.class),
        VALID_TIME(PartialOrCompleteTimePeriod.class),
        IS_CANCEL_MESSAGE(Boolean.class),
        REPORT_METADATA(GenericReportProperties.class),
        BASE_FORECAST(TAFBaseForecastProperties.class),
        CHANGE_FORECAST(List.class),
        MISSING_MESSAGE(Boolean.class),
        AERODROME(Aerodrome.class);

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
