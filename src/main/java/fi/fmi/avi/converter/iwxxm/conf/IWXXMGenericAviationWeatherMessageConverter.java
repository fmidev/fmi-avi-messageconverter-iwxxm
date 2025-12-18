package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.*;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser.ScannerKey;
import fi.fmi.avi.converter.iwxxm.generic.metar.METARSPECIFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.sigmet.SIGMETAIRMETFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.swx.SWXFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.taf.TAFFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.tca.TCAFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.vaa.VAAFieldXPathProvider;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    /**
     * Resolves METAR or SPECI message type from element local name.
     * <p>
     * Note: old Spring version, can't use lambdas.
     * </p>
     */
    private static final Function<Element, MessageType> METAR_SPECI_TYPE_RESOLVER = new Function<Element, MessageType>() {
        @Override
        public MessageType apply(final Element element) {
            final String name = element.getLocalName();
            if ("METAR".equals(name)) {
                return MessageType.METAR;
            }
            if ("SPECI".equals(name)) {
                return MessageType.SPECI;
            }
            return null;
        }
    };

    /**
     * Resolves SIGMET or AIRMET message type from element local name.
     * <p>
     * Note: old Spring version, can't use lambdas.
     * </p>
     */
    private static final Function<Element, MessageType> SIGMET_AIRMET_TYPE_RESOLVER = new Function<Element, MessageType>() {
        @Override
        public MessageType apply(final Element element) {
            final String name = element.getLocalName();
            if ("AIRMET".equals(name)) {
                return MessageType.AIRMET;
            }
            return MessageType.SIGMET; // SIGMET, VolcanicAshSIGMET, TropicalCycloneSIGMET
        }
    };

    private static final GenericIWXXMScanner TAF_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new TAFFieldXPathProvider())
            .messageType(MessageType.TAF)
            .requireReportStatus(true)
            .extractValidityTime(true)
            .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
            .build();

    private static final GenericIWXXMScanner METAR_SPECI_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new METARSPECIFieldXPathProvider())
            .messageTypeResolver(METAR_SPECI_TYPE_RESOLVER)
            .requireReportStatus(true)
            .extractValidityTime(false)
            .extractObservationTime(true)
            .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
            .build();

    private static final GenericIWXXMScanner SIGMET_AIRMET_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new SIGMETAIRMETFieldXPathProvider())
            .messageTypeResolver(SIGMET_AIRMET_TYPE_RESOLVER)
            .requireReportStatus(true)
            .extractValidityTime(true)
            .locationIndicator(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, IWXXMField.ORIGINATING_MWO)
            .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, IWXXMField.ISSUING_ATS_UNIT)
            .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, IWXXMField.ISSUING_ATS_REGION)
            .build();

    private static final GenericIWXXMScanner SWX_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new SWXFieldXPathProvider())
            .messageType(MessageType.SPACE_WEATHER_ADVISORY)
            .requireReportStatus(true)
            .extractValidityTime(false)
            .locationIndicator(LocationIndicatorType.ISSUING_CENTRE, IWXXMField.ISSUING_CENTRE)
            .build();

    private static final GenericIWXXMScanner VAA_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new VAAFieldXPathProvider())
            .messageType(MessageType.VOLCANIC_ASH_ADVISORY)
            .requireReportStatus(false)
            .extractValidityTime(false)
            .locationIndicator(LocationIndicatorType.ISSUING_CENTRE, IWXXMField.ISSUING_CENTRE)
            .build();

    private static final GenericIWXXMScanner TCA_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new TCAFieldXPathProvider())
            .messageType(MessageType.TROPICAL_CYCLONE_ADVISORY)
            .requireReportStatus(false)
            .extractValidityTime(false)
            .locationIndicator(LocationIndicatorType.ISSUING_CENTRE, IWXXMField.ISSUING_CENTRE)
            .build();

    @Bean
    public IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner(
            @Qualifier("genericAviationWeatherMessageIWXXMElementParser") final GenericAviationWeatherMessageParser<Element> messageParser) {
        return new IWXXMGenericBulletinScanner(messageParser);
    }

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser(
            final IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromDOM(iwxxmGenericBulletinScanner);
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser(
            final IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromString(iwxxmGenericBulletinScanner);
    }

    @Bean
    public GenericAviationWeatherMessageParser<Document> genericAviationWeatherMessageIWXXMDOMParser() {
        final Map<ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromDOM(scannersMap);
    }

    @Bean
    public GenericAviationWeatherMessageParser<String> genericAviationWeatherMessageIWXXMStringParser() {
        final Map<ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromString(scannersMap);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMElementParser")
    public GenericAviationWeatherMessageParser<Element> genericAviationWeatherMessageIWXXMElementParser() {
        final Map<ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromElement(scannersMap);
    }

    @Bean
    @Qualifier("genericAviationMessageScannerMap")
    public Map<ScannerKey, GenericAviationWeatherMessageScanner> genericAviationMessageScannerMap() {
        final Map<ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = new HashMap<>();
        scannersMap.put(new ScannerKey("TAF"), TAF_SCANNER);
        scannersMap.put(new ScannerKey("METAR"), METAR_SPECI_SCANNER);
        scannersMap.put(new ScannerKey("SPECI"), METAR_SPECI_SCANNER);
        scannersMap.put(new ScannerKey("SIGMET"), SIGMET_AIRMET_SCANNER);
        scannersMap.put(new ScannerKey("TropicalCycloneSIGMET"), SIGMET_AIRMET_SCANNER);
        scannersMap.put(new ScannerKey("VolcanicAshSIGMET"), SIGMET_AIRMET_SCANNER);
        scannersMap.put(new ScannerKey("AIRMET"), SIGMET_AIRMET_SCANNER);
        scannersMap.put(new ScannerKey("SpaceWeatherAdvisory"), SWX_SCANNER);
        scannersMap.put(new ScannerKey("VolcanicAshAdvisory"), VAA_SCANNER);
        scannersMap.put(new ScannerKey("TropicalCycloneAdvisory"), TCA_SCANNER);
        return scannersMap;
    }

}
