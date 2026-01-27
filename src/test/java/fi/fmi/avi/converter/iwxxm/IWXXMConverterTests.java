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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Shared test helpers for IWXXM tests. Implement this interface to use the helpers.
 */
public interface IWXXMConverterTests {
    String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    String IWXXM_3_0_NAMESPACE = "http://icao.int/iwxxm/3.0";
    String IWXXM_2021_2_NAMESPACE = "http://icao.int/iwxxm/2021-2";
    String IWXXM_2023_1_NAMESPACE = "http://icao.int/iwxxm/2023-1";
    String IWXXM_2025_2_NAMESPACE = "http://icao.int/iwxxm/2025-2";

    ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    static Document readDocument(final InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        requireNonNull(inputStream, "inputStream");
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(inputStream);
    }

    /** Normalize gml:id, xlink:href, and volcanoId attributes for XML comparison. */
    static String setFixedGmlIds(final String iwxxm) {
        return iwxxm.replaceAll(
                "\\b(xlink:href|gml:id|(?:[A-Za-z_][\\w.-]*:)?volcanoId)\\s*=\\s*\"(#)?[^\"]*\"",
                "$1=\"$2fixed-gml-id\""
        );
    }

    /**
     * Assert XML equality, ignoring whitespace, comments, and variable IDs.
     */
    static void assertXMLEqualsIgnoringVariables(final String expected, final String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLAssert.assertXMLEqual(setFixedGmlIds(expected), setFixedGmlIds(actual));
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
        try (final InputStream stream = getClass().getResourceAsStream(resourceName)) {
            requireNonNull(stream, resourceName);
            return IOUtils.toString(stream, "UTF-8");
        }
    }

    default <T> T readFromJSON(final String resourceName, final Class<T> targetType) throws IOException {
        requireNonNull(resourceName, "resourceName");
        requireNonNull(targetType, "targetType");
        try (final InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                return OBJECT_MAPPER.readValue(inputStream, targetType);
            }
            throw new FileNotFoundException("Resource '" + resourceName + "' could not be loaded");
        }
    }
}
