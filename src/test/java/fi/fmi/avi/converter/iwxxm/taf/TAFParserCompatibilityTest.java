package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static fi.fmi.avi.model.AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN;
import static fi.fmi.avi.model.AviationCodeListUser.RelationalOperator.ABOVE;
import static fi.fmi.avi.model.AviationCodeListUser.RelationalOperator.BELOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INSTANT;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

/** Preserve each version's conversion and validation behavior while sharing its implementation. */
@RunWith(Parameterized.class)
public class TAFParserCompatibilityTest implements IWXXMConverterTests {
    private static AnnotationConfigApplicationContext context;
    private static AviMessageConverter converter;

    private final String resourceDirectory;
    private final ConversionSpecification<String, TAF> stringSpecification;
    private final ConversionSpecification<Document, TAF> domSpecification;

    public TAFParserCompatibilityTest(final String version, final String resourceDirectory,
            final ConversionSpecification<String, TAF> stringSpecification,
            final ConversionSpecification<Document, TAF> domSpecification) {
        this.resourceDirectory = resourceDirectory;
        this.stringSpecification = stringSpecification;
        this.domSpecification = domSpecification;
    }

    @Parameterized.Parameters(name = "IWXXM {0}")
    public static Collection<Object[]> versions() {
        return Arrays.asList(new Object[][] {
                { "3.0", "/fi/fmi/avi/converter/iwxxm/v3_0/", IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO,
                        IWXXMConverter.IWXXM30_DOM_TO_TAF_POJO },
                { "2025-2", "/fi/fmi/avi/converter/iwxxm/v2025_2/taf/", IWXXMConverter.IWXXM2025_2_STRING_TO_TAF_POJO,
                        IWXXMConverter.IWXXM2025_2_DOM_TO_TAF_POJO }
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

    @Test
    public void stringAndDomProduceTheSameModel() throws Exception {
        final TAF fromString = assertThatConversionResult(converter.convertMessage(
                readResourceToString(resourceDirectory + "taf-A5-1.xml"), stringSpecification)).isSuccessful().getMessage();
        final TAF fromDom = assertThatConversionResult(converter.convertMessage(
                readDocumentFromResource(resourceDirectory + "taf-A5-1.xml"), domSpecification)).isSuccessful().getMessage();
        assertThat(fromDom).isEqualTo(fromString);
    }

    @Test
    public void translationTimeHintFillsAnAbsentXmlValue() throws Exception {
        final ZonedDateTime time = ZonedDateTime.parse("2026-01-02T03:04:05Z");
        final TAF taf = assertThatConversionResult(converter.convertMessage(
                readResourceToString(resourceDirectory + "taf-A5-1.xml"), stringSpecification,
                new ConversionHints(ConversionHints.KEY_TRANSLATION_TIME, time))).isSuccessful().getMessage();
        assertThat(taf.getTranslationTime()).contains(time);
    }

    @Test
    public void xmlTranslationTimesTakePrecedenceOverTheHint() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-translated.xml");
        final TAF taf = assertThatConversionResult(converter.convertMessage(xml, domSpecification,
                new ConversionHints(ConversionHints.KEY_TRANSLATION_TIME, ZonedDateTime.parse("2026-01-02T03:04:05Z"))))
                .isSuccessful().getMessage();
        assertThat(taf.getTranslationTime().map(ZonedDateTime::toInstant))
                .contains(ZonedDateTime.parse(xml.getDocumentElement().getAttribute("translationTime")).toInstant());
        assertThat(taf.getTranslatedBulletinReceptionTime().map(ZonedDateTime::toInstant))
                .contains(ZonedDateTime.parse(xml.getDocumentElement().getAttribute("translatedBulletinReceptionTime")).toInstant());
    }

    @Test
    public void automaticTranslationTimeHintUsesTheCurrentTime() throws Exception {
        final ZonedDateTime before = ZonedDateTime.now();
        final TAF taf = assertThatConversionResult(converter.convertMessage(
                readResourceToString(resourceDirectory + "taf-A5-1.xml"), stringSpecification,
                new ConversionHints(ConversionHints.KEY_TRANSLATION_TIME, ConversionHints.VALUE_TRANSLATION_TIME_AUTO)))
                .isSuccessful().getMessage();
        assertThat(taf.getTranslationTime()).map(ZonedDateTime::toInstant).get(INSTANT)
                .isBetween(before.toInstant(), ZonedDateTime.now().toInstant());
    }

    @Test
    public void malformedXmlProducesSyntaxFailure() {
        final ConversionResult<TAF> result = converter.convertMessage("<TAF", stringSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Unable to parse input");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void schemaErrorsPreventModelConstruction() throws Exception {
        final String original = readResourceToString(resourceDirectory + "taf-A5-1.xml");
        final String xml = original.replace("reportStatus=\"NORMAL\"", "");
        assertThat(xml).isNotEqualTo(original);
        final ConversionResult<TAF> result = converter.convertMessage(xml, stringSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "XML Schema validation issue");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void schematronWarningsStillPreventModelConstruction() throws Exception {
        final String original = readResourceToString(resourceDirectory + "taf-A5-1.xml");
        final String xml = original.replace("http://codes.wmo.int/306/4678/TSRA", "http://codes.wmo.int/306/4678/FZRASN");
        assertThat(xml).isNotEqualTo(original);
        final ConversionResult<TAF> result = converter.convertMessage(xml, stringSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "AerodromePresentOrForecastWeather");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void unknownWeatherRetainsScannerError() throws Exception {
        final String original = readResourceToString(resourceDirectory + "taf-A5-1.xml");
        final String xml = original.replace("http://codes.wmo.int/306/4678/TSRA", "http://codes.wmo.int/306/4678/INVALID");
        assertThat(xml).isNotEqualTo(original);
        final ConversionResult<TAF> result = converter.convertMessage(xml, stringSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Illegal weather code");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void unknownIssueTimeRetainsItsSyntaxError() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-1.xml");
        final Element issueTime = (Element) xml.getElementsByTagNameNS(xml.getDocumentElement().getNamespaceURI(), "issueTime").item(0);
        final Element position = (Element) issueTime.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", "timePosition").item(0);
        position.setTextContent("");
        position.setAttribute("indeterminatePosition", "unknown");

        final ConversionResult<TAF> result = converter.convertMessage(xml, domSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Issue time is not valid");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void unknownChangePeriodRetainsItsMissingDataError() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-1.xml");
        final Element changeForecast = (Element) xml.getElementsByTagNameNS(xml.getDocumentElement().getNamespaceURI(), "changeForecast").item(0);
        for (final String position : new String[] { "beginPosition", "endPosition" }) {
            final Element time = (Element) changeForecast.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", position).item(0);
            time.setTextContent("");
            time.setAttribute("indeterminatePosition", "unknown");
        }

        final ConversionResult<TAF> result = converter.convertMessage(xml, domSpecification);
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Valid period is required in change forecasts");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void parsesMeanWindAndGustSpeedOperators() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-1.xml");
        final String namespace = xml.getDocumentElement().getNamespaceURI();
        final Element wind = (Element) xml.getElementsByTagNameNS(namespace, "AerodromeSurfaceWindForecast").item(0);
        final Element meanOperator = xml.createElementNS(namespace, "iwxxm:meanWindSpeedOperator");
        meanOperator.setTextContent("BELOW");
        wind.appendChild(meanOperator);
        final Element gust = xml.createElementNS(namespace, "iwxxm:windGustSpeed");
        gust.setAttribute("uom", "m/s");
        gust.setTextContent("49");
        wind.appendChild(gust);
        final Element gustOperator = xml.createElementNS(namespace, "iwxxm:windGustSpeedOperator");
        gustOperator.setTextContent("ABOVE");
        wind.appendChild(gustOperator);

        final TAF taf = assertThatConversionResult(converter.convertMessage(xml, domSpecification)).isSuccessful().getMessage();

        assertThat(taf.getBaseForecast()).flatMap(TAFForecast::getSurfaceWind).hasValueSatisfying(surfaceWind ->
                assertThat(surfaceWind).extracting(SurfaceWind::getMeanWindSpeedOperator, SurfaceWind::getWindGustOperator)
                        .containsExactly(Optional.of(BELOW), Optional.of(ABOVE)));
    }

    @Test
    public void resolvesReferencedChangeForecastPeriod() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-1.xml");
        final String namespace = xml.getDocumentElement().getNamespaceURI();
        final String gml = "http://www.opengis.net/gml/3.2";
        final Element firstChange = (Element) xml.getElementsByTagNameNS(namespace, "changeForecast").item(0);
        final Element secondChange = (Element) xml.getElementsByTagNameNS(namespace, "changeForecast").item(1);
        final Element referencedPeriod = (Element) firstChange.getElementsByTagNameNS(gml, "TimePeriod").item(0);
        final Element phenomenonTime = (Element) secondChange.getElementsByTagNameNS(namespace, "phenomenonTime").item(0);
        phenomenonTime.removeChild(phenomenonTime.getElementsByTagNameNS(gml, "TimePeriod").item(0));
        phenomenonTime.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", "#" + referencedPeriod.getAttributeNS(gml, "id"));

        final TAF taf = assertThatConversionResult(converter.convertMessage(xml, domSpecification)).isSuccessful().getMessage();

        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class)).element(1)
                .satisfies(change -> {
                    assertThat(change.getPeriodOfChange().getStartTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T06:00Z"));
                    assertThat(change.getPeriodOfChange().getEndTime()).flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                            .contains(ZonedDateTime.parse("2012-08-16T08:00Z"));
                });
    }

    @Test
    public void parsesCloudLayerWithUnknownConvectiveCloudType() throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-1.xml");
        final String namespace = xml.getDocumentElement().getNamespaceURI();
        final Element cloudType = (Element) xml.getElementsByTagNameNS(namespace, "cloudType").item(0);
        cloudType.removeAttributeNS("http://www.w3.org/1999/xlink", "href");
        cloudType.setAttribute("nilReason", CODELIST_VALUE_NIL_REASON_UNKNOWN);

        final TAF taf = assertThatConversionResult(converter.convertMessage(xml, domSpecification)).isSuccessful().getMessage();

        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class)).first()
                .satisfies(change -> assertThat(change.getCloud()).flatMap(CloudForecast::getLayers).get(list(CloudLayer.class))
                        .first().satisfies(layer -> assertThat(layer.getCloudType()).isEmpty()));
    }

    private ConversionResult<TAF> cancellationWithUnknownTimes(final String... positions) throws Exception {
        final Document xml = readDocumentFromResource(resourceDirectory + "taf-A5-2.xml");
        final Element period = (Element) xml.getElementsByTagNameNS(xml.getDocumentElement().getNamespaceURI(), "cancelledReportValidPeriod").item(0);
        for (final String position : positions) {
            final Element time = (Element) period.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", position).item(0);
            time.setTextContent("");
            time.setAttribute("indeterminatePosition", "unknown");
        }
        return converter.convertMessage(xml, domSpecification);
    }

    @Test
    public void unknownCancellationStartRetainsItsMissingDataWarning() throws Exception {
        final ConversionResult<TAF> result = cancellationWithUnknownTimes("beginPosition");
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Cancellation start time is missing.");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void unknownCancellationEndRetainsItsMissingDataWarning() throws Exception {
        final ConversionResult<TAF> result = cancellationWithUnknownTimes("endPosition");
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Cancellation end time is missing.");
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    @Test
    public void unknownCancellationPeriodRetainsItsMissingDataError() throws Exception {
        final ConversionResult<TAF> result = cancellationWithUnknownTimes("beginPosition", "endPosition");
        assertThatConversionResult(result).hasStatus(ConversionResult.Status.FAIL)
                .hasIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Cancellation period is missing.");
        assertThat(result.getConvertedMessage()).isEmpty();
    }
}
