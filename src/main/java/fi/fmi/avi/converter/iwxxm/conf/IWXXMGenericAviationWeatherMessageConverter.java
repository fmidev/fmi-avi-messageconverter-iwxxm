package fi.fmi.avi.converter.iwxxm.conf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.GenericBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMGenericBulletinScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.sigmet.GenericSIGMETIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.GenericTAFIWXXMScanner;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    @Bean
    public IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner(
            @Qualifier("genericAviationWeatherMessageIWXXMElementParser") final GenericAviationWeatherMessageParser<Element> messageParser) {
        return new IWXXMGenericBulletinScanner(messageParser);
    }

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser(
            IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromDOM(iwxxmGenericBulletinScanner);
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser(
            IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromString(iwxxmGenericBulletinScanner);
    }

    @Bean
    public GenericAviationWeatherMessageParser<Document> genericAviationWeatherMessageIWXXMDOMParser() {
        final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = createScannerMap();
        return new GenericAviationWeatherMessageParser.FromDOM(scannersMap);
    }

    @Bean
    public GenericAviationWeatherMessageParser<String> genericAviationWeatherMessageIWXXMStringParser() {
        final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = createScannerMap();
        return new GenericAviationWeatherMessageParser.FromString(scannersMap);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMElementParser")
    public GenericAviationWeatherMessageParser<Element> genericAviationWeatherMessageIWXXMElementParser() {
        final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = createScannerMap();
        return new GenericAviationWeatherMessageParser.FromElement(scannersMap);
    }

    private Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> createScannerMap() {
        final Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> scannersMap = new HashMap<>();

        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "TropicalCycloneSIGMET"),
                new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "TropicalCycloneSIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "VolcanicAshSIGMET"), new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "VolcanicAshSIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "SIGMET"), new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "SIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "TAF"), new GenericTAFIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "TAF"),
                new fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner());

        return scannersMap;
    }

    // Serializers:
}
