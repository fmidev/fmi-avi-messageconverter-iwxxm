package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import java.util.List;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SurfaceWind;

public class TAFForecastProperties extends AbstractPropertyContainer {
    public TAFForecastProperties() {

    }

    public enum Name implements PropertyName {

        CLOUD_AND_qVISIBILITY_OK(Boolean.class),
        PREVAILING_VISIBILITY(NumericMeasure.class),
        PREVAILING_VISIBILIT_OPERATOR(AviationCodeListUser.RelationalOperator.class),
        SURFACEWIND(SurfaceWind.class),
        FORECAST_WEATHER(List.class),
        NO_SIGNIFICANT_WEATHER(Boolean.class),
        CLOUD_FORECAST(CloudForecast.class);

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
