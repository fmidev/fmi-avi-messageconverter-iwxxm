package fi.fmi.avi.converter.iwxxm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

@Configuration
@Import(IWXXMConverter.class)
public class IWXXMTestConfiguration {

    // TAF

    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafIWXXM30StringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAF> tafIWXXM30DOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXMStringParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAFBulletin> tafBulletinIWXXM30StringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<Document, TAFBulletin> tafBulletinIWXXM30DOMParser;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXM30StringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXM30DOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXM30StringSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXM30DOMSerializer;

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
    @Qualifier("sigmetIWXXMStringSerializer")
    private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer;

    @Autowired
    @Qualifier("sigmetIWXXMDOMSerializer")
    private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer;

    @Autowired
    @Qualifier("sigmetIWXXM30StringSerializer")
    private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXM30StringSerializer;

    @Autowired
    @Qualifier("sigmetIWXXM30DOMSerializer")
    private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXM30DOMSerializer;

    @Autowired
    @Qualifier("sigmetIWXXM20231StringSerializer")
    private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXM20231StringSerializer;

    @Autowired
    @Qualifier("sigmetIWXXM20231DOMSerializer")
    private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXM20231DOMSerializer;

    @Autowired
    @Qualifier("airmetIWXXMStringSerializer")
    private AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer;

    @Autowired
    @Qualifier("airmetIWXXMDOMSerializer")
    private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer;

    @Autowired
    @Qualifier("airmetIWXXM30StringSerializer")
    private AviMessageSpecificConverter<AIRMET, String> airmetIWXXM30StringSerializer;

    @Autowired
    @Qualifier("airmetIWXXM30DOMSerializer")
    private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXM30DOMSerializer;

    @Autowired
    @Qualifier("airmetIWXXM20231StringSerializer")
    private AviMessageSpecificConverter<AIRMET, String> airmetIWXXM20231StringSerializer;

    @Autowired
    @Qualifier("airmetIWXXM20231DOMSerializer")
    private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXM20231DOMSerializer;

    // Space weather

    @Autowired
    private AviMessageSpecificConverter<String, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXM30StringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SpaceWeatherAdvisoryAmd79> spaceWeatherIWXXM30DOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXM30StringParser;

    @Autowired
    private AviMessageSpecificConverter<Document, SpaceWeatherAmd79Bulletin> spaceWeatherBulletinIWXXM30DOMParser;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd79, String> spaceWeatherIWXXM30StringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd79, Document> spaceWeatherIWXXM30DOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, String> spaceWeatherBulletinIWXXM30StringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, Document> spaceWeatherBulletinIWXXM30DOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd82, String> spaceWeatherIWXXM20252StringSerializer;

    @Autowired
    private AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd82, Document> spaceWeatherIWXXM20252DOMSerializer;

    // Generic bulletins

    @Autowired
    private AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser;

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser;

    // Generic Aviation Weather Message

    @Autowired
    @Qualifier("genericAviationWeatherMessageIWXXMDOMParser")
    private AviMessageSpecificConverter<Document, GenericAviationWeatherMessage> genericAviationWeatherMessageDOMParser;

    @Autowired
    @Qualifier("genericAviationWeatherMessageIWXXMStringParser")
    private AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageStringParser;

    @Bean
    static ObjectMapper getObjectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        return om;
    }

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter p = new AviMessageConverter();

        // TAF;
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_TAF_POJO, tafIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_STRING_TO_TAF_POJO, tafIWXXM30StringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_TAF_POJO, tafIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_DOM_TO_TAF_POJO, tafIWXXM30DOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING, tafIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM30_STRING, tafIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM, tafIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM30_DOM, tafIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING, tafBulletinIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_IWXXM30_STRING, tafBulletinIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM, tafBulletinIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_BULLETIN_POJO_TO_WMO_COLLECT_IWXXM30_DOM, tafBulletinIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_STRING_TO_TAF_BULLETIN_POJO, tafBulletinIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_IWXXM30_STRING_TO_TAF_BULLETIN_POJO, tafBulletinIWXXM30StringParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_TAF_BULLETIN_POJO, tafBulletinIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_IWXXM30_DOM_TO_TAF_BULLETIN_POJO, tafBulletinIWXXM30DOMParser);

        // METAR & SPECI:
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_METAR_POJO, metarIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_METAR_POJO, metarIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_STRING_TO_SPECI_POJO, speciIWXXMStringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM21_DOM_TO_SPECI_POJO, speciIWXXMDOMParser);

        // SIGMET & AIRMET:
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_DOM, sigmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING, sigmetIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_DOM, sigmetIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM30_STRING, sigmetIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_DOM, sigmetIWXXM20231DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM2023_1_STRING, sigmetIWXXM20231StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM, airmetIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING, airmetIWXXMStringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM30_DOM, airmetIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM30_STRING, airmetIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_DOM, airmetIWXXM20231DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM2023_1_STRING, airmetIWXXM20231StringSerializer);

        // Space weather
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_STRING_TO_SPACE_WEATHER_POJO, spaceWeatherIWXXM30StringParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM30_DOM_TO_SPACE_WEATHER_POJO, spaceWeatherIWXXM30DOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_STRING, spaceWeatherIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM30_DOM, spaceWeatherIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SWX_30_BULLETIN_POJO_TO_WMO_COLLECT_STRING, spaceWeatherBulletinIWXXM30StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SWX_30_BULLETIN_POJO_TO_WMO_COLLECT_DOM, spaceWeatherBulletinIWXXM30DOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_STRING_TO_SWX_30_BULLETIN_POJO, spaceWeatherBulletinIWXXM30StringParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_SWX_30_BULLETIN_POJO, spaceWeatherBulletinIWXXM30DOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_STRING, spaceWeatherIWXXM20252StringSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.SPACE_WEATHER_POJO_TO_IWXXM2025_2_DOM, spaceWeatherIWXXM20252DOMSerializer);

        // Generic bulletin messages:
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.WMO_COLLECT_STRING_TO_GENERIC_BULLETIN_POJO, genericBulletinIWXXMStringParser);

        p.setMessageSpecificConverter(IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, genericAviationWeatherMessageDOMParser);
        p.setMessageSpecificConverter(IWXXMConverter.IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, genericAviationWeatherMessageStringParser);

        return p;
    }

}
