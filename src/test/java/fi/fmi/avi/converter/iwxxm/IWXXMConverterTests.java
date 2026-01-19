package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.ConversionIssue;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Objects.requireNonNull;

public interface IWXXMConverterTests {
    String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    String IWXXM_3_0_NAMESPACE = "http://icao.int/iwxxm/3.0";
    String IWXXM_2021_2_NAMESPACE = "http://icao.int/iwxxm/2021-2";
    String IWXXM_2023_1_NAMESPACE = "http://icao.int/iwxxm/2023-1";
    String IWXXM_2025_2_NAMESPACE = "http://icao.int/iwxxm/2025-2";

    static Document readDocumentFromResource(final String resourceName, final Class<? extends IWXXMConverterTests> resourceReference) throws IOException, ParserConfigurationException, SAXException {
        requireNonNull(resourceName, "resourceName");
        requireNonNull(resourceReference, "resourceReference");
        try (final InputStream inputStream = resourceReference.getResourceAsStream(resourceName)) {
            requireNonNull(inputStream, resourceName);
            return readDocument(inputStream);
        }
    }

    static Document readDocumentFromString(final String xmlString) throws Exception {
        requireNonNull(xmlString, "xmlString");
        try (final InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            return readDocument(inputStream);
        }
    }

    static Document readDocument(final InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        requireNonNull(inputStream, "inputStream");
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputStream);
    }

    static String readResourceToString(final String resourceName, final Class<? extends IWXXMConverterTests> resourceReference) throws IOException {
        requireNonNull(resourceName, "resourceName");
        requireNonNull(resourceReference, "resourceReference");
        try (final InputStream resourceStream = resourceReference.getResourceAsStream(resourceName)) {
            requireNonNull(resourceStream, resourceName);
            return IOUtils.toString(resourceStream, "UTF-8");
        }
    }

    static void printIssues(final List<ConversionIssue> issues) {
        if (!issues.isEmpty()) {
            for (final ConversionIssue item : issues) {
                System.out.println("********************************************************");
                System.out.println(item.getMessage());
            }
        }
    }

    /**
     * Set fixed id values to ignore ids in test comparisons while converter produces non-repeatable random ids.
     *
     * @param iwxxm IWXXM document
     * @return an IWXXM document with fixed gml:id attributes
     */
    static String setFixedGmlIds(final String iwxxm) {
        return iwxxm.replaceAll(
                "\\b(xlink:href|gml:id|(?:[A-Za-z_][\\w.-]*:)?volcanoId)\\s*=\\s*\"(#)?[^\"]*\"",
                "$1=\"$2fixed-gml-id\""
        );
    }

    static void assertXMLEqualsIgnoringVariables(final String input, final String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLAssert.assertXMLEqual(setFixedGmlIds(input), setFixedGmlIds(actual));
    }

    default Document readDocumentFromResource(final String name) throws IOException, ParserConfigurationException, SAXException {
        return IWXXMConverterTests.readDocumentFromResource(name, getClass());
    }

    default String readResourceToString(final String fileName) throws IOException {
        return IWXXMConverterTests.readResourceToString(fileName, getClass());
    }
}
