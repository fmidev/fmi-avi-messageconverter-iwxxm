package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory.SpaceWeatherBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory.SpaceWeatherIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory.SpaceWeatherIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory.SpaceWeatherIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory.SpaceWeatherIWXXMStringSerializer;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherBulletin;

@Configuration
public class IWXXMSpaceWeatherConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAdvisory> spaceWeatherIWXXMStringParser() {
        return new SpaceWeatherIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAdvisory> spaceWeatherIWXXMDOMParser() {
        return new SpaceWeatherIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMStringParser() {
        final SpaceWeatherBulletinIWXXMParser<String> retval = new SpaceWeatherBulletinIWXXMParser.AsString();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMDOMParser() {
        final SpaceWeatherBulletinIWXXMParser<Document> retval = new SpaceWeatherBulletinIWXXMParser.AsDOM();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisory, String> spaceWeatherIWXXMStringSerializer() {
        return new SpaceWeatherIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisory, Document> spaceWeatherIWXXMDOMSerializer() {
        return new SpaceWeatherIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherBulletin, String> spaceWeatherBulletinIWXXMStringSerializer() {
        final BulletinIWXXMSerializer.AsString<SpaceWeatherAdvisory, SpaceWeatherBulletin> retval = new BulletinIWXXMSerializer.AsString<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherBulletin, Document> spaceWeatherBulletinIWXXMDOMSerializer() {
        final BulletinIWXXMSerializer.AsDOM<SpaceWeatherAdvisory, SpaceWeatherBulletin> retval = new BulletinIWXXMSerializer.AsDOM<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

}
