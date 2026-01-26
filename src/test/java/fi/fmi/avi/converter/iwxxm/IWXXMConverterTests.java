package fi.fmi.avi.converter.iwxxm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public interface IWXXMConverterTests {
    String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    String IWXXM_3_0_NAMESPACE = "http://icao.int/iwxxm/3.0";
    String IWXXM_2021_2_NAMESPACE = "http://icao.int/iwxxm/2021-2";
    String IWXXM_2023_1_NAMESPACE = "http://icao.int/iwxxm/2023-1";
    String IWXXM_2025_2_NAMESPACE = "http://icao.int/iwxxm/2025-2";

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

    default Document readDocumentFromResource(final String resourceName) throws IOException, ParserConfigurationException, SAXException {
        requireNonNull(resourceName, "resourceName");
        try (final InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
            requireNonNull(inputStream, resourceName);
            return readDocument(inputStream);
        }
    }

    default String readResourceToString(final String resourceName) throws IOException {
        requireNonNull(resourceName, "resourceName");
        try (final InputStream resourceStream = getClass().getResourceAsStream(resourceName)) {
            requireNonNull(resourceStream, resourceName);
            return IOUtils.toString(resourceStream, "UTF-8");
        }
    }

    default <T> T readFromJSON(final String resourceName, final Class<T> targetType) throws IOException {
        requireNonNull(resourceName, "resourceName");
        requireNonNull(targetType, "targetType");
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        try (final InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, targetType);
            } else {
                throw new FileNotFoundException("Resource '" + resourceName + "' could not be loaded");
            }
        }
    }
}
