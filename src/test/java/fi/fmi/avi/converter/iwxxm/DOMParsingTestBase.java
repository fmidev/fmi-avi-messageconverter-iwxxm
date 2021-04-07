package fi.fmi.avi.converter.iwxxm;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class DOMParsingTestBase {

    protected static Document readDocument(final Class<?> clz, final String name) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (InputStream inputStream = clz.getResourceAsStream(name)) {
            return documentBuilder.parse(inputStream);
        }
    }
}
