package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import static fi.fmi.avi.converter.iwxxm.taf.TAFBaseForecastProperties.Name.TEMPERATURES;
import static fi.fmi.avi.converter.iwxxm.taf.TAFForecastProperties.Name.CLOUD_AND_VISIBILITY_OK;
import static fi.fmi.avi.converter.iwxxm.taf.TAFForecastProperties.Name.CLOUD_FORECAST;
import static fi.fmi.avi.converter.iwxxm.taf.TAFForecastProperties.Name.SURFACEWIND;
import static fi.fmi.avi.converter.iwxxm.taf.TAFProperties.Name.AERODROME;
import static fi.fmi.avi.converter.iwxxm.taf.TAFProperties.Name.BASE_FORECAST;
import static fi.fmi.avi.converter.iwxxm.taf.TAFProperties.Name.CHANGE_FORECAST;
import static fi.fmi.avi.converter.iwxxm.taf.TAFProperties.Name.VALID_TIME;

import java.util.ArrayList;
import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.taf.TAFBaseForecastProperties;
import fi.fmi.avi.converter.iwxxm.taf.TAFChangeForecastProperties;
import fi.fmi.avi.converter.iwxxm.taf.TAFCloudForecastProperties;
import fi.fmi.avi.converter.iwxxm.taf.TAFForecastProperties;
import fi.fmi.avi.converter.iwxxm.taf.TAFProperties;
import fi.fmi.avi.converter.iwxxm.taf.TAFScanningSupport;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Scanner;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import icao.iwxxm30.AerodromeAirTemperatureForecastPropertyType;
import icao.iwxxm30.AerodromeAirTemperatureForecastType;
import icao.iwxxm30.AerodromeCloudForecastType;
import icao.iwxxm30.AerodromeSurfaceWindForecastType;
import icao.iwxxm30.CloudLayerPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastPropertyType;
import icao.iwxxm30.MeteorologicalAerodromeForecastType;
import icao.iwxxm30.TAFType;

public class TAFIWXXMScanner extends AbstractIWXXM30Scanner {
    public static List<ConversionIssue> collectTAFProperties(final TAFType input, final ReferredObjectRetrievalContext refCtx, final TAFProperties properties,
            final ConversionHints hints) {
        final IssueList issueList = new IssueList();

        final GenericReportProperties meta = new GenericReportProperties();
        issueList.addAll(AbstractIWXXM30Scanner.collectReportMetadata(input, meta, hints));
        properties.set(TAFProperties.Name.REPORT_METADATA, meta);

        TAFScanningSupport.collectIssueTime(input.getIssueTime(), properties, issueList, refCtx);

        TAFScanningSupport.validPeriod(input.getValidPeriod(), refCtx).ifPresent(period -> properties.set(VALID_TIME, period));

        if (input.getAerodrome() != null) {
            buildAerodrome(input.getAerodrome().getAirportHeliport(), issueList, refCtx)//
                    .ifPresent(aerodrome -> properties.set(AERODROME, aerodrome));
        }

        if (input.getBaseForecast() != null) {
            if (input.getBaseForecast().getMeteorologicalAerodromeForecast() == null) {
                // Leave BASE_FORECAST absent for NIL reports.
                return issueList;
            }
            final TAFBaseForecastProperties baseForecastProperties = new TAFBaseForecastProperties();

            setTAFBaseForecastProperties(baseForecastProperties, input.getBaseForecast().getMeteorologicalAerodromeForecast(), issueList, refCtx, hints);
            properties.set(BASE_FORECAST, baseForecastProperties);
        }

        final List<TAFChangeForecastProperties> changeForecasts = new ArrayList<>();

        for (final MeteorologicalAerodromeForecastPropertyType forecast : input.getChangeForecast()) {
            final TAFChangeForecastProperties changeForecastProperties = new TAFChangeForecastProperties();

            setTAFChangeForecastProperties(changeForecastProperties, forecast.getMeteorologicalAerodromeForecast(), refCtx, issueList, hints);

            changeForecasts.add(changeForecastProperties);
        }
        properties.set(CHANGE_FORECAST, changeForecasts);

        TAFScanningSupport.collectCancellation(input.isIsCancelReport(), input.getCancelledReportValidPeriod(), properties, issueList, refCtx);

        return issueList;
    }

    private static void setTAFBaseForecastProperties(final TAFBaseForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, final ConversionHints hints) {
        if (!input.getTemperature().isEmpty()) {
            props.set(TEMPERATURES, input.getTemperature().stream()//
                    .map(type -> setTemperatures(type, refCtx))//
                    .collect(toImmutableList()));
        }

        final TAFForecastProperties forecastProperties = new TAFForecastProperties();
        setTAFForecastProperties(forecastProperties, input, issueList, refCtx, hints);
        props.set(TAFBaseForecastProperties.Name.FORECAST, forecastProperties);
    }

