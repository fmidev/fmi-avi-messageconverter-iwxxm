package fi.fmi.avi.converter.iwxxm.sigmet;

import java.io.ByteArrayInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;

/**
 * Specialization of {@link AbstractSIGMETIWXXMParser} for String input.
 */
public class SIGMETIWXXMStringParser extends AbstractSIGMETIWXXMParser<String> {

    @Override
    protected Document parseAsDom(final String input) throws ConversionException {
        Document retval = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
            retval = db.parse(bais);
        } catch (Exception e) {
            throw new ConversionException("Error in parsing input as to an XML document", e);
        }
        return retval;
    }
}
