package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("sigmetIWXXMStringSerializer")
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer() {
        return new SIGMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("sigmetIWXXMDOMSerializer")
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer() {
        return new SIGMETIWXXMSerializer.ToDOM();
    }

    @Bean
    @Qualifier("sigmetIWXXM30StringSerializer")
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXM30StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.SIGMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("sigmetIWXXM30DOMSerializer")
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXM30DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.sigmet.SIGMETIWXXMSerializer.ToDOM();
    }

    @Bean
    @Qualifier("airmetIWXXMStringSerializer")
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer() {
        return new AIRMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("airmetIWXXMDOMSerializer")
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer() {
        return new AIRMETIWXXMSerializer.ToDOM();
    }

}
