package fi.fmi.avi.converter.iwxxm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

@Configuration
@Import(IWXXMConverter.class)
public class IWXXMTestConfiguration {
    @Bean
    private static ObjectMapper getObjectMapper() {
        System.err.println("ObjectMapper created in IWXXMTestConfiguration");
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.registerModule(new JtsModule());
        om.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        return om;
    }

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer;
    
    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, METAR> metarIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, METAR> metarIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SPECI> speciIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, SPECI> speciIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM,tafIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING,tafIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, tafIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, tafIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_METAR_POJO, metarIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, metarIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_SPECI_POJO, speciIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_SPECI_POJO, speciIWXXMDOMParser);

        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM, tafBulletinIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING, tafBulletinIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_DOM,sigmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING,sigmetIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM,airmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING,airmetIWXXMStringSerializer);

        return p;
    }

}
