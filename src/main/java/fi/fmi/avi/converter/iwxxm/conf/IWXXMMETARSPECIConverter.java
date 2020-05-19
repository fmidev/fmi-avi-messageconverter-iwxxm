package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.METARIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v2_1.metar.SPECIIWXXMParser;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;

@Configuration
public class IWXXMMETARSPECIConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, METAR> metarIWXXMStringParser() {
        return new METARIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, METAR> metarIWXXMDOMParser() {
        return new METARIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, SPECI> speciIWXXMStringParser() {
        return new SPECIIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SPECI> speciIWXXMDOMParser() {
        return new SPECIIWXXMParser.FromDOM();
    }

    //Serializers:
}
