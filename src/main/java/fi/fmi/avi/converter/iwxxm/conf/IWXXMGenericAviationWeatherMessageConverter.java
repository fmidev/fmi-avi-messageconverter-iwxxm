package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.iwxxm.generic.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.GenericBulletinIWXXMParser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@Configuration
public class IWXXMGenericAviationWeatherMessageConverter {

    @Bean
    public GenericAviationWeatherMessageScanner genericAviationWeatherMessageIWXXMScanner() {
        return new GenericAviationWeatherMessageScanner();
    }

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser(GenericAviationWeatherMessageScanner genericAviationWeatherMessageIWXXMScanner) {
        return new GenericBulletinIWXXMParser.FromDOM(genericAviationWeatherMessageIWXXMScanner);
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser(GenericAviationWeatherMessageScanner genericAviationWeatherMessageIWXXMScanner) {
        return new GenericBulletinIWXXMParser.FromString(genericAviationWeatherMessageIWXXMScanner);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMDOMParser")
    public AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMDOMParser(GenericAviationWeatherMessageScanner genericAviationWeatherMessageIWXXMScanner) {
        return new GenericAviationWeatherMessageParser.FromDOM(genericAviationWeatherMessageIWXXMScanner);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMStringParser")
    public AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMStringParser(GenericAviationWeatherMessageScanner genericAviationWeatherMessageIWXXMScanner) {
        return new GenericAviationWeatherMessageParser.FromString(genericAviationWeatherMessageIWXXMScanner);
    }

    // Serializers:
}
