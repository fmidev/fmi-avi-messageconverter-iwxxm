package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.List;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.NumericMeasure;

public class TAFCloudForecastProperties extends AbstractPropertyContainer {



    public enum Name implements PropertyName {

        VERTICAL_VISIBILITY(NumericMeasure.class),
        CLOUD_LAYER(List.class),
        NO_SIGNIFICANT_CLOUD(Boolean.class),
        NO_SIGNIFICANT_VERTICAL_VISIBILITY(Boolean.class);

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
