package fi.fmi.avi.converter.iwxxm.conf;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.taf.TAFBulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFBulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMJAXBSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringSerializer;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import icao.iwxxm21.TAFType;

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
     * Pre-configured spec for {@link TAF} to IWXXM 2.1 XML format TAF document DOM Node.
     */
    public static final ConversionSpecification<TAF, TAFType> TAF_POJO_TO_IWXXM21_JAXB = new ConversionSpecification<>(TAF.class, TAFType.class, null, null);

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
     * Pre-configured spec for {@link TAFBulletin} to WMO COLLECT 1.2 XML String containing IWXXM 2.1 TAFs.
     */
    public static final ConversionSpecification<TAFBulletin, String> TAF_BULLETIN_POJO_TO_WMO_COLLECT_STRING = new ConversionSpecification<>(TAFBulletin.class,
            String.class, null, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF");

    /**
     * Pre-configured spec for {@link TAFBulletin} to WMO COLLECT 1.2 XML DOM document containing IWXXM 2.1 TAFs.
     */
    public static final ConversionSpecification<TAFBulletin, Document> TAF_BULLETIN_POJO_TO_WMO_COLLECT_DOM = new ConversionSpecification<>(TAFBulletin.class,
            Document.class, null, "XML/WMO COLLECT 1.2 + IWXXM 2.1 TAF");


    @Bean
    public AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMDOMSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMStringSerializer();
    }

    @Bean
    public AviMessageSpecificConverter<TAF, TAFType> tafIWXXMJAXBSerializer() {
        return new TAFIWXXMJAXBSerializer();
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
    public AviMessageSpecificConverter<TAFBulletin, Document> tafBulletinIWXXMDOMSerializer() {
        TAFBulletinIWXXMDOMSerializer retval = new TAFBulletinIWXXMDOMSerializer();
        retval.setMessageConverter(tafIWXXMJAXBSerializer());
        return retval;
    }

    @Bean
    public AviMessageSpecificConverter<TAFBulletin, String> tafBulletinIWXXMStringSerializer() {
        TAFBulletinIWXXMStringSerializer retval = new TAFBulletinIWXXMStringSerializer();
        retval.setMessageConverter(tafIWXXMJAXBSerializer());
        return retval;
    }

}
