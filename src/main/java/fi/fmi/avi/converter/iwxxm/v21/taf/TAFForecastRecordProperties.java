package fi.fmi.avi.converter.iwxxm.v21.taf;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;

/**
 * Container class for properties parsed from an IWXXM MeteorologicalAerodromeForecastRecord.
 */
public class TAFForecastRecordProperties extends AbstractPropertyContainer {

    public enum Name implements PropertyName {
        PREVAILING_VISIBILITY(NumericMeasure.class),
        PREVAILING_VISIBILITY_OPERATOR(AviationCodeListUser.RelationalOperator.class), SURFACE_WIND(SurfaceWind.class),
        WEATHER(Weather.class),
        CLOUD(CloudForecast.class),
        TEMPERATURE(TAFAirTemperatureForecast.class),
        CHANGE_INDICATOR(AviationCodeListUser.TAFChangeIndicator.class),
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

    public TAFForecastRecordProperties() {
    }
}
