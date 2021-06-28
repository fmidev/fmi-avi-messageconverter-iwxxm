package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.GenericAviationWeatherMessageParser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    // Parsers:
    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMDOMParser")
    public AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMDOMParser() {
        return new GenericAviationWeatherMessageParser.FromDOM();
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMStringParser")
    public AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMStringParser() {
        return new GenericAviationWeatherMessageParser.FromString();
    }
}
