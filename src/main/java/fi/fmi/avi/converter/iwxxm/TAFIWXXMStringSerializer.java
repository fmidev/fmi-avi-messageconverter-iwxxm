package fi.fmi.avi.converter.iwxxm;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm21.TAFType;

public class TAFIWXXMStringSerializer extends AbstractTAFIWXXMSerializer<String> {

    public TAFIWXXMStringSerializer() {
    }

    @Override
    protected String render(TAFType taf, ConversionHints hints) throws ConversionException {
        return renderXMLString(taf, hints);
    }

    private String renderXMLString(final TAFType tafElem, final ConversionHints hints) throws ConversionException {
        Document result = renderXMLDocument(tafElem, hints);
        String retval = null;
        if (result != null) {
            try {
                StringWriter sw = new StringWriter();
                Result output = new StreamResult(sw);
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();

                //TODO: switch these on based on the ConversionHints:
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                DOMSource dsource = new DOMSource(result);
                transformer.transform(dsource, output);
                retval = sw.toString();
            } catch (TransformerException e) {
              throw new ConversionException("Exception in rendering to String", e);
            }
        }
        return retval;
    }

}
