package fi.fmi.avi.converter.iwxxm.v2025_2.taf;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.time.ZonedDateTime;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMParserTest implements IWXXMConverterTests {

    @Autowired
    private AviMessageConverter converter;

    private TAF parseResource(final String name) throws IOException {
        return assertThatConversionResult(converter.convertMessage(readResourceToString(name), IWXXMConverter.IWXXM2025_2_STRING_TO_TAF_POJO))
                .isSuccessful().getMessage();
    }

    @Test
    public void normalMessageTest() throws IOException {
        assertThat(converter.isSpecificationSupported(IWXXMConverter.IWXXM2025_2_STRING_TO_TAF_POJO)).isTrue();
        final TAF taf = parseResource("taf-reportstatus-normal.xml");

        assertThat(taf.getReportStatus()).isEqualTo(AviationWeatherMessage.ReportStatus.NORMAL);
        assertThat(taf.getPermissibleUsage()).contains(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
        assertThat(taf.getPermissibleUsageReason()).contains(AviationCodeListUser.PermissibleUsageReason.TEST);
        assertThat(taf.getIssueTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .contains(ZonedDateTime.parse("2012-08-15T18:00Z"));
        assertThat(taf.getValidityTime()).hasValueSatisfying(period -> {
            assertThat(period.getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .contains(ZonedDateTime.parse("2012-08-16T00:00Z"));
            assertThat(period.getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .contains(ZonedDateTime.parse("2012-08-16T18:00Z"));
        });

        assertThat(taf.getAerodrome()).satisfies(aerodrome -> {
            assertThat(aerodrome.getDesignator()).isEqualTo("YUDO");
            assertThat(aerodrome.getName()).contains("DONLON/INTERNATIONAL");
            assertThat(aerodrome.getLocationIndicatorICAO()).contains("YUDO");
            assertThat(aerodrome.getReferencePoint()).hasValueSatisfying(point -> {
                assertThat(point.getCrs()).hasValueSatisfying(crs -> {
                    assertThat(crs.getName()).isEqualTo("http://www.opengis.net/def/crs/EPSG/0/4326");
                    assertThat(crs.getAxisLabels()).containsExactly("Lat", "Long");
                    assertThat(crs.getDimension()).contains(2);
                });
                assertThat(point.getElevationUom()).contains("M");
                assertThat(point.getElevationValue()).contains(12d);
                assertThat(point.getVerticalDatum()).contains("EGM_96");
                assertThat(point.getCoordinates()).containsExactly(12.34, -12.34);
            });
        });

        assertThat(taf.getBaseForecast()).hasValueSatisfying(base -> {
            assertThat(base.isCeilingAndVisibilityOk()).isFalse();
            assertThat(base.isNoSignificantWeather()).isTrue();
            assertThat(base.getPrevailingVisibility()).contains(NumericMeasureImpl.of(9000d, "m"));
            assertThat(base.getSurfaceWind()).hasValueSatisfying(wind -> {
                assertThat(wind.isVariableDirection()).isFalse();
                assertThat(wind.getMeanWindDirection()).contains(NumericMeasureImpl.of(130d, "deg"));
                assertThat(wind.getMeanWindSpeed()).isEqualTo(NumericMeasureImpl.of(5d, "m/s"));
            });
            assertThat(base.getCloud()).flatMap(CloudForecast::getLayers).get(list(CloudLayer.class))
                    .singleElement().satisfies(layer -> {
                        assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.BKN);
                        assertThat(layer.getBase()).contains(NumericMeasureImpl.of(2000d, "[ft_i]"));
                    });
        });

        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class))
                .allSatisfy(change -> assertThat(change.isCeilingAndVisibilityOk()).isFalse())
                .satisfiesExactly(change -> {
                    assertThat(change.getChangeIndicator()).isEqualTo(AviationCodeListUser.TAFChangeIndicator.BECOMING);
                    assertThat(change.getPeriodOfChange().getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T06:00Z"));
                    assertThat(change.getPeriodOfChange().getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T08:00Z"));
                    assertThat(change.getCloud()).flatMap(CloudForecast::getLayers).get(list(CloudLayer.class))
                            .satisfiesExactly(layer -> {
                                assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.SCT);
                                assertThat(layer.getBase()).contains(NumericMeasureImpl.of(1500d, "[ft_i]"));
                                assertThat(layer.getCloudType()).contains(AviationCodeListUser.CloudType.CB);
                            }, layer -> {
                                assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.BKN);
                                assertThat(layer.getBase()).contains(NumericMeasureImpl.of(2000d, "[ft_i]"));
                            });
                }, change -> {
                    assertThat(change.getChangeIndicator()).isEqualTo(AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS);
                    assertThat(change.getPeriodOfChange().getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T08:00Z"));
                    assertThat(change.getPeriodOfChange().getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T12:00Z"));
                    assertThat(change.getPrevailingVisibility()).contains(NumericMeasureImpl.of(1000d, "m"));
                    assertThat(change.getForecastWeather()).get(list(Weather.class)).extracting(Weather::getCode).containsExactly("TSRA");
                    assertThat(change.getSurfaceWind()).hasValueSatisfying(wind -> {
                        assertThat(wind.isVariableDirection()).isFalse();
                        assertThat(wind.getMeanWindDirection()).contains(NumericMeasureImpl.of(170d, "deg"));
                        assertThat(wind.getMeanWindSpeed()).isEqualTo(NumericMeasureImpl.of(6d, "m/s"));
                        assertThat(wind.getWindGust()).contains(NumericMeasureImpl.of(12d, "m/s"));
                    });
                    assertThat(change.getCloud()).flatMap(CloudForecast::getLayers).get(list(CloudLayer.class))
                            .satisfiesExactly(layer -> {
                                assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.SCT);
                                assertThat(layer.getBase()).contains(NumericMeasureImpl.of(1000d, "[ft_i]"));
                                assertThat(layer.getCloudType()).contains(AviationCodeListUser.CloudType.CB);
                            }, layer -> {
                                assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.BKN);
                                assertThat(layer.getBase()).contains(NumericMeasureImpl.of(2000d, "[ft_i]"));
                            });
                }, change -> {
                    assertThat(change.getChangeIndicator()).isEqualTo(AviationCodeListUser.TAFChangeIndicator.FROM);
                    assertThat(change.getPeriodOfChange().getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T12:30Z"));
                    assertThat(change.getPeriodOfChange().getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T18:00Z"));
                    assertThat(change.getPrevailingVisibility()).contains(NumericMeasureImpl.of(10000d, "m"));
                    assertThat(change.getPrevailingVisibilityOperator()).contains(AviationCodeListUser.RelationalOperator.ABOVE);
                    assertThat(change.getSurfaceWind()).hasValueSatisfying(wind -> {
                        assertThat(wind.isVariableDirection()).isFalse();
                        assertThat(wind.getMeanWindDirection()).contains(NumericMeasureImpl.of(150d, "deg"));
                        assertThat(wind.getMeanWindSpeed()).isEqualTo(NumericMeasureImpl.of(4d, "m/s"));
                    });
                    assertThat(change.getCloud()).flatMap(CloudForecast::getLayers).get(list(CloudLayer.class))
                            .singleElement().satisfies(layer -> {
                                assertThat(layer.getAmount()).contains(AviationCodeListUser.CloudAmount.BKN);
                                assertThat(layer.getBase()).contains(NumericMeasureImpl.of(2000d, "[ft_i]"));
                            });
                });
    }

    @Test
    public void cancelMessageTest() throws IOException {
        final TAF taf = parseResource("taf-cancel-message.xml");

        assertThat(taf.getReportStatus()).isEqualTo(AviationWeatherMessage.ReportStatus.AMENDMENT);
        assertThat(taf.getPermissibleUsage()).contains(AviationCodeListUser.PermissibleUsage.OPERATIONAL);
        assertThat(taf.isCancelMessage()).isTrue();
        assertThat(taf.getIssueTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                .contains(ZonedDateTime.parse("2012-08-16T15:00Z"));
        assertThat(taf.getAerodrome()).satisfies(aerodrome -> {
            assertThat(aerodrome.getDesignator()).isEqualTo("YUDO");
            assertThat(aerodrome.getName()).contains("DONLON/INTERNATIONAL");
            assertThat(aerodrome.getLocationIndicatorICAO()).contains("YUDO");
        });
        assertThat(taf.getReferredReportValidPeriod()).hasValueSatisfying(period -> {
            assertThat(period.getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .contains(ZonedDateTime.parse("2012-08-16T00:00Z"));
            assertThat(period.getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .contains(ZonedDateTime.parse("2012-08-16T18:00Z"));
        });
    }

    @Test
    public void noSignificantWeatherTest() throws IOException {
        final TAF taf = parseResource("taf-no-significant-weather-or-cloud.xml");

        assertThat(taf.getBaseForecast()).hasValueSatisfying(base ->
                assertThat(base.isNoSignificantWeather()).as("nothingOfOperationalSignificance nilReason").isTrue());
        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class)).hasSize(4)
                .allSatisfy(change -> assertThat(change.isNoSignificantWeather()).isFalse())
                .last().satisfies(change -> assertThat(change.getForecastWeather()).as("missing weather nilReason").isEmpty());
        assertThat(taf.isTranslated()).isFalse();
    }

    @Test
    public void noSignificantCloudTest() throws IOException {
        final TAF taf = parseResource("taf-no-significant-weather-or-cloud.xml");

        assertThat(taf.getBaseForecast()).flatMap(TAFForecast::getCloud).hasValueSatisfying(cloud ->
                assertThat(cloud.isNoSignificantCloud()).as("nothingOfOperationalSignificance nilReason").isTrue());
        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class))
                .satisfiesExactly(change ->
                        assertThat(change.getCloud()).as("omitted cloud").isEmpty(),
                    change -> assertThat(change.getCloud()).hasValueSatisfying(cloud ->
                            assertThat(cloud.isNoSignificantCloud()).isFalse()),
                    change -> assertThat(change.getCloud()).hasValueSatisfying(cloud -> {
                        assertThat(cloud.isVerticalVisibilityMissing()).isFalse();
                        assertThat(cloud.getVerticalVisibility()).contains(NumericMeasureImpl.of(2000d, "[ft_i]"));
                    }),
                    change -> assertThat(change.getCloud()).as("missing cloud nilReason").isEmpty());
        assertThat(taf.isTranslated()).isFalse();
    }

    @Test
    public void translatedMetaPropsTest() throws IOException {
        final TAF taf = parseResource("taf-translated.xml");

        assertThat(taf.isTranslated()).isTrue();
        assertThat(taf.getTranslatedBulletinID()).contains("123456");
        assertThat(taf.getTranslationCentreDesignator()).contains("Translator center");
        assertThat(taf.getTranslationCentreName()).contains("Translator center 1");
        assertThat(taf.getTranslatedBulletinReceptionTime()).contains(ZonedDateTime.parse("2012-08-16T00:00Z[GMT]"));
        assertThat(taf.getTranslationTime()).contains(ZonedDateTime.parse("2012-08-15T00:00Z[GMT]"));
    }

    @Test
    public void CAVOKTrueTest() throws IOException {
        final TAF taf = parseResource("taf-cavok.xml");

        assertThat(taf.getBaseForecast()).hasValueSatisfying(base -> assertThat(base.isCeilingAndVisibilityOk()).isTrue());
        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class))
                .first().satisfies(change -> assertThat(change.isCeilingAndVisibilityOk()).isTrue());
    }
}
