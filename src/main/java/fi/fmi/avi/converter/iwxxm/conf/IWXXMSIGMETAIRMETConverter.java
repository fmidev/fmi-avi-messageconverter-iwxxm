package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

@Configuration
public class IWXXMSIGMETAIRMETConverter {
    // Serializers:

    @Bean
    @Qualifier("sigmetIWXXMStringSerializer")
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2_1.sigmet.SIGMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("sigmetIWXXMDOMSerializer")
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2_1.sigmet.SIGMETIWXXMSerializer.ToDOM();
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
    @Qualifier("sigmetIWXXM20231StringSerializer")
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXM20231StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2023_1.sigmet.SIGMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("sigmetIWXXM20231DOMSerializer")
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXM20231DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2023_1.sigmet.SIGMETIWXXMSerializer.ToDOM();
    }

    @Bean
    @Qualifier("airmetIWXXMStringSerializer")
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2_1.airmet.AIRMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("airmetIWXXMDOMSerializer")
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2_1.airmet.AIRMETIWXXMSerializer.ToDOM();
    }

    @Bean
    @Qualifier("airmetIWXXM30StringSerializer")
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXM30StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.airmet.AIRMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("airmetIWXXM30DOMSerializer")
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXM30DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.airmet.AIRMETIWXXMSerializer.ToDOM();
    }

    @Bean
    @Qualifier("airmetIWXXM20231StringSerializer")
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXM20231StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2023_1.airmet.AIRMETIWXXMSerializer.ToString();
    }

    @Bean
    @Qualifier("airmetIWXXM20231DOMSerializer")
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXM20231DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2023_1.airmet.AIRMETIWXXMSerializer.ToDOM();
    }
}
