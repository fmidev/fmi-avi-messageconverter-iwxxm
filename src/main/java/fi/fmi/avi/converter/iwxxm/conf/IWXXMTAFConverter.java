package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.v21.taf.TAFBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v21.taf.TAFIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v21.taf.TAFIWXXMSerializer;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import icao.iwxxm21.TAFType;

@Configuration
public class IWXXMTAFConverter {

    // Parsers:
    @Bean
    public AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser() {
        return new TAFIWXXMParser.AsString();
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser() {
        return new TAFIWXXMParser.AsDOM();
    }

    @Bean
    public AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXMStringParser() {
        final TAFBulletinIWXXMParser<String> retval = new TAFBulletinIWXXMParser.AsString();
        retval.setMessageConverter(tafIWXXMDOMParser());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXMDOMParser() {
        final TAFBulletinIWXXMParser<Document> retval = new TAFBulletinIWXXMParser.AsDOM();
        retval.setMessageConverter(tafIWXXMDOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMSerializer.AsString();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMSerializer.AsDOM();
    }

    // TODO: check if this bean / class is actually used / required somewhere?
    @Bean
    public AviMessageSpecificConverter<TAF, TAFType> tafIWXXMJAXBSerializer() {
        return new TAFIWXXMSerializer.AsJAXBObject();
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer() {
        final BulletinIWXXMSerializer.AsString<TAF, TAFBulletin> retval = new BulletinIWXXMSerializer.AsString<>();
        retval.setMessageConverter(tafIWXXMDOMSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer() {
        final BulletinIWXXMSerializer.AsDOM<TAF, TAFBulletin> retval = new BulletinIWXXMSerializer.AsDOM<>();
        retval.setMessageConverter(tafIWXXMDOMSerializer());
        return retval;
    }

}
