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
import fi.fmi.avi.converter.iwxxm.v2_1.metar.GenericMETARSPECIIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.airmet.GenericAIRMETIWXXMScanner;
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

        final String iwxxm_2_1_NamespaceURI = "http://icao.int/iwxxm/2.1";
        final String iwxxm_3_0_NamespaceURI = "http://icao.int/iwxxm/3.0";
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "TropicalCycloneSIGMET"), new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "TropicalCycloneSIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "VolcanicAshSIGMET"), new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "VolcanicAshSIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "SIGMET"), new GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "SIGMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "TAF"), new GenericTAFIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "TAF"),
                new fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner());
        final GenericMETARSPECIIWXXMScanner genericMETARSPECIIWXXM21Scanner = new GenericMETARSPECIIWXXMScanner();
        final fi.fmi.avi.converter.iwxxm.v3_0.metar.GenericMETARSPECIIWXXMScanner genericMETARSPECIIWXXM30Scanner = new fi.fmi.avi.converter.iwxxm.v3_0.metar.GenericMETARSPECIIWXXMScanner();
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "METAR"), genericMETARSPECIIWXXM21Scanner);
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "METAR"), genericMETARSPECIIWXXM30Scanner);
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_2_1_NamespaceURI, "SPECI"), genericMETARSPECIIWXXM21Scanner);
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey(iwxxm_3_0_NamespaceURI, "SPECI"), genericMETARSPECIIWXXM30Scanner);
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/2.1", "AIRMET"), new GenericAIRMETIWXXMScanner());
        scannersMap.put(new GenericAviationWeatherMessageParser.ScannerKey("http://icao.int/iwxxm/3.0", "AIRMET"),
                new fi.fmi.avi.converter.iwxxm.v3_0.airmet.GenericAIRMETIWXXMScanner());

        return scannersMap;
    }

    // Serializers:
}
