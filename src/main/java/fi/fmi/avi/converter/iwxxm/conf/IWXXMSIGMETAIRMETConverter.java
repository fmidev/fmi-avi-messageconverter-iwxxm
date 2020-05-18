package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.v21.airmet.AIRMETIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.v21.airmet.AIRMETIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.v21.sigmet.SIGMETIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.v21.sigmet.SIGMETIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.v21.sigmet.SIGMETIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.v21.sigmet.SIGMETIWXXMStringSerializer;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

@Configuration
public class IWXXMSIGMETAIRMETConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SIGMET> sigmetIWXXMStringParser() {
        return new SIGMETIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SIGMET> sigmetIWXXMDOMParser() {
        return new SIGMETIWXXMDOMParser();
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer() {
        return new SIGMETIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer() {
        return new SIGMETIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer() {
        return new AIRMETIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer() {
        return new AIRMETIWXXMDOMSerializer();
    }

}
