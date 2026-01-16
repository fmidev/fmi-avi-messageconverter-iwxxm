package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.*;
import fi.fmi.avi.converter.iwxxm.generic.metar.METARSPECIFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.sigmet.SIGMETAIRMETFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.swx.SWXFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.taf.TAFFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.tca.TCAFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.vaa.VAAFieldXPathProvider;
import fi.fmi.avi.model.GenericAviationWeatherMessage.LocationIndicatorType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    private static final GenericIWXXMScanner TAF_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new TAFFieldXPathProvider())
            .requireReportStatus(true)
            .extractValidityTime(true)
            .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
            .build();

    private static final GenericIWXXMScanner METAR_SPECI_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new METARSPECIFieldXPathProvider())
            .requireReportStatus(true)
            .extractValidityTime(false)
            .extractObservationTime(true)
            .locationIndicator(LocationIndicatorType.AERODROME, IWXXMField.AERODROME)
            .build();

    private static final GenericIWXXMScanner SIGMET_AIRMET_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new SIGMETAIRMETFieldXPathProvider())
            .requireReportStatus(true)
            .extractValidityTime(true)
            .locationIndicator(LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE, IWXXMField.ORIGINATING_MWO)
            .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT, IWXXMField.ISSUING_ATS_UNIT)
            .locationIndicator(LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION, IWXXMField.ISSUING_ATS_REGION)
            .build();

    private static final GenericIWXXMScanner SWX_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new SWXFieldXPathProvider())
            .requireReportStatus(true)
            .extractValidityTime(false)
            .locationIndicator(LocationIndicatorType.ISSUING_CENTRE, IWXXMField.ISSUING_CENTRE)
            .build();

    private static final GenericIWXXMScanner VAA_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new VAAFieldXPathProvider())
            .requireReportStatus(false)
            .extractValidityTime(false)
            .locationIndicator(LocationIndicatorType.ISSUING_CENTRE, IWXXMField.ISSUING_CENTRE)
            .build();

    private static final GenericIWXXMScanner TCA_SCANNER = GenericIWXXMScanner.builder()
            .fieldProvider(new TCAFieldXPathProvider())
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
        final Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromDOM(scannersMap);
    }

    @Bean
    public GenericAviationWeatherMessageParser<String> genericAviationWeatherMessageIWXXMStringParser() {
        final Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromString(scannersMap);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMElementParser")
    public GenericAviationWeatherMessageParser<Element> genericAviationWeatherMessageIWXXMElementParser() {
        final Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> scannersMap = genericAviationMessageScannerMap();
        return new GenericAviationWeatherMessageParser.FromElement(scannersMap);
    }

    @Bean
    @Qualifier("genericAviationMessageScannerMap")
    public Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> genericAviationMessageScannerMap() {
        final Map<IWXXMMessageType, GenericAviationWeatherMessageScanner> scannersMap = new EnumMap<>(IWXXMMessageType.class);
        scannersMap.put(IWXXMMessageType.TAF, TAF_SCANNER);
        scannersMap.put(IWXXMMessageType.METAR, METAR_SPECI_SCANNER);
        scannersMap.put(IWXXMMessageType.SPECI, METAR_SPECI_SCANNER);
        scannersMap.put(IWXXMMessageType.SIGMET, SIGMET_AIRMET_SCANNER);
        scannersMap.put(IWXXMMessageType.TROPICAL_CYCLONE_SIGMET, SIGMET_AIRMET_SCANNER);
        scannersMap.put(IWXXMMessageType.VOLCANIC_ASH_SIGMET, SIGMET_AIRMET_SCANNER);
        scannersMap.put(IWXXMMessageType.AIRMET, SIGMET_AIRMET_SCANNER);
        scannersMap.put(IWXXMMessageType.SPACE_WEATHER_ADVISORY, SWX_SCANNER);
        scannersMap.put(IWXXMMessageType.VOLCANIC_ASH_ADVISORY, VAA_SCANNER);
        scannersMap.put(IWXXMMessageType.TROPICAL_CYCLONE_ADVISORY, TCA_SCANNER);
        return Collections.unmodifiableMap(scannersMap);
    }

}
