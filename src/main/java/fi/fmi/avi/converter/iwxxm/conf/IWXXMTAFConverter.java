package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.TAFBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.profile.IWXXMSchemaProfile;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.TAFIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.TAFIWXXMSerializer;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

@Configuration
@Import(IWXXMSchemaProfilesConfig.class)
public class IWXXMTAFConverter {

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser() {
        return new TAFIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser() {
        return new TAFIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, TAF> tafIWXXM30StringParser() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMParser.FromString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAF> tafIWXXM30DOMParser() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMParser.FromDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXMStringParser() {
        final TAFBulletinIWXXMParser<String> retval = new TAFBulletinIWXXMParser.FromString();
        retval.setMessageConverter(tafIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXM30StringParser() {
        final TAFBulletinIWXXMParser<String> retval = new TAFBulletinIWXXMParser.FromString();
        retval.setMessageConverter(tafIWXXM30DOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXMDOMParser() {
        final TAFBulletinIWXXMParser<Document> retval = new TAFBulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(tafIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXM30DOMParser() {
        final TAFBulletinIWXXMParser<Document> retval = new TAFBulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(tafIWXXM30DOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMSerializer.ToString();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXM30StringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMSerializer.ToString();
    }

    @Bean
    public AbstractIWXXMSerializer<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMSerializer.ToDOM();
    }

    @Bean
    public AbstractIWXXMSerializer<TAF, Document> tafIWXXM30DOMSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMSerializer.ToDOM();
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMStringSerializer<>(aixmWxSchemaProfile, tafIWXXMDOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXM30StringSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMStringSerializer<>(aixmWxSchemaProfile, tafIWXXM30DOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMDOMSerializer<>(aixmWxSchemaProfile, tafIWXXMDOMSerializer());
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXM30DOMSerializer(final IWXXMSchemaProfile aixmWxSchemaProfile) {
        return new BulletinIWXXMDOMSerializer<>(aixmWxSchemaProfile, tafIWXXM30DOMSerializer());
    }

}
