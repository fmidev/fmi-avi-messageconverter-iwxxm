package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.TAFBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.TAFIWXXMParser;
import fi.fmi.avi.converter.iwxxm.v2_1.taf.TAFIWXXMSerializer;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import icao.iwxxm21.TAFType;

@Configuration
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
    public AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXMDOMParser() {
        final TAFBulletinIWXXMParser<Document> retval = new TAFBulletinIWXXMParser.FromDOM();
        retval.setMessageConverter(tafIWXXMDOMParser());
        return retval;
    }

    // Serializers:

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMSerializer.ToString();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, String> taf30IWXXMStringSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMSerializer.ToString();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, Document> taf30IWXXMDomSerializer() {
        return new fi.fmi.avi.converter.iwxxm.v3_0.taf.TAFIWXXMSerializer.ToDOM();
    }

    @Bean
    public AbstractIWXXMSerializer<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMSerializer.ToDOM();
    }

    // TODO: check if this bean / class is actually used / required somewhere?
    @Bean
    public AviMessageSpecificConverter<TAF, TAFType> tafIWXXMJAXBSerializer() {
        return new TAFIWXXMSerializer.ToJAXBObject();
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer() {
        final BulletinIWXXMStringSerializer<TAF, TAFBulletin> retval = new BulletinIWXXMStringSerializer<>();
        retval.setMessageConverter(tafIWXXMDOMSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer() {
        final BulletinIWXXMDOMSerializer<TAF, TAFBulletin> retval = new BulletinIWXXMDOMSerializer<>();
        retval.setMessageConverter(tafIWXXMDOMSerializer());
        return retval;
    }

}
