package fi.fmi.avi.converter.iwxxm.v2_1.metar;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;

/**
 * Created by rinne on 25/07/2018.
 */
public class TrendForecastRecordProperties extends AbstractPropertyContainer {

    public enum Name implements PropertyName {
        SURFACE_WIND(SurfaceWind.class),
        PREVAILING_VISIBILITY(NumericMeasure.class),
        PREVAILING_VISIBILITY_OPERATOR(AviationCodeListUser.RelationalOperator.class),
        FORECAST_WEATHER(Weather.class),
        CLOUD(CloudForecast.class),
        CHANGE_INDICATOR(AviationCodeListUser.TrendForecastChangeIndicator.class),
        CLOUD_AND_VISIBILITY_OK(Boolean.class),
        NO_SIGNIFICANT_WEATHER(Boolean.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public TrendForecastRecordProperties() {
    }
}
