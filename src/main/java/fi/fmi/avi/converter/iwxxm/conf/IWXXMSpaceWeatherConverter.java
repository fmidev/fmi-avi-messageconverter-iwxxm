package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.SpaceWeatherAmd79BulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.profile.IWXXMSchemaProfile;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v3_0.swx.SpaceWeatherIWXXMSerializer;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAmd82Bulletin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

@Configuration
@Import(IWXXMSchemaProfilesConfig.class)
public class IWXXMSpaceWeatherConverter {

    // Parsers:

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXM30StringParser() {
        return new SpaceWeatherIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXM30DOMParser() {
        return new SpaceWeatherIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXM30StringParser() {
        final SpaceWeatherAmd79BulletinIWXXMParser<String> retval = new SpaceWeatherAmd79BulletinIWXXMParser.FromString();
        retval.setMessageConverter(spaceWeatherIWXXM30DOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXM30DOMParser() {
        final SpaceWeatherAmd79BulletinIWXXMParser<Document> retval = new SpaceWeatherAmd79BulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(spaceWeatherIWXXM30DOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd79, String> spaceWeatherIWXXM30StringSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToString();
    }

    @Bean
    public AbstractIWXXMSerializer<SpaceWeatherAdvisoryAmd79, Document> spaceWeatherIWXXM30DOMSerializer() {
        return new SpaceWeatherIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, String> spaceWeatherBulletinIWXXM30StringSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMStringSerializer<>(aixmWxSchemaProfile, spaceWeatherIWXXM30DOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, Document> spaceWeatherBulletinIWXXM30DOMSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMDOMSerializer<>(aixmWxSchemaProfile, spaceWeatherIWXXM30DOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd82, String> spaceWeatherIWXXM20252StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2025_2.swx.SpaceWeatherIWXXMSerializer.ToString();
    }

    @Bean
    public AbstractIWXXMSerializer<SpaceWeatherAdvisoryAmd82, Document> spaceWeatherIWXXM20252DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v2025_2.swx.SpaceWeatherIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd82Bulletin, String> spaceWeatherBulletinIWXXM20252StringSerializer(final IWXXMSchemaProfile aixmFullSchemaProfile) {
        return new BulletinIWXXMStringSerializer<>(aixmFullSchemaProfile, spaceWeatherIWXXM20252DOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<SpaceWeatherAmd82Bulletin, Document> spaceWeatherBulletinIWXXM20252DOMSerializer(final IWXXMSchemaProfile aixmFullSchemaProfile) {
        return new BulletinIWXXMDOMSerializer<>(aixmFullSchemaProfile, spaceWeatherIWXXM20252DOMSerializer());
    }

}
