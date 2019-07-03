package fi.fmi.avi.converter.iwxxm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.bulletin.GenericBulletinIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.bulletin.GenericBulletinIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.bulletin.TAFBulletinIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.bulletin.TAFBulletinIWXXMStringSerializer;
import fi.fmi.avi.converter.iwxxm.metar.METARIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.metar.METARIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.metar.SPECIIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.metar.SPECIIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMJAXBSerializer;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringParser;
import fi.fmi.avi.converter.iwxxm.taf.TAFIWXXMStringSerializer;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
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
     * Pre-configured spec for IWXXM 2.1 XML format METAR document String to {@link METAR}.
     */
    public static final ConversionSpecification<String, METAR> IWXXM21_STRING_TO_METAR_POJO = new ConversionSpecification<>(String.class, METAR.class,
            "METAR, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format METAR document DOM Node to {@link SPECI}.
     */
    public static final ConversionSpecification<Document, METAR> IWXXM21_DOM_TO_METAR_POJO = new ConversionSpecification<>(Document.class, METAR.class,
            "METAR, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format SPECI document String to {@link SPECI}.
     */
    public static final ConversionSpecification<String, SPECI> IWXXM21_STRING_TO_SPECI_POJO = new ConversionSpecification<>(String.class, SPECI.class,
            "SPECI, XML/IWXXM 2.1", null);

    /**
     * Pre-configured spec for IWXXM 2.1 XML format METAR document DOM Node to {@link TAF}.
     */
    public static final ConversionSpecification<Document, SPECI> IWXXM21_DOM_TO_SPECI_POJO = new ConversionSpecification<>(Document.class, SPECI.class,
            "SPECI, XML/IWXXM 2.1", null);
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

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML DOM document to {@link GenericMeteorologicalBulletin}
     */
    public static final ConversionSpecification<Document, GenericMeteorologicalBulletin> IWXXM21_DOM_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(
            Document.class, GenericMeteorologicalBulletin.class, "XML/WMO COLLECT 1.2", null);

    /**
     * Pre-configured spec for WMO COLLECT 1.2 XML document String to {@link GenericMeteorologicalBulletin}
     */
    public static final ConversionSpecification<String, GenericMeteorologicalBulletin> IWXXM21_STRING_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(
            String.class, GenericMeteorologicalBulletin.class, "XML/WMO COLLECT 1.2", null);

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
    public AviMessageSpecificConverter<String, METAR> metarIWXXMStringParser() {
        return new METARIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, METAR> metarIWXXMDOMParser() {
        return new METARIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<String, SPECI> speciIWXXMStringParser() {
        return new SPECIIWXXMStringParser();
    }

    @Bean
    public AviMessageSpecificConverter<Document, SPECI> speciIWXXMDOMParser() {
        return new SPECIIWXXMDOMParser();
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

    @Bean
    public AviMessageSpecificConverter<Document, GenericMeteorologicalBulletin> genericBulletinIWXXMDOMParser() {
        return new GenericBulletinIWXXMDOMParser();
    }

    @Bean
    public AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinIWXXMStringParser() {
        return new GenericBulletinIWXXMStringParser();
    }

}
