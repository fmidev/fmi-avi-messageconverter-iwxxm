package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import icao.iwxxm21.MeteorologicalAerodromeForecastRecordType;

/**
 * Container class for properties parsed from an IWXXM MeteorologicalAerodromeForecastRecord.
 */
public class TAFForecastRecordProperties extends AbstractPropertyContainer<MeteorologicalAerodromeForecastRecordType> {

    public enum Name {
        PREVAILING_VISIBILITY(NumericMeasure.class),
        PREVAILING_VISIBILITY_OPERATOR(AviationCodeListUser.RelationalOperator.class), SURFACE_WIND(SurfaceWind.class),
        WEATHER(Weather.class),
        CLOUD(CloudForecast.class),
        TEMPERATURE(TAFAirTemperatureForecast.class),
        CHANGE_INDICATOR(AviationCodeListUser.TAFChangeIndicator.class),
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

    public TAFForecastRecordProperties(final MeteorologicalAerodromeForecastRecordType record) {
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
