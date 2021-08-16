package fi.fmi.avi.converter.iwxxm.conf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.iwxxm.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.GenericBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMGenericBulletinScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.sigmet.GenericSIGMETIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.GenericTAFIWXXMScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    @Bean
    public GenericSIGMETIWXXMScanner genericSIGMETIWXXM21Scanner() {
        return new GenericSIGMETIWXXMScanner();
    }

    @Bean
    public fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner genericSIGMETIWXXM30Scanner() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner();
    }

    @Bean
    public GenericTAFIWXXMScanner genericTAFIWXXM21Scanner() {
        return new GenericTAFIWXXMScanner();
    }

    @Bean
    public fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner genericTAFIWXXM30Scanner() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner();
    }

    @Bean
    public GenericAviationWeatherMessageScannerMap genericAviationWeatherMessageScannerMap(GenericSIGMETIWXXMScanner genericSIGMETIWXXM21Scanner, GenericTAFIWXXMScanner genericTAFIWXXM21Scanner,
            fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner genericSIGMETIWXXM30Scanner,
            fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner genericTAFIWXXM30Scanner) {
        Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> genericMessageScannerMap =
                new HashMap<>();

        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "TropicalCycloneSIGMET"), genericSIGMETIWXXM21Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "TropicalCycloneSIGMET"), genericSIGMETIWXXM30Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "VolcanicAshSIGMET"), genericSIGMETIWXXM21Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "VolcanicAshSIGMET"), genericSIGMETIWXXM30Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "SIGMET"), genericSIGMETIWXXM21Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "SIGMET"), genericSIGMETIWXXM30Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "TAF"), genericTAFIWXXM21Scanner);
        genericMessageScannerMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "TAF"), genericTAFIWXXM30Scanner);

        return new GenericAviationWeatherMessageScannerMap(genericMessageScannerMap);
    }

    @Bean
    public IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner(@Qualifier("genericAviationWeatherMessageIWXXMDOMParser") AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> messageParser) {
        return new IWXXMGenericBulletinScanner((GenericAviationWeatherMessageParser) messageParser);
    }

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser(IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromDOM(iwxxmGenericBulletinScanner);
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser(IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        return new GenericBulletinIWXXMParser.FromString(iwxxmGenericBulletinScanner);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMDOMParser")
    public AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMDOMParser(
            GenericAviationWeatherMessageScannerMap genericAviationWeatherMessageScannerMap) {
        return new GenericAviationWeatherMessageParser.FromDOM(genericAviationWeatherMessageScannerMap.getMap());
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMStringParser")
    public AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMStringParser(
            GenericAviationWeatherMessageScannerMap genericAviationWeatherMessageScannerMap) {
        return new GenericAviationWeatherMessageParser.FromString(genericAviationWeatherMessageScannerMap.getMap());
    }

    private class GenericAviationWeatherMessageScannerMap {
        private Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> map;
        public GenericAviationWeatherMessageScannerMap(Map scannerMap) {
            this.map = scannerMap;
        }
        public Map<GenericAviationWeatherMessageParser.ScannerKey, GenericAviationWeatherMessageScanner> getMap() {
            return map;
        }
    }

    // Serializers:
}
