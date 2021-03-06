package fi.fmi.avi.converter.iwxxm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

@Configuration
@Import(IWXXMConverter.class)
public class IWXXMTestConfiguration {
    @Bean
    private static ObjectMapper getObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        return om;
    }

    // TAF
    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer;

    // METAR & SPECI

    @Autowired
    private AviMessageSpecificConverter<String, METAR> metarIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, METAR> metarIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, SPECI> speciIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SPECI> speciIWXXMDOMParser;

    // SIGMET & AIRMET

    @Autowired
    private AviMessageSpecificConverter<String, SIGMET> sigmetIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SIGMET> sigmetIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer;

    // Space weather

    @Autowired
    private AviMessageSpecificConverter<String, SpaceWeatherAdvisory> spaceWeatherIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SpaceWeatherAdvisory> spaceWeatherIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SpaceWeatherBulletin> spaceWeatherBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisory, String> spaceWeatherIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisory, Document> spaceWeatherIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherBulletin, String> spaceWeatherBulletinIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherBulletin, Document> spaceWeatherBulletinIWXXMDOMSerializer;

    // Generic bulletins

    @Autowired
    private AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();

        // TAF;
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, tafIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, tafIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING, tafIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM, tafIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING, tafBulletinIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM, tafBulletinIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_STRING_TO_TAF_BULLETIN_POJO, tafBulletinIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO, tafBulletinIWXXMDOMParser);

        // METAR & SPECI:
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_METAR_POJO, metarIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, metarIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_SPECI_POJO, speciIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_SPECI_POJO, speciIWXXMDOMParser);

        // SIGMET & AIRMET:
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_DOM, sigmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING, sigmetIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_SIGMET_POJO, sigmetIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_SIGMET_POJO, sigmetIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM, airmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING, airmetIWXXMStringSerializer);

        // Space weather
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, spaceWeatherIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO, spaceWeatherIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING, spaceWeatherIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_DOM, spaceWeatherIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SWX_BULLETIN_POJO_TO_WMO_COLLECT_STRING, spaceWeatherBulletinIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SWX_BULLETIN_POJO_TO_WMO_COLLECT_DOM, spaceWeatherBulletinIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_STRING_TO_SWX_BULLETIN_POJO, spaceWeatherBulletinIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_SWX_BULLETIN_POJO, spaceWeatherBulletinIWXXMDOMParser);

        // Generic bulletin messages:
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMStringParser);
        return p;
    }

}
