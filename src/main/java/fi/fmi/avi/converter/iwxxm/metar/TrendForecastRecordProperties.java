package fi.fmi.avi.converter.iwxxm.metar;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import icao.iwxxm21.MeteorologicalAerodromeTrendForecastRecordType;

/**
 * Created by rinne on 25/07/2018.
 */
public class TrendForecastRecordProperties extends AbstractPropertyContainer<MeteorologicalAerodromeTrendForecastRecordType> {

    public enum Name {
        SURFACE_WIND(SurfaceWind.class),
        PREVAILING_VISIBILITY(NumericMeasure.class),
        PREVAILING_VISIBILITY_OPERATOR(AviationCodeListUser.RelationalOperator.class),
        FORECAST_WEATHER(Weather.class),
        CLOUD(CloudForecast.class),
        CHANGE_INDICATOR(AviationCodeListUser.TrendForecastChangeIndicator.class),
        CLOUD_AND_VISIBILITY_OK(Boolean.class),
        NO_SIGNIFICANT_WEATHER(Boolean.class);

        private Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public TrendForecastRecordProperties(final MeteorologicalAerodromeTrendForecastRecordType record) {
        super(record);
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
