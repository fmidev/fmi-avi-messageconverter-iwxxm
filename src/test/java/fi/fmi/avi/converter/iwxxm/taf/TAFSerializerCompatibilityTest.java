package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

/** Conversion contracts shared by the two implementations, including their intentional differences. */
@RunWith(Parameterized.class)
public class TAFSerializerCompatibilityTest implements IWXXMConverterTests {
    private static AnnotationConfigApplicationContext context;
    private static AviMessageConverter converter;
    private final String resources;
    private final String namespace;
    private final boolean supportsNil;
    private final ConversionSpecification<String, TAF> parser;
    private final ConversionSpecification<TAF, String> stringSerializer;
    private final ConversionSpecification<TAF, Document> domSerializer;

    public TAFSerializerCompatibilityTest(final String version, final String resources, final String namespace, final boolean supportsNil,
            final ConversionSpecification<String, TAF> parser, final ConversionSpecification<TAF, String> stringSerializer,
            final ConversionSpecification<TAF, Document> domSerializer) {
        this.resources = resources;
        this.namespace = namespace;
        this.supportsNil = supportsNil;
        this.parser = parser;
        this.stringSerializer = stringSerializer;
        this.domSerializer = domSerializer;
    }

    @Parameterized.Parameters(name = "IWXXM {0}")
    public static Collection<Object[]> versions() {
        return Arrays.asList(new Object[][] {
                { "3.0", "/fi/fmi/avi/converter/iwxxm/v3_0/", IWXXM_3_0_NAMESPACE, false,
                        IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM },
                { "2025-2", "/fi/fmi/avi/converter/iwxxm/v2025_2/taf/", IWXXM_2025_2_NAMESPACE, true,
                        IWXXMConverter.IWXXM2025_2_STRING_TO_TAF_POJO, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_STRING,
                        IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_DOM }
        });
    }

    @BeforeClass
    public static void setUpConverter() {
        context = new AnnotationConfigApplicationContext(IWXXMTestConfiguration.class);
        converter = context.getBean(AviMessageConverter.class);
    }

    @AfterClass
    public static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    private TAF source(final String name) throws Exception {
        return assertThatConversionResult(converter.convertMessage(readResourceToString(resources + name), parser)).isSuccessful().getMessage();
    }

    private Document serialize(final TAF input) {
        return assertThatConversionResult(converter.convertMessage(input, domSerializer)).isSuccessful().getMessage();
    }

    private Element baseForecast(final Document xml) {
        return (Element) xml.getElementsByTagNameNS(namespace, "MeteorologicalAerodromeForecast").item(0);
    }

