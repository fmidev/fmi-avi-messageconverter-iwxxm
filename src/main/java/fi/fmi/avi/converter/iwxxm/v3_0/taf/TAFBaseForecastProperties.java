package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.List;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;

public class TAFBaseForecastProperties extends AbstractPropertyContainer {
    public TAFBaseForecastProperties() {
    }

    public enum Name implements PropertyName {

        FORECAST(TAFForecastProperties.class),
        TEMPERATURES(List.class),
        MISSING_MESSAGE(Boolean.class);

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
