package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AerodromeMessageIWXXMSerializerBase.ConverterValidationEventHandler;
import fi.fmi.avi.converter.iwxxm.AerodromeMessageIWXXMSerializerBase.IWXXMNamespaceMapper;
import fi.fmi.avi.converter.iwxxm.AerodromeMessageIWXXMSerializerBase.IWXXMSchemaResourceResolver;
import icao.iwxxm21.TAFType;

public abstract class AbstractIWXXMSerializer<T> extends IWXXMConverterBase {

    public AbstractIWXXMSerializer() {
    }

    @SuppressWarnings("unchecked")
    protected <S> Document renderXMLDocument(final S input, final ConversionHints hints) throws ConversionException {
        StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://icao.int/iwxxm/2.1 https://schemas.wmo.int/iwxxm/2.1/iwxxm.xsd http://def.wmo.int/metce/2013 http://schemas.wmo.int/metce/1.2/metce.xsd http://www.opengis.net/samplingSpatial/2.0 http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd");
            marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IWXXMNamespaceMapper());
            marshaller.marshal(wrap(input, (Class<S>)input.getClass()), sw);
            return asCleanedUpXML(sw, hints);
        } catch (JAXBException e) {
             throw new ConversionException("Exception in rendering to DOM", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <S> boolean validateDocument(final S input, final ConversionHints hints, final ConversionResult<T> result) throws ConversionException {
        try {
            //XML Schema validation:
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            IWXXMSchemaResourceResolver resolver = IWXXMSchemaResourceResolver.getInstance();
            schemaFactory.setResourceResolver(resolver);
            //Secure processing does not allow "file" protocol loading for schemas:
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema iwxxmSchema = schemaFactory.newSchema(TAFType.class.getResource("/int/icao/iwxxm/2.1/iwxxm.xsd"));
            Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://icao.int/iwxxm/2.1 https://schemas.wmo.int/iwxxm/2.1/iwxxm.xsd http://def.wmo.int/metce/2013 http://schemas.wmo.int/metce/1.2/metce.xsd http://www.opengis.net/samplingSpatial/2.0 http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd");
            marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IWXXMNamespaceMapper());
            marshaller.setSchema(iwxxmSchema);
            ConverterValidationEventHandler<T> eventHandler = new ConverterValidationEventHandler<T>(result);
            marshaller.setEventHandler(eventHandler);           
            //Marshall to run the validation:
            marshaller.marshal(wrap(input, (Class<S>)input.getClass()), new DefaultHandler());
          
            if (eventHandler.errorsFound()) {
                return false;
            } else {
                return true;
            }
            
        } catch (Exception e) {
            throw new ConversionException("Exception in validation", e);
        }
    }

    protected Document asCleanedUpXML(final StringWriter input, final ConversionHints hints) throws ConversionException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(input.toString()));
            Document dom3Doc = db.parse(is);
            DOMResult cleanedResult = new DOMResult();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(this.getCleanupTransformationStylesheet(hints));
            DOMSource dsource = new DOMSource(dom3Doc);
            transformer.transform(dsource, cleanedResult);
            return (Document) cleanedResult.getNode();
        } catch (ParserConfigurationException|SAXException|IOException|TransformerException e) {
            throw new ConversionException("Exception in cleaning up", e);
        }
    }
    
    protected abstract Source getCleanupTransformationStylesheet(final ConversionHints hints);
 
}
