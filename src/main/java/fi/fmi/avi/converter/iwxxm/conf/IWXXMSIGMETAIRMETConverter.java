package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.v2_1.airmet.AIRMETIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.v2_1.sigmet.SIGMETIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v2_1.sigmet.SIGMETIWXXMSerializer;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

@Configuration
public class IWXXMSIGMETAIRMETConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SIGMET> sigmetIWXXMStringParser() {
        return new SIGMETIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SIGMET> sigmetIWXXMDOMParser() {
        return new SIGMETIWXXMParser.FromDOM();
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer() {
        return new SIGMETIWXXMSerializer.ToString();
    }

    @Bean
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer() {
        return new SIGMETIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer() {
        return new AIRMETIWXXMSerializer.ToString();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer() {
        return new AIRMETIWXXMSerializer.ToDOM();
    }

}
