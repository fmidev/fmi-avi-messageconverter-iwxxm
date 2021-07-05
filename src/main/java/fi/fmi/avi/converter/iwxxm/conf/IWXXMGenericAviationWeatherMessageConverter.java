package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.GenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.GenericBulletinIWXXMParser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

@Configuration
public class IWXXMGenericBulletinConverter {

    @Bean
    public GenericAviationWeatherMessageScanner scanner() {
        return new GenericAviationWeatherMessageScanner();
    }

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser(GenericAviationWeatherMessageScanner scanner) {
        return new GenericBulletinIWXXMParser.FromDOM(scanner);
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser(GenericAviationWeatherMessageScanner scanner) {
        return new GenericBulletinIWXXMParser.FromString(scanner);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMDOMParser")
    public AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMDOMParser(GenericAviationWeatherMessageScanner scanner) {
        return new GenericAviationWeatherMessageParser.FromDOM(scanner);
    }

    @Bean
    @Qualifier("genericAviationWeatherMessageIWXXMStringParser")
    public AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageIWXXMStringParser(GenericAviationWeatherMessageScanner scanner) {
        return new GenericAviationWeatherMessageParser.FromString(scanner);
    }

    // Serializers:
}