    @Test
    public void stringAndDomHaveTheSameStructureAndPreserveForecasts() throws Exception {
        final TAF input = source("taf-A5-1.xml");
        final String xml = assertThatConversionResult(converter.convertMessage(input, stringSerializer)).isSuccessful().getMessage();
        final StringWriter domXml = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(serialize(input)), new StreamResult(domXml));
        IWXXMConverterTests.assertXMLEqualsIgnoringVariables(xml, domXml.toString());
        assertThat(assertThatConversionResult(converter.convertMessage(xml, parser)).isSuccessful().getMessage()).isEqualTo(input);
    }

    @Test
    public void cancellationPreservesTheReferredValidityPeriod() throws Exception {
        final TAF input = source("taf-A5-2.xml");
        final String xml = assertThatConversionResult(converter.convertMessage(input, stringSerializer)).isSuccessful().getMessage();
        final TAF parsed = assertThatConversionResult(converter.convertMessage(xml, parser)).isSuccessful().getMessage();
        assertThat(parsed.isCancelMessage()).isTrue();
        assertThat(parsed.getReferredReportValidPeriod()).isEqualTo(input.getReferredReportValidPeriod());
        assertThat(parsed.getBaseForecast()).isEmpty();
    }

    @Test
    public void missingUsageDefaultsToNonOperationalTest() throws Exception {
        final TAF input = TAFImpl.Builder.from(source("taf-A5-1.xml")).clearPermissibleUsage()
                .setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.EXERCISE).setPermissibleUsageSupplementary("ignored").build();
        final Element root = serialize(input).getDocumentElement();
        assertThat(root.getAttribute("permissibleUsage")).isEqualTo("NON-OPERATIONAL");
        assertThat(root.getAttribute("permissibleUsageReason")).isEqualTo("TEST");
        assertThat(root.hasAttribute("permissibleUsageSupplementary")).isFalse();
    }

    @Test
    public void explicitWeatherTakesPrecedenceOverNoSignificantWeather() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setForecastWeather(WeatherImpl.fromCodes("RA")).setNoSignificantWeather(true).build()).build();
        final Element base = baseForecast(serialize(input));
        assertThat(base.getElementsByTagNameNS(namespace, "weather").getLength()).isEqualTo(1);
        final Element weather = (Element) base.getElementsByTagNameNS(namespace, "weather").item(0);
        assertThat(weather.getAttributeNS("http://www.w3.org/1999/xlink", "href"))
                .isEqualTo(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER + "RA");
        assertThat(weather.hasAttribute("nilReason")).isFalse();
    }

    @Test
    public void explicitUsageAndTranslationMetadataArePreserved() throws Exception {
        final TAF input = TAFImpl.Builder.from(source("taf-translated.xml"))
                .setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL)
                .setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.EXERCISE).setPermissibleUsageSupplementary("exercise").build();
        final Element root = serialize(input).getDocumentElement();
        assertThat(root.getAttribute("permissibleUsage")).isEqualTo("NON-OPERATIONAL");
        assertThat(root.getAttribute("permissibleUsageReason")).isEqualTo("EXERCISE");
        assertThat(root.getAttribute("permissibleUsageSupplementary")).isEqualTo("exercise");
        assertThat(input.getTranslatedBulletinID()).hasValueSatisfying(expected ->
                assertThat(root.getAttribute("translatedBulletinID")).isEqualTo(expected));
        assertThat(input.getTranslatedBulletinReceptionTime()).hasValueSatisfying(expected ->
                assertThat(ZonedDateTime.parse(root.getAttribute("translatedBulletinReceptionTime"))).isEqualTo(expected));
        assertThat(input.getTranslationCentreDesignator()).hasValueSatisfying(expected ->
                assertThat(root.getAttribute("translationCentreDesignator")).isEqualTo(expected));
        assertThat(input.getTranslationCentreName()).hasValueSatisfying(expected ->
                assertThat(root.getAttribute("translationCentreName")).isEqualTo(expected));
        assertThat(input.getTranslationTime()).hasValueSatisfying(expected ->
                assertThat(ZonedDateTime.parse(root.getAttribute("translationTime"))).isEqualTo(expected));
    }

    @Test
    public void untranslatedMessagesOmitTranslationMetadata() throws Exception {
        final TAF input = TAFImpl.Builder.from(source("taf-translated.xml")).setTranslated(false).setTranslatedTAC("original TAC").build();
        final Element root = serialize(input).getDocumentElement();
        for (final String attribute : new String[] { "translatedBulletinID", "translatedBulletinReceptionTime", "translationCentreDesignator",
                "translationCentreName", "translationTime", "translationFailedTAC" }) {
            assertThat(root.hasAttribute(attribute)).as(attribute).isFalse();
        }
    }

    @Test
    public void cavokOmitsConflictingFieldsAndRetainsAllThreeWarnings() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setCeilingAndVisibilityOk(true).clearPrevailingVisibility().setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.ABOVE)
                .setForecastWeather(WeatherImpl.fromCodes("RA")).build()).build();
        final ConversionResult<Document> result = converter.convertMessage(input, domSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_WARNINGS);
        assertThat(result.getConversionIssues()).extracting(ConversionIssue::getMessage).containsExactly(
                "Visibility included with CAVOK, ignoring", "Weather included with CAVOK, ignoring", "Cloud included with CAVOK, ignoring");
        final Element base = baseForecast(assertThatConversionResult(result).getMessage());
        assertThat(base.getAttribute("cloudAndVisibilityOK")).isEqualTo("true");
        for (final String element : new String[] { "prevailingVisibility", "prevailingVisibilityOperator", "weather", "cloud" }) {
            assertThat(base.getElementsByTagNameNS(namespace, element).getLength()).as(element).isZero();
        }
        assertThat(base.getElementsByTagNameNS(namespace, "surfaceWind").getLength()).isEqualTo(1);
    }

    @Test
    public void baseForecastWithCavokOmitsConflictingVisibility() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setCeilingAndVisibilityOk(true).build()).build();
        final ConversionResult<Document> result = converter.convertMessage(input, domSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_WARNINGS)
                .hasIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL, "Visibility included with CAVOK, ignoring");
        assertThat(result.getConvertedMessage()).hasValueSatisfying(xml ->
                assertThat(baseForecast(xml).getElementsByTagNameNS(namespace, "prevailingVisibility").getLength())
                        .as("prevailingVisibility elements in a CAVOK base forecast").isZero());
        assertThatConversionResult(result).hasNoIssueContaining("MeteorologicalAerodromeForecast-1");
    }

    @Test
    public void noSignificantWeatherAndCloudKeepTheirNilReasons() throws Exception {
        final TAF input = source("taf-no-significant-weather-or-cloud.xml");
        assertThat(input.getBaseForecast()).hasValueSatisfying(base -> {
            assertThat(base.isNoSignificantWeather()).isTrue();
            assertThat(base.getForecastWeather()).isEmpty();
            assertThat(base.getCloud()).hasValueSatisfying(cloud -> assertThat(cloud.isNoSignificantCloud()).isTrue());
        });

        final Element base = baseForecast(serialize(input));
        for (final String name : new String[] { "weather", "cloud" }) {
            final Element element = (Element) base.getElementsByTagNameNS(namespace, name).item(0);
            assertThat(element.getAttribute("nilReason"))
                    .isEqualTo(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
            assertThat(element.hasAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "nil")).isFalse();
        }
        final String xml = assertThatConversionResult(converter.convertMessage(input, stringSerializer)).isSuccessful().getMessage();
        assertThat(assertThatConversionResult(converter.convertMessage(xml, parser)).isSuccessful().getMessage()).isEqualTo(input);
    }

    @Test
    public void weatherExtensionsKeepTheirWarningAndSerializedOutput() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setForecastWeather(WeatherImpl.fromCodes("FZRASN")).build()).build();
        final ConversionResult<String> result = converter.convertMessage(input, stringSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_WARNINGS)
                .hasIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "AerodromePresentOrForecastWeather");
        assertThat(result.getConvertedMessage()).hasValueSatisfying(xml -> assertThat(xml).contains("http://codes.wmo.int/306/4678/FZRASN"));
    }

    @Test
    public void writesMeanWindAndGustSpeedOperators() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final SurfaceWind wind = SurfaceWindImpl.builder().setVariableDirection(true)
                .setMeanWindSpeed(NumericMeasureImpl.of(5d, "m/s"))
                .setMeanWindSpeedOperator(AviationCodeListUser.RelationalOperator.BELOW)
                .setWindGust(NumericMeasureImpl.of(49d, "m/s"))
                .setWindGustOperator(AviationCodeListUser.RelationalOperator.ABOVE).build();
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setSurfaceWind(wind).build()).build();

        final Element base = baseForecast(serialize(input));

        assertThat(base.getElementsByTagNameNS(namespace, "meanWindSpeedOperator").item(0))
                .as("mean wind speed operator").isNotNull().extracting(Node::getTextContent).isEqualTo("BELOW");
        assertThat(base.getElementsByTagNameNS(namespace, "windGustSpeedOperator").item(0))
                .as("wind gust speed operator").isNotNull().extracting(Node::getTextContent).isEqualTo("ABOVE");
    }

    @Test
    public void variableWindGustAndTemperatureExtremesSurviveRoundTrip() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final ZonedDateTime start = original.getValidityTime().get().getStartTime().get().getCompleteTime().get();
        final SurfaceWind wind = SurfaceWindImpl.builder().setVariableDirection(true)
                .setMeanWindSpeed(NumericMeasureImpl.builder().setValue(10.0).setUom("[kn_i]").build())
                .setWindGust(NumericMeasureImpl.builder().setValue(20.0).setUom("[kn_i]").build()).build();
        final TAFAirTemperatureForecast temperature = TAFAirTemperatureForecastImpl.builder()
                .setMaxTemperature(NumericMeasureImpl.builder().setValue(15.0).setUom("Cel").build())
                .setMaxTemperatureTime(PartialOrCompleteTimeInstant.of(start.plusHours(2)))
                .setMinTemperature(NumericMeasureImpl.builder().setValue(-5.0).setUom("Cel").build())
                .setMinTemperatureTime(PartialOrCompleteTimeInstant.of(start.plusHours(4))).build();
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setSurfaceWind(wind).setTemperatures(Collections.singletonList(temperature)).build()).build();
        final String xml = assertThatConversionResult(converter.convertMessage(input, stringSerializer)).isSuccessful().getMessage();
        final TAF parsed = assertThatConversionResult(converter.convertMessage(xml, parser)).isSuccessful().getMessage();
        assertThat(parsed.getBaseForecast()).hasValueSatisfying(base -> {
            assertThat(base.getSurfaceWind()).contains(wind);
            assertThat(base.getTemperatures()).get(list(TAFAirTemperatureForecast.class)).containsExactly(temperature);
        });
    }

    @Test
    public void reportStatusesArePreserved() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        for (final AviationWeatherMessage.ReportStatus status : AviationWeatherMessage.ReportStatus.values()) {
            final TAF input = TAFImpl.Builder.from(original).setReportStatus(status).build();
            assertThat(serialize(input).getDocumentElement().getAttribute("reportStatus")).isEqualTo(status.name());
        }
    }

    @Test
    public void missingIssueTimeStopsConversion() throws Exception {
        final ConversionResult<String> result = converter.convertMessage(TAFImpl.Builder.from(source("taf-A5-1.xml")).clearIssueTime().build(), stringSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Issue time for TAF is missing");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void validityWithoutAnEndStopsConversion() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setValidityTime(PartialOrCompleteTimePeriod.builder()
                .setStartTime(original.getValidityTime().get().getStartTime()).build()).build();
        final ConversionResult<String> result = converter.convertMessage(input, stringSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Validity time for TAF is missing start or end");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void outOfBoundsChangeForecastRetainsBothErrorsAndOutput() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAFChangeForecast change = original.getChangeForecasts().get().get(0);
        final ZonedDateTime start = original.getValidityTime().get().getStartTime().get().getCompleteTime().get().minusHours(1);
        final ZonedDateTime end = original.getValidityTime().get().getEndTime().get().getCompleteTime().get().plusHours(1);
        final TAF input = TAFImpl.Builder.from(original).setChangeForecasts(Collections.singletonList(TAFChangeForecastImpl.Builder.from(change)
                .setPeriodOfChange(PartialOrCompleteTimePeriod.builder().setStartTime(PartialOrCompleteTimeInstant.of(start))
                        .setEndTime(PartialOrCompleteTimeInstant.of(end)).build()).build())).build();
        final ConversionResult<String> result = converter.convertMessage(input, stringSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_ERRORS)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL, "before TAF validity start time")
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.LOGICAL, "after TAF validity end time");
        assertThat(result.getConvertedMessage()).isPresent();
    }

    @Test
    public void changeForecastWithoutAnEndIsReportedAndOmitted() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAFChangeForecast change = original.getChangeForecasts().get().get(0);
        final TAF input = TAFImpl.Builder.from(original).setChangeForecasts(Collections.singletonList(TAFChangeForecastImpl.Builder.from(change)
                .setPeriodOfChange(PartialOrCompleteTimePeriod.builder().setStartTime(change.getPeriodOfChange().getStartTime()).build()).build())).build();
        final ConversionResult<Document> result = converter.convertMessage(input, domSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_ERRORS)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Missing full validity start and/or end times in change forecast");
        assertThat(result.getConvertedMessage()).hasValueSatisfying(xml ->
                assertThat(xml.getElementsByTagNameNS(namespace, "changeForecast").getLength()).isZero());
    }

    @Test
    public void changeForecastWithoutTafValidityIsReportedAndOmitted() throws Exception {
        final TAF input = TAFImpl.Builder.from(source("taf-A5-1.xml")).clearValidityTime().build();
        final ConversionResult<Document> result = converter.convertMessage(input, domSerializer);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_ERRORS)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "TAF validity time is missing complete start and end time");
        assertThat(result.getConvertedMessage()).hasValueSatisfying(xml ->
                assertThat(xml.getElementsByTagNameNS(namespace, "changeForecast").getLength()).isZero());
    }

    @Test
    public void nilReportsRetainTheirVersionSpecificBehavior() throws Exception {
        final TAF input = TAFImpl.Builder.from(source("taf-A5-1.xml")).clearBaseForecast().clearChangeForecasts().clearValidityTime().build();
        final ConversionResult<Document> result = converter.convertMessage(input, domSerializer);
        if (supportsNil) {
            final Element base = (Element) assertThatConversionResult(result).isSuccessful().getMessage().getElementsByTagNameNS(namespace, "baseForecast").item(0);
            assertThat(base.getAttribute("nilReason")).isEqualTo("http://codes.wmo.int/common/nil/missing");
        } else {
            assertThatConversionResult(result).hasStatus(ConversionResult.Status.WITH_ERRORS)
                    .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Base forecast missing for non-cancellation TAF");
            assertThat(result.getConvertedMessage()).hasValueSatisfying(xml ->
                    assertThat(xml.getElementsByTagNameNS(namespace, "baseForecast").getLength()).isZero());
        }
    }

    @Test
    public void missingVerticalVisibilityRetainsItsVersionSpecificRepresentation() throws Exception {
        final TAF original = source("taf-A5-1.xml");
        final TAF input = TAFImpl.Builder.from(original).setBaseForecast(TAFBaseForecastImpl.Builder.from(original.getBaseForecast().get())
                .setCloud(CloudForecastImpl.builder().setVerticalVisibilityMissing(true).build()).build()).build();
        final Document xml = assertThatConversionResult(converter.convertMessage(input, domSerializer)).getMessage();
        if (supportsNil) {
            final Element visibility = (Element) baseForecast(xml).getElementsByTagNameNS(namespace, "verticalVisibility").item(0);
            assertThat(visibility.getAttribute("nilReason")).isEqualTo("http://codes.wmo.int/common/nil/unknown");
            assertThat(visibility.getTextContent()).isEmpty();
        } else {
            assertThat(baseForecast(xml).getElementsByTagNameNS(namespace, "verticalVisibility").getLength()).isZero();
        }
    }
}
