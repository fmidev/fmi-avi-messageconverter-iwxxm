package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.*;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser.ScannerKey;
import fi.fmi.avi.converter.iwxxm.generic.metar.GenericMETARSPECIIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.metar.METARSPECIFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.sigmet.GenericSIGMETAIRMETIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.sigmet.SIGMETAIRMETFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.swx.GenericSWXIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.swx.SWXFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.taf.GenericTAFIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.taf.TAFFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.tca.GenericTCAIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.tca.TCAFieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.vaa.GenericVAAIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.generic.vaa.VAAFieldXPathProvider;
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

        // XPath field providers
        final FieldXPathProvider tafFieldProvider = new TAFFieldXPathProvider();
        final FieldXPathProvider sigmetAirmetFieldProvider = new SIGMETAIRMETFieldXPathProvider();
        final FieldXPathProvider vaaFieldProvider = new VAAFieldXPathProvider();
        final FieldXPathProvider tcaFieldProvider = new TCAFieldXPathProvider();
        final FieldXPathProvider metarFieldProvider = new METARSPECIFieldXPathProvider();
        final FieldXPathProvider swxFieldProvider = new SWXFieldXPathProvider();

        final GenericTAFIWXXMScanner genericTafScanner = new GenericTAFIWXXMScanner(tafFieldProvider);
        scannersMap.put(new ScannerKey("TAF"), genericTafScanner);

        final GenericSWXIWXXMScanner genericSpaceWeatherScanner = new GenericSWXIWXXMScanner(swxFieldProvider);
        scannersMap.put(new ScannerKey("SpaceWeatherAdvisory"), genericSpaceWeatherScanner);

        final GenericTCAIWXXMScanner genericTcaScanner = new GenericTCAIWXXMScanner(tcaFieldProvider);
        scannersMap.put(new ScannerKey("TropicalCycloneAdvisory"), genericTcaScanner);

        final GenericVAAIWXXMScanner genericVaaScanner = new GenericVAAIWXXMScanner(vaaFieldProvider);
        scannersMap.put(new ScannerKey("VolcanicAshAdvisory"), genericVaaScanner);

        final GenericSIGMETAIRMETIWXXMScanner genericSigmetScanner = new GenericSIGMETAIRMETIWXXMScanner(sigmetAirmetFieldProvider);
        scannersMap.put(new ScannerKey("SIGMET"), genericSigmetScanner);
        scannersMap.put(new ScannerKey("TropicalCycloneSIGMET"), genericSigmetScanner);
        scannersMap.put(new ScannerKey("VolcanicAshSIGMET"), genericSigmetScanner);
        scannersMap.put(new ScannerKey("AIRMET"), genericSigmetScanner);

        final GenericMETARSPECIIWXXMScanner genericMetarScanner = new GenericMETARSPECIIWXXMScanner(metarFieldProvider);
        scannersMap.put(new ScannerKey("METAR"), genericMetarScanner);
        scannersMap.put(new ScannerKey("SPECI"), genericMetarScanner);

        return scannersMap;
    }

}
