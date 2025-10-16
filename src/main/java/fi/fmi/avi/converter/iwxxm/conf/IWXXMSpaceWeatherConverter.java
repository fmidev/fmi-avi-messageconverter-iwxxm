package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.SpaceWeatherBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMSerializer;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

@Configuration
public class IWXXMSpaceWeatherConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXMStringParser() {
        return new SpaceWeatherIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXMDOMParser() {
        return new SpaceWeatherIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXMStringParser() {
        final SpaceWeatherBulletinIWXXMParser<String> retval = new SpaceWeatherBulletinIWXXMParser.FromString();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXMDOMParser() {
        final SpaceWeatherBulletinIWXXMParser<Document> retval = new SpaceWeatherBulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(spaceWeatherIWXXMDOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd79, String> spaceWeatherIWXXMStringSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToString();
    }

    @Bean
    public AbstractIWXXMSerializer<SpaceWeatherAdvisoryAmd79, Document> spaceWeatherIWXXMDOMSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, String> spaceWeatherBulletinIWXXMStringSerializer() {
        final BulletinIWXXMStringSerializer<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> retval = new BulletinIWXXMStringSerializer<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, Document> spaceWeatherBulletinIWXXMDOMSerializer() {
        final BulletinIWXXMDOMSerializer<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> retval = new BulletinIWXXMDOMSerializer<>();
        retval.setMessageConverter(spaceWeatherIWXXMDOMSerializer());
        return retval;
    }

}
