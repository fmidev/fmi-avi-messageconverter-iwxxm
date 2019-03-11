package fi.fmi.avi.converter.iwxxm.conf;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.airmet.AIRMETIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.airmet.AIRMETIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.sigmet.SIGMETIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.sigmet.SIGMETIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.sigmet.SIGMETIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.sigmet.SIGMETIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringSerializer;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
@Configuration
public class IWXXMConverter {

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document String.
     */
    public static final ConversionSpecification<TAF, String> TAF_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(TAF.class, String.class,
            null, "TAF, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document DOM Node.
     */
    public static final ConversionSpecification<TAF, Document> TAF_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(TAF.class, Document.class,
            null, "TAF, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document String to {@link TAF}.
     */
    public static final ConversionSpecification<String,TAF> IWXXM21_STRING_TO_TAF_POJO = new ConversionSpecification<>(String.class,TAF.class,
            "TAF, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document DOM Node to {@link TAF}.
     */
    public static final ConversionSpecification<Document,TAF> IWXXM21_DOM_TO_TAF_POJO = new ConversionSpecification<>(Document.class,TAF.class,
            "TAF, XML/IWXXM 2.1", null);


    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document String.
     */
    public static final ConversionSpecification<SIGMET, String> SIGMET_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(SIGMET.class, String.class,
            null, "SIGMET, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document DOM Node.
     */
    public static final ConversionSpecification<SIGMET, Document> SIGMET_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(SIGMET.class, Document.class,
            null, "SIGMET, XML/IWXXM 2.1");


    public static final ConversionSpecification<String,SIGMET> IWXXM21_STRING_TO_SIGMET_POJO = new ConversionSpecification<>(String.class,SIGMET.class,
            "SIGMET, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document DOM Node to {@link TAF}.
     */
    public static final ConversionSpecification<Document,SIGMET> IWXXM21_DOM_TO_SIGMET_POJO = new ConversionSpecification<>(Document.class,SIGMET.class,
            "SIGMET, XML/IWXXM 2.1", null);


    /**
     * Pre-configured spec for {@link AIRMET} to IWXXM 2.1 XML format AIRMET document String.
     */
    public static final ConversionSpecification<AIRMET, String> AIRMET_POJO_TO_IWXXM21_STRING = new ConversionSpecification<>(AIRMET.class, String.class,
            null, "AIRMET, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for {@link AIRMET} to IWXXM 2.1 XML format AIRMET document DOM Node.
     */
    public static final ConversionSpecification<AIRMET, Document> AIRMET_POJO_TO_IWXXM21_DOM = new ConversionSpecification<>(AIRMET.class, Document.class,
            null, "AIRMET, XML/IWXXM 2.1");

    @Bean
    public AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<String, TAF> tafIWXXMStringParser() {
        return new TAFIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, TAF> tafIWXXMDOMParser() {
        return new TAFIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer() {
        return new SIGMETIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer() {
        return new SIGMETIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<String, SIGMET> sigmetIWXXMStringParser() {
        return new SIGMETIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SIGMET> sigmetIWXXMDOMParser() {
        return new SIGMETIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer() {
        return new AIRMETIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer() {
        return new AIRMETIWXXMStringSerializer();
    }

}
