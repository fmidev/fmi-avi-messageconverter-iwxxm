package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.v21.metar.METARIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.v21.metar.METARIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.v21.metar.SPECIIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.v21.metar.SPECIIWXXMStringParser;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;

@Configuration
public class IWXXMMETARSPECIConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, METAR> metarIWXXMStringParser() {
        return new METARIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, METAR> metarIWXXMDOMParser() {
        return new METARIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<String, SPECI> speciIWXXMStringParser() {
        return new SPECIIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SPECI> speciIWXXMDOMParser() {
        return new SPECIIWXXMDOMParser();
    }

    //Serializers:
}
