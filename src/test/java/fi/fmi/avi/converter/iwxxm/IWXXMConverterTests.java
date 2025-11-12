package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.ConversionIssue;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public interface IWXXMConverterTests {
    String IWXXM_2_1_NAMESPACE = "http://icao.int/iwxxm/2.1";
    String IWXXM_3_0_NAMESPACE = "http://icao.int/iwxxm/3.0";
    String IWXXM_2025_2_NAMESPACE = "http://icao.int/iwxxm/2025-2";

    static String readResourceToString(final String fileName, final Class<? extends IWXXMConverterTests> referenceClass) throws IOException {
        try (final InputStream is = referenceClass.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
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
     * A dirty hack to unify all gml:id attribute values, to ignore id in comparison while converter produces non-repeatable random ids.
     *
     * @param iwxxm
     *         IWXXM document
     *
     * @return an IWXXM document with fixed gml:id attributes
     */
    static String setFixedGmlIds(final String iwxxm) {
        return iwxxm.replaceAll("\\b(xlink:href|gml:id)\\s*=\\s*\"(#)?([a-z]+[-.])?[^\"]*\"", "$1=\"$2$3fixed-gml-id\"");
    }

    static void assertXMLEqualsIgnoringVariables(final String input, final String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLAssert.assertXMLEqual(setFixedGmlIds(input), setFixedGmlIds(actual));
    }

    default String readResourceToString(final String fileName) throws IOException {
        final Class<? extends IWXXMConverterTests> referenceClass = getClass();
        return IWXXMConverterTests.readResourceToString(fileName, referenceClass);
    }
}
