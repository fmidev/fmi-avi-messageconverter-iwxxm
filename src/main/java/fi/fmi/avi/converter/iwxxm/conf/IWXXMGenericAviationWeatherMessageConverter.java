package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser.ScannerKey;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.GenericBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMGenericBulletinScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.GenericMETARSPECIIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.sigmet.GenericSIGMETAIRMETIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.GenericTAFIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.tca.GenericTropicalCycloneAdvisoryIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v2_1.vaa.GenericVolcanicAshAdvisoryIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.GenericSpaceWeatherAdvisoryIWXXMScanner;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

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

        final String iwxxm_2_1_NamespaceURI = "http://icao.int/iwxxm/2.1";
        final String iwxxm_3_0_NamespaceURI = "http://icao.int/iwxxm/3.0";
        final String iwxxm_2023_1_NamespaceURI = "http://icao.int/iwxxm/2023-1";
        final String iwxxm_2025_2_NamespaceURI = "http://icao.int/iwxxm/2025-2";

        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "TAF"), new GenericTAFIWXXMScanner());
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "TAF"), new fi.fmi.avi.converter.iwxxm.v3_0.taf.GenericTAFIWXXMScanner());

        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "SpaceWeatherAdvisory"), new GenericSpaceWeatherAdvisoryIWXXMScanner());
        scannersMap.put(new ScannerKey(iwxxm_2025_2_NamespaceURI, "SpaceWeatherAdvisory"),
                new fi.fmi.avi.converter.iwxxm.v2025_2.swx.GenericSpaceWeatherAdvisoryIWXXMScanner());

        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "TropicalCycloneAdvisory"), new GenericTropicalCycloneAdvisoryIWXXMScanner());
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "TropicalCycloneAdvisory"),
                new fi.fmi.avi.converter.iwxxm.v3_0.tca.GenericTropicalCycloneAdvisoryIWXXMScanner());

        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "VolcanicAshAdvisory"), new GenericVolcanicAshAdvisoryIWXXMScanner());
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "VolcanicAshAdvisory"),
                new fi.fmi.avi.converter.iwxxm.v3_0.vaa.GenericVolcanicAshAdvisoryIWXXMScanner());

        final GenericSIGMETAIRMETIWXXMScanner genericSIGMETIWXXM21Scanner = new GenericSIGMETAIRMETIWXXMScanner();
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "SIGMET"), genericSIGMETIWXXM21Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "TropicalCycloneSIGMET"), genericSIGMETIWXXM21Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "VolcanicAshSIGMET"), genericSIGMETIWXXM21Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "AIRMET"), genericSIGMETIWXXM21Scanner);

        final fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETAIRMETIWXXMScanner genericSIGMETIWXXM30Scanner = new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.GenericSIGMETAIRMETIWXXMScanner();
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "TropicalCycloneSIGMET"), genericSIGMETIWXXM30Scanner);
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "VolcanicAshSIGMET"), genericSIGMETIWXXM30Scanner);
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "SIGMET"), genericSIGMETIWXXM30Scanner);
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "AIRMET"), genericSIGMETIWXXM30Scanner);

        final fi.fmi.avi.converter.iwxxm.v2023_1.sigmet.GenericSIGMETAIRMETIWXXMScanner genericSIGMETIWXXM20231Scanner = new fi.fmi.avi.converter.iwxxm.v2023_1.sigmet.GenericSIGMETAIRMETIWXXMScanner();
        scannersMap.put(new ScannerKey(iwxxm_2023_1_NamespaceURI, "TropicalCycloneSIGMET"), genericSIGMETIWXXM20231Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2023_1_NamespaceURI, "VolcanicAshSIGMET"), genericSIGMETIWXXM20231Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2023_1_NamespaceURI, "SIGMET"), genericSIGMETIWXXM20231Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2023_1_NamespaceURI, "AIRMET"), genericSIGMETIWXXM20231Scanner);

        final GenericMETARSPECIIWXXMScanner genericMETARSPECIIWXXM21Scanner = new GenericMETARSPECIIWXXMScanner();
        final fi.fmi.avi.converter.iwxxm.v3_0.metar.GenericMETARSPECIIWXXMScanner genericMETARSPECIIWXXM30Scanner = new fi.fmi.avi.converter.iwxxm.v3_0.metar.GenericMETARSPECIIWXXMScanner();
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "METAR"), genericMETARSPECIIWXXM21Scanner);
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "METAR"), genericMETARSPECIIWXXM30Scanner);
        scannersMap.put(new ScannerKey(iwxxm_2_1_NamespaceURI, "SPECI"), genericMETARSPECIIWXXM21Scanner);
        scannersMap.put(new ScannerKey(iwxxm_3_0_NamespaceURI, "SPECI"), genericMETARSPECIIWXXM30Scanner);

        return scannersMap;
    }

    // Serializers:
}
