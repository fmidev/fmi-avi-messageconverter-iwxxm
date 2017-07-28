package fi.fmi.avi.converter.iwxxm.conf;

import javax.xml.bind.JAXBException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.TAFIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.TAFIWXXMStringSerializer;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
@Configuration
public class IWXXMConverterConfig {
    /**
     * Pre-configured spec for IWXXM 2.1 XML format METAR document String to {@link METAR} POJO.
     */
    public static final ConversionSpecification<String, METAR> IWXXM21_TO_METAR_POJO = new ConversionSpecification<>(String.class, METAR.class,
            "METAR, XML/IWXXM 2.1",null);

    /**
     * Pre-configured spec for {@link METAR} to IWXXM 2.1 XML format METAR document String.
     */
    public static final ConversionSpecification<METAR, String> METAR_POJO_TO_IWXXM21 = new ConversionSpecification<>(METAR.class, String.class,
            null, "METAR, XML/IWXXM 2.1");

    /**
     * Pre-configured spec for IWXXM 2.1 XML format TAF document String format to {@link TAF} POJO.
     */
    public static final ConversionSpecification<String, TAF> IWXXM21_TO_TAF_POJO = new ConversionSpecification<>(String.class, TAF.class,
            "TAF, XML/IWXXM 2.1",null);

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


    @Bean
    public AviMessageConverter aviMessageConverter() throws JAXBException {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TAF_POJO_TO_IWXXM21_DOM,new TAFIWXXMDOMSerializer());
        p.setMessageSpecificConverter(TAF_POJO_TO_IWXXM21_STRING,new TAFIWXXMStringSerializer());
        return p;
    }

}
