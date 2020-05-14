package fi.fmi.avi.converter.iwxxm.v21.metar;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.v21.OMObservationProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v21.AbstractIWXXM21Parser;
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
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReportBuilder;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.SeaState;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.WindShear;
import fi.fmi.avi.model.metar.immutable.TrendForecastImpl;
import icao.iwxxm21.MeteorologicalAerodromeObservationReportType;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class MeteorologicalAerodromeObservationReportIWXXMParserBase<T, S extends MeteorologicalTerminalAirReport, B extends MeteorologicalTerminalAirReportBuilder<? extends MeteorologicalTerminalAirReport, B>>
        extends AbstractIWXXM21Parser<T, S> {

    protected abstract B getEmptyBuilder();

    protected B getBuilder(final MeteorologicalAerodromeObservationReportType input, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<? extends MeteorologicalTerminalAirReport> result, final ConversionHints hints) {
        final METARProperties properties = new METARProperties();

        //Collect properties and do detailed validation:
        result.addIssue(IWXXMMETARScanner.collectMETARProperties(input, refCtx, properties, hints));

        //Build the METAR:
        final Optional<AviationCodeListUser.MetarStatus> status = properties.get(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "METAR status not known, unable to proceed"));
            return null;
        }

        final B builder = getEmptyBuilder();
        builder.setStatus(status.get());

        properties.get(METARProperties.Name.AUTOMATED, Boolean.class).ifPresent(builder::setAutomatedStation);
        properties.get(METARProperties.Name.SNOW_CLOSURE, Boolean.class).ifPresent(builder::setSnowClosure);

        final Optional<OMObservationProperties> observationProperties = properties.get(METARProperties.Name.OBSERVATION, OMObservationProperties.class);
        if (observationProperties.isPresent()) {
            if (observationProperties.get().get(OMObservationProperties.Name.AERODROME, Aerodrome.class).isPresent()) {
                builder.setAerodrome(observationProperties.get().get(OMObservationProperties.Name.AERODROME, Aerodrome.class).get());
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No aerodrome info in METAR"));
            }
            Optional<PartialOrCompleteTimeInstant> phenTime = observationProperties.get()
                    .get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTimeInstant.class);

            if (phenTime.isPresent()) {
                builder.setIssueTime(phenTime.get());
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "No phenomenon time given in METAR, unable to parse POJO"));
            }

            if (AviationCodeListUser.MetarStatus.MISSING != status.get()) {
                if (observationProperties.get().get(OMObservationProperties.Name.RESULT, ObservationRecordProperties.class).isPresent()) {
                    final ObservationRecordProperties record = observationProperties.get()
                            .get(OMObservationProperties.Name.RESULT, ObservationRecordProperties.class)
                            .get();
                    record.get(ObservationRecordProperties.Name.SURFACE_WIND, ObservedSurfaceWind.class).ifPresent(builder::setSurfaceWind);
                    record.get(ObservationRecordProperties.Name.VISIBILITY, HorizontalVisibility.class).ifPresent(builder::setVisibility);
                    builder.setRunwayVisualRanges(record.getList(ObservationRecordProperties.Name.RUNWAY_VISUAL_RANGE, RunwayVisualRange.class));
                    builder.setPresentWeather(record.getList(ObservationRecordProperties.Name.PRESENT_WEATHER, Weather.class));
                    record.get(ObservationRecordProperties.Name.CLOUD, ObservedClouds.class).ifPresent(builder::setClouds);
                    record.get(ObservationRecordProperties.Name.AIR_TEMPERATURE, NumericMeasure.class).ifPresent(builder::setAirTemperature);
                    record.get(ObservationRecordProperties.Name.DEWPOINT_TEMPERATURE, NumericMeasure.class).ifPresent(builder::setDewpointTemperature);
                    record.get(ObservationRecordProperties.Name.QNH, NumericMeasure.class).ifPresent(builder::setAltimeterSettingQNH);
                    builder.setRecentWeather(record.getList(ObservationRecordProperties.Name.RECENT_WEATHER, Weather.class));
                    record.get(ObservationRecordProperties.Name.WIND_SHEAR, WindShear.class).ifPresent(builder::setWindShear);
                    record.get(ObservationRecordProperties.Name.SEA_STATE, SeaState.class).ifPresent(builder::setSeaState);
                    builder.setRunwayStates(record.getList(ObservationRecordProperties.Name.RUNWAY_STATE, RunwayState.class));
                    record.get(ObservationRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class).ifPresent(builder::setCeilingAndVisibilityOk);
                }
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No observation in METAR"));
        }

        final Optional<Boolean> noSignificantChanges = properties.get(METARProperties.Name.TREND_NO_SIGNIFICANT_CHANGES, Boolean.class);
        if (noSignificantChanges.isPresent() && noSignificantChanges.get()) {
            builder.setNoSignificantChanges(true);
        } else {
            final List<TrendForecast> trends = new ArrayList<>();
            for (final OMObservationProperties trendProperties : properties.getList(METARProperties.Name.TREND_FORECAST, OMObservationProperties.class)) {
                final TrendForecastImpl.Builder trendBuilder = TrendForecastImpl.builder();
                final Optional<PartialOrCompleteTime> phenTime = trendProperties.get(OMObservationProperties.Name.PHENOMENON_TIME, PartialOrCompleteTime.class);
                if (phenTime.isPresent()) {
                    if (phenTime.get() instanceof PartialOrCompleteTimeInstant) {
                        //AT
                        trendBuilder.setInstantOfChange((PartialOrCompleteTimeInstant) phenTime.get());
                    } else if (phenTime.get() instanceof PartialOrCompleteTimePeriod) {
                        //FM/TL
                        trendBuilder.setPeriodOfChange((PartialOrCompleteTimePeriod) phenTime.get());
                    }
                }
                final Optional<TrendForecastRecordProperties> trendRecord = trendProperties.get(OMObservationProperties.Name.RESULT,
                        TrendForecastRecordProperties.class);
                if (trendRecord.isPresent()) {
                    trendRecord.get()
                            .get(TrendForecastRecordProperties.Name.CHANGE_INDICATOR, AviationCodeListUser.TrendForecastChangeIndicator.class)
                            .ifPresent(trendBuilder::setChangeIndicator);
                    trendRecord.get().get(TrendForecastRecordProperties.Name.SURFACE_WIND, SurfaceWind.class)
                            .ifPresent(trendBuilder::setSurfaceWind);
                    final Optional<Boolean> cavok = trendRecord.get().get(TrendForecastRecordProperties.Name.CLOUD_AND_VISIBILITY_OK, Boolean.class);
                    if (cavok.isPresent() && cavok.get()) {
                        trendBuilder.setCeilingAndVisibilityOk(true);
                    } else {
                        trendRecord.get()
                                .get(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY, NumericMeasure.class)
                                .ifPresent(trendBuilder::setPrevailingVisibility);
                        trendRecord.get()
                                .get(TrendForecastRecordProperties.Name.PREVAILING_VISIBILITY_OPERATOR, AviationCodeListUser.RelationalOperator.class)
                                .ifPresent(trendBuilder::setPrevailingVisibilityOperator);

                        final Optional<Boolean> nsw = trendRecord.get().get(TrendForecastRecordProperties.Name.NO_SIGNIFICANT_WEATHER, Boolean.class);
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
                builder.setTrends(trends);
            }
        }

        properties.get(METARProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent((metaProps) -> {
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class).ifPresent(builder::setPermissibleUsage);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                    .ifPresent(builder::setPermissibleUsageReason);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class).ifPresent(builder::setPermissibleUsageSupplementary);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).ifPresent(builder::setTranslatedBulletinID);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                    .ifPresent(builder::setTranslatedBulletinReceptionTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class).ifPresent(builder::setTranslationCentreDesignator);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).ifPresent(builder::setTranslationCentreName);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).ifPresent(builder::setTranslationTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).ifPresent(builder::setTranslatedTAC);
        });
        return builder;
    }
}
