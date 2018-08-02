package fi.fmi.avi.converter.iwxxm;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class DOMParsingTestBase {

    protected static Document readDocument(final String name) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(TAFIWXXMParserTest.class.getResourceAsStream(name));
    }
}
