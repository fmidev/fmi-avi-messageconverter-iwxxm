package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class TAFChangeForecastProperties extends AbstractPropertyContainer {
    public TAFChangeForecastProperties() {
    }

    public enum Name implements PropertyName {

        FORECAST(TAFForecastProperties.class),
        CHANGE_INDICATOR(AviationCodeListUser.TAFChangeIndicator.class),
        CHANGE_PERIOD(PartialOrCompleteTimePeriod.class);

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
