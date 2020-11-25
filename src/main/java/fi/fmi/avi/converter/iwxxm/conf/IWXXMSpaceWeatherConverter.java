package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMSerializer;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;

@Configuration
public class IWXXMSpaceWeatherConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAdvisory> spaceWeatherIWXXMStringParser() {
        return new SpaceWeatherIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAdvisory> spaceWeatherIWXXMDOMParser() {
        return new SpaceWeatherIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMStringParser() {
        final SpaceWeatherBulletinIWXXMParser<String> retval = new SpaceWeatherBulletinIWXXMParser.FromString();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMDOMParser() {
        final SpaceWeatherBulletinIWXXMParser<Document> retval = new SpaceWeatherBulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisory, String> spaceWeatherIWXXMStringSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToString();
    }

    @Bean
    public AbstractIWXXMSerializer<SpaceWeatherAdvisory, Document> spaceWeatherIWXXMDOMSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherBulletin, String> spaceWeatherBulletinIWXXMStringSerializer() {
        final BulletinIWXXMStringSerializer<SpaceWeatherAdvisory, SpaceWeatherBulletin> retval = new BulletinIWXXMStringSerializer<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherBulletin, Document> spaceWeatherBulletinIWXXMDOMSerializer() {
        final BulletinIWXXMDOMSerializer<SpaceWeatherAdvisory, SpaceWeatherBulletin> retval = new BulletinIWXXMDOMSerializer<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

}
