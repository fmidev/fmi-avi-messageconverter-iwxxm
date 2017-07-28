package fi.fmi.avi.converter.iwxxm.conf;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.TAFIWXXMDOMSerializer;
import fi.fmi.avi.converter.iwxxm.TAFIWXXMStringSerializer;
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


    @Bean
    public AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer() {
        return new TAFIWXXMDOMSerializer();
    }
    
    @Bean
    public AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer() {
        return new TAFIWXXMStringSerializer();
    }
    

}