    private static void setTAFChangeForecastProperties(final TAFChangeForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final ReferredObjectRetrievalContext refCtx, final List<ConversionIssue> issueList, final ConversionHints hints) {
        if (input == null) {
            return;
        }
        TAFScanningSupport.collectChangeForecastProperties(input.getPhenomenonTime(),
                input.getChangeIndicator() == null ? null : input.getChangeIndicator().value(), props, issueList, refCtx);

        final TAFForecastProperties forecastProperties = new TAFForecastProperties();

        setTAFForecastProperties(forecastProperties, input, issueList, refCtx, hints);

        props.set(TAFChangeForecastProperties.Name.FORECAST, forecastProperties);
    }

    private static void setTAFForecastProperties(final TAFForecastProperties props, final MeteorologicalAerodromeForecastType input,
            final List<ConversionIssue> issueList, final ReferredObjectRetrievalContext refCtx, final ConversionHints hints) {

        props.set(CLOUD_AND_VISIBILITY_OK, input.isCloudAndVisibilityOK());

        TAFScanningSupport.collectVisibility(input.getPrevailingVisibility(),
                input.getPrevailingVisibilityOperator() == null ? null : input.getPrevailingVisibilityOperator().value(), props);
        TAFScanningSupport.collectWeather(input.getWeather(), props, hints, issueList);

        if (input.getSurfaceWind() != null) {
            props.set(SURFACEWIND, setSurfaceWind(input.getSurfaceWind().getAerodromeSurfaceWindForecast()));
        }

        if (input.getCloud() != null && hasNothingOfOperationalSignificance(input.getCloud().getNilReason())) {
            final TAFCloudForecastProperties prop = new TAFCloudForecastProperties();
            prop.set(TAFCloudForecastProperties.Name.NO_SIGNIFICANT_CLOUD, true);
            props.set(CLOUD_FORECAST, prop);
        } else if (input.getCloud() != null && input.getCloud().getNilReason().isEmpty() && input.getCloud().getAerodromeCloudForecast() != null) {
            props.set(CLOUD_FORECAST, setCloudForecast(input.getCloud().getAerodromeCloudForecast(), issueList));
        }
    }

    private static TAFAirTemperatureForecast setTemperatures(final AerodromeAirTemperatureForecastPropertyType input,
            final ReferredObjectRetrievalContext refCtx) {
        final AerodromeAirTemperatureForecastType forecast = input.getAerodromeAirTemperatureForecast();
        return TAFScanningSupport.temperatures(forecast.getMaximumAirTemperature(), forecast.getMaximumAirTemperatureTime(),
                forecast.getMinimumAirTemperature(), forecast.getMinimumAirTemperatureTime(), refCtx);
    }

    private static SurfaceWind setSurfaceWind(final AerodromeSurfaceWindForecastType forecast) {
        return TAFScanningSupport.surfaceWind(forecast.isVariableWindDirection(), forecast.getMeanWindDirection(), forecast.getMeanWindSpeed(),
                forecast.getMeanWindSpeedOperator() == null ? null : forecast.getMeanWindSpeedOperator().value(), forecast.getWindGustSpeed(),
                forecast.getWindGustSpeedOperator() == null ? null : forecast.getWindGustSpeedOperator().value());
    }

    private static TAFCloudForecastProperties setCloudForecast(final AerodromeCloudForecastType input, final List<ConversionIssue> issueList) {
        final TAFCloudForecastProperties prop = new TAFCloudForecastProperties();

        if (input.getVerticalVisibility() == null //
                || input.getVerticalVisibility().isNil() //
                || !input.getVerticalVisibility().getValue().getNilReason().isEmpty()) {
            prop.set(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY_MISSING, true);
        } else if (input.getVerticalVisibility() != null) {
            asNumericMeasure(input.getVerticalVisibility().getValue())//
                    .ifPresent(nm -> prop.set(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY, nm));
        }
        prop.set(TAFCloudForecastProperties.Name.CLOUD_LAYER, setCloudLayers(input.getLayer(), issueList));

        if (!prop.contains(TAFCloudForecastProperties.Name.VERTICAL_VISIBILITY) && prop.getList(TAFCloudForecastProperties.Name.CLOUD_LAYER, CloudLayer.class)
                .isEmpty()) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No vertical visibility or cloud layers in forecast"));
        }
        return prop;
    }

    private static List<CloudLayer> setCloudLayers(final List<CloudLayerPropertyType> input, final List<ConversionIssue> issues) {
        return input.stream().map(CloudLayerPropertyType::getCloudLayer)
                .map(layer -> TAFScanningSupport.cloudLayer(layer.getAmount(), layer.getBase(),
                        layer.getCloudType() == null || layer.getCloudType().isNil() ? null : layer.getCloudType().getValue(), issues))
                .collect(toImmutableList());
    }

}
