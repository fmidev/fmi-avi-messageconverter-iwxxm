package fi.fmi.avi.converter.iwxxm.v2025_2.taf;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.converter.iwxxm.IWXXMTestConfiguration;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import icao.iwxxm2025_2.TAFType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static fi.fmi.avi.converter.iwxxm.ConversionResultAssertion.assertThatConversionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IWXXMTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFIWXXMConversionTest implements IWXXMConverterTests {
    @Autowired
    private AviMessageConverter converter;

    private TAF parse(final String xml) {
        return assertThatConversionResult(converter.convertMessage(xml, IWXXMConverter.IWXXM2025_2_STRING_TO_TAF_POJO))
                .isSuccessful().getMessage();
    }

    @Test
    public void omittedCavokIsFalseInBaseAndChangeForecasts() throws Exception {
        final String xml = readResourceToString("taf-cavok-omitted.xml");
        assertThat(xml).doesNotContain("cloudAndVisibilityOK");
        final TAF taf = parse(xml);
        assertThat(taf.getBaseForecast()).hasValueSatisfying(base -> assertThat(base.isCeilingAndVisibilityOk()).isFalse());
        assertThat(taf.getChangeForecasts()).get(list(TAFChangeForecast.class)).hasSize(3)
                .allSatisfy(forecast -> assertThat(forecast.isCeilingAndVisibilityOk()).isFalse());
    }

    @Test
    public void domRoundTripPreservesFullAixmAerodromeAndForecasts() throws Exception {
        final TAF source = parse(readResourceToString("taf-A5-1.xml"));
        final TAF input = TAFImpl.Builder.from(source)
                .setAerodrome(AerodromeImpl.immutableCopyOf(source.getAerodrome()).toBuilder()
                        .setDesignatorIATA("DON").setFieldElevationValue(123.45).setFieldElevationUom("FT").build())
                .build();
        final Document xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_DOM))
                .isSuccessful().getMessage();
        assertThat(xml.getDocumentElement().getNamespaceURI()).isEqualTo(IWXXM_2025_2_NAMESPACE);
        assertThat(xml.getDocumentElement().getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation"))
                .contains("http://schemas.wmo.int/iwxxm/2025-2/iwxxm.xsd");
        final TAF parsed = assertThatConversionResult(converter.convertMessage(xml, IWXXMConverter.IWXXM2025_2_DOM_TO_TAF_POJO))
                .isSuccessful().getMessage();
        assertThat(parsed).isEqualTo(input);
    }

    @Test
    public void wellDevelopedFunnelCloudSurvivesStringRoundTrip() throws Exception {
        final TAF source = parse(readResourceToString("taf-A5-1.xml"));
        final TAF input = TAFImpl.Builder.from(source).setBaseForecast(TAFBaseForecastImpl.Builder.from(source.getBaseForecast().get())
                .setForecastWeather(WeatherImpl.fromCodes("+FC")).build()).build();
        final String xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_STRING))
                .isSuccessful().getMessage();
        assertThat(xml).contains("http://codes.wmo.int/306/4678/+FC");
        assertThat(parse(xml).getBaseForecast()).flatMap(TAFForecast::getForecastWeather).get(list(Weather.class))
                .extracting(Weather::getCode).containsExactly("+FC");
    }

    @Test
    public void amendmentAndCorrectionStatusesSurviveRoundTrip() throws Exception {
        for (final AviationWeatherMessage.ReportStatus status : new AviationWeatherMessage.ReportStatus[]{
                AviationWeatherMessage.ReportStatus.AMENDMENT, AviationWeatherMessage.ReportStatus.CORRECTION}) {
            final TAF input = TAFImpl.Builder.from(parse(readResourceToString("taf-A5-1.xml"))).setReportStatus(status).build();
            final String xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_STRING))
                    .isSuccessful().getMessage();
            assertThat(parse(xml).getReportStatus()).isEqualTo(status);
        }
    }

    @Test
    public void bulletinStringAndDomRoundTripsIncludeCancellation() throws Exception {
        final TAF normal = parse(readResourceToString("taf-A5-1.xml"));
        final TAF cancellation = parse(readResourceToString("taf-A5-2.xml"));
        final TAFBulletin input = TAFBulletinImpl.builder()
                .setHeading(BulletinHeadingImpl.builder().setGeographicalDesignator("FI").setLocationIndicator("EFKL")
                        .setBulletinNumber(31).setIssueTime(normal.getIssueTime().get())
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)
                        .setType(BulletinHeading.Type.NORMAL).build())
                .addMessages(normal, cancellation).build();
        final String xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_IWXXM2025_2_STRING))
                .isSuccessful().getMessage();
        final TAFBulletin parsed = assertThatConversionResult(converter.convertMessage(xml, IWXXMConverter.WMO_COLLECT_IWXXM2025_2_STRING_TO_TAF_BULLETIN_POJO))
                .isSuccessful().getMessage();
        assertThat(parsed.getMessages()).containsExactly(normal, cancellation);
        final Document dom = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_IWXXM2025_2_DOM))
                .isSuccessful().getMessage();
        assertThat(dom.getElementsByTagNameNS(IWXXM_2025_2_NAMESPACE, "TAF").getLength()).isEqualTo(2);
        final TAFBulletin parsedDom = assertThatConversionResult(converter.convertMessage(dom, IWXXMConverter.WMO_COLLECT_IWXXM2025_2_DOM_TO_TAF_BULLETIN_POJO))
                .isSuccessful().getMessage();
        assertThat(parsedDom.getMessages()).containsExactly(normal, cancellation);
    }

    @Test
    public void missingVerticalVisibilitySurvivesRoundTrip() throws Exception {
        final TAF source = parse(readResourceToString("taf-A5-1.xml"));
        final TAF input = TAFImpl.Builder.from(source).setBaseForecast(TAFBaseForecastImpl.Builder.from(source.getBaseForecast().get())
                .setCloud(CloudForecastImpl.builder().setVerticalVisibilityMissing(true).build()).build()).build();
        final String xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_STRING))
                .isSuccessful().getMessage();
        assertThat(parse(xml).getBaseForecast()).flatMap(TAFForecast::getCloud)
                .hasValueSatisfying(cloud -> assertThat(cloud.isVerticalVisibilityMissing()).isTrue());
    }

    @Test
    public void nilReportSurvivesRoundTrip() throws Exception {
        final TAF input = TAFImpl.Builder.from(parse(readResourceToString("taf-A5-1.xml")))
                .clearBaseForecast().clearChangeForecasts().clearValidityTime().build();
        final String xml = assertThatConversionResult(converter.convertMessage(input, IWXXMConverter.TAF_POJO_TO_IWXXM2025_2_STRING))
                .isSuccessful().getMessage();
        final TAF parsed = parse(xml);
        assertThat(parsed.isMissingMessage()).isTrue();
        assertThat(parsed.getValidityTime()).isEmpty();
    }

    @Test
    public void everyOfficialWeatherCodeIsSupported() throws Exception {
        final List<String> codes = new ArrayList<>();
        try (final InputStream input = TAFType.class.getResourceAsStream("/int/icao/iwxxm/2025_2/rule/codes.wmo.int-49-2-AerodromePresentOrForecastWeather.rdf")) {
            final Document rdf = IWXXMConverterTests.readDocument(input);
            final NodeList concepts = rdf.getElementsByTagNameNS("http://www.w3.org/2004/02/skos/core#", "Concept");
            for (int i = 0; i < concepts.getLength(); i++) {
                final Element concept = (Element) concepts.item(i);
                final NodeList versions = concept.getElementsByTagNameNS("http://www.w3.org/2002/07/owl#", "versionInfo");
                for (int v = 0; v < versions.getLength(); v++) {
                    if (IWXXM_2025_2_NAMESPACE.equals(((Element) versions.item(v)).getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource"))) {
                        codes.add(concept.getElementsByTagNameNS("http://www.w3.org/2004/02/skos/core#", "notation").item(0).getTextContent());
                        break;
                    }
                }
            }
        }
        assertThat(codes).isNotEmpty().contains("+FC");
        assertThat(WeatherImpl.WEATHER_CODES.keySet()).containsAll(codes);
    }
}