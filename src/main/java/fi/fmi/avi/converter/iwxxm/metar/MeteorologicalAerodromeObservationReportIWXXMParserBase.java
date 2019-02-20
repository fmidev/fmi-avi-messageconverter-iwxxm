package fi.fmi.avi.converter.iwxxm.metar;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractJAXBIWXXMParser;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.metar.HorizontalVisibility;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.SeaState;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.WindShear;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.metar.immutable.TrendForecastImpl;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class MeteorologicalAerodromeObservationReportIWXXMParserBase<T, S extends MeteorologicalTerminalAirReport>
        extends AbstractJAXBIWXXMParser<T, S> {

    protected METARImpl.Builder getBuilder(final MeteorologicalAerodromeObservationReportType input, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<? extends MeteorologicalTerminalAirReport> result, final ConversionHints hints) {
        METARProperties properties = new METARProperties();

        //Collect properties and do detailed validation:
        result.addIssue(IWXXMMETARScanner.collectMETARProperties(input, refCtx, properties, hints));

        //Build the METAR:
        Optional<AviationCodeListUser.MetarStatus> status = properties.get(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "METAR status not known, unable to proceed"));
            return null;
        }
        METARImpl.Builder metarBuilder = new METARImpl.Builder();
        metarBuilder.setStatus(status.get());

        properties.get(METARProperties.Name.AUTOMATED, Boolean.class).ifPresent(metarBuilder::setAutomatedStation);
        properties.get(METARProperties.Name.SNOW_CLOSURE, Boolean.class).ifPresent(metarBuilder::setSnowClosure);

        Optional<OMObservationProperties> observationProperties = properties.get(METARProperties.Name.OBSERVATION, OMObservationProperties.class);
        if (observationProperties.isPresent()) {
            if (observationProperties.get().get(OMObservationProperties.Name.AERODROME, Aerodrome.class).isPresent()) {
                metarBuilder.setAerodrome(observationProperties.get().get(OMObservationProperties.Name.AERODROME, Aerodrome.class).get());
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No aerodrome info in METAR"));
            }

            observationProperties.get()
                    .get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimeInstant.class)
                    .ifPresent(metarBuilder::setIssueTime);

            if (AviationCodeListUser.MetarStatus.MISSING != status.get()) {
                if (observationProperties.get().get(OMObservationProperties.Name.RESULT, ObservationRecordProperties.class).isPresent()) {
                    ObservationRecordProperties record = observationProperties.get()
                            .get(OMObservationProperties.Name.RESULT, ObservationRecordProperties.class)
                            .get();
                    record.get(ObservationRecordProperties.Name.SURFACE_WIND, ObservedSurfaceWind.class).ifPresent(metarBuilder::setSurfaceWind);
                    record.get(ObservationRecordProperties.Name.VISIBILITY, HorizontalVisibility.class).ifPresent(metarBuilder::setVisibility);
                    metarBuilder.setRunwayVisualRanges(record.getList(ObservationRecordProperties.Name.RUNWAY_VISUAL_RANGE, RunwayVisualRange.class));
                    metarBuilder.setPresentWeather(record.getList(ObservationRecordProperties.Name.PRESENT_WEATHER, Weather.class));
                    record.get(ObservationRecordProperties.Name.CLOUD, ObservedClouds.class).ifPresent(metarBuilder::setClouds);
                    record.get(ObservationRecordProperties.Name.AIR_TEMPERATURE, NumericMeasure.class).ifPresent(metarBuilder::setAirTemperature);
                    record.get(ObservationRecordProperties.Name.DEWPOINT_TEMPERATURE, NumericMeasure.class).ifPresent(metarBuilder::setDewpointTemperature);
                    record.get(ObservationRecordProperties.Name.QNH, NumericMeasure.class).ifPresent(metarBuilder::setAltimeterSettingQNH);
                    metarBuilder.setRecentWeather(record.getList(ObservationRecordProperties.Name.RECENT_WEATHER, Weather.class));
                    record.get(ObservationRecordProperties.Name.WIND_SHEAR, WindShear.class).ifPresent(metarBuilder::setWindShear);
                    record.get(ObservationRecordProperties.Name.SEA_STATE, SeaState.class).ifPresent(metarBuilder::setSeaState);
                    metarBuilder.setRunwayStates(record.getList(ObservationRecordProperties.Name.RUNWAY_STATE, RunwayState.class));
                    record.get(ObservationRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class).ifPresent(metarBuilder::setCeilingAndVisibilityOk);
                }
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation in METAR"));
        }

        Optional<Boolean> noSignificantChanges = properties.get(METARProperties.Name.TREND_NO_SIGNIFICANT_CHANGES, Boolean.class);
        if (noSignificantChanges.isPresent() && noSignificantChanges.get()) {
            metarBuilder.setNoSignificantChanges(true);
        } else {
            List<TrendForecast> trends = new ArrayList<>();
            for (OMObservationProperties trendProperties : properties.getList(METARProperties.Name.TREND_FORECAST, OMObservationProperties.class)) {
                TrendForecastImpl.Builder trendBuilder = new TrendForecastImpl.Builder();
                Optional<PartialOrCompleteTime> phenTime = trendProperties.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTime.class);
                if (phenTime.isPresent()) {
                    if (phenTime.get() instanceof PartialOrCompleteTimeInstant) {
                        //AT
                        trendBuilder.setInstantOfChange((PartialOrCompleteTimeInstant) phenTime.get());
                    } else if (phenTime.get() instanceof PartialOrCompleteTimePeriod) {
                        //FM/TL
                        trendBuilder.setPeriodOfChange((PartialOrCompleteTimePeriod) phenTime.get());
                    }
                }
                Optional<TrendForecastRecordProperties> trendRecord = trendProperties.get(OMObservationProperties.Name.RESULT,
                        TrendForecastRecordProperties.class);
                if (trendRecord.isPresent()) {
                    trendRecord.get()
                            .get(TrendForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TrendForecastChangeIndicator.class)
                            .ifPresent(trendBuilder::setChangeIndicator);
                    trendRecord.get().get(TrendForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class)
                            .ifPresent(trendBuilder::setSurfaceWind);
                    Optional<Boolean> cavok = trendRecord.get().get(TrendForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
                    if (cavok.isPresent() && cavok.get()) {
                        trendBuilder.setCeilingAndVisibilityOk(true);
                    } else {
                        trendRecord.get()
                                .get(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class)
                                .ifPresent(trendBuilder::setPrevailingVisibility);
                        trendRecord.get()
                                .get(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                                .ifPresent(trendBuilder::setPrevailingVisibilityOperator);

                        Optional<Boolean> nsw = trendRecord.get().get(TrendForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
                        if (nsw.isPresent() && nsw.get()) {
                            trendBuilder.setNoSignificantWeather(true);
                        } else {
                            trendBuilder.setForecastWeather(trendRecord.get().getList(TrendForecastRecordProperties.Name.FORECAST_WEATHER, Weather.class));
                        }
                        trendRecord.get().get(TrendForecastRecordProperties.Name.CLOUD, CloudForecast.class).ifPresent(trendBuilder::setCloud);
                    }
                }
                trends.add(trendBuilder.build());
            }
            if (!trends.isEmpty()) {
                metarBuilder.setTrends(trends);
            }
        }

        properties.get(METARProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent((metaProps) -> {
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class)
                    .ifPresent(metarBuilder::setPermissibleUsage);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                    .ifPresent(metarBuilder::setPermissibleUsageReason);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class).ifPresent(metarBuilder::setPermissibleUsageSupplementary);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).ifPresent(metarBuilder::setTranslatedBulletinID);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                    .ifPresent(metarBuilder::setTranslatedBulletinReceptionTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class).ifPresent(metarBuilder::setTranslationCentreDesignator);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).ifPresent(metarBuilder::setTranslationCentreName);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).ifPresent(metarBuilder::setTranslationTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).ifPresent(metarBuilder::setTranslatedTAC);
        });
        return metarBuilder;
    }
}
