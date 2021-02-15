package fi.fmi.avi.converter.iwxxm.v3_0;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;

public class TestHelper {
    private static final Pattern UUID_DIFFERENCE_PATTERN = Pattern.compile(
            "(((Expected\\sattribute\\svalue\\s)?(\\sbut\\swas\\s)?)('#?uuid.(([a-z0-9]*)-?){5}')){2}");
    private static final Pattern COORDINATE_FORMATTING_DIFFERENCE_PATTERN = Pattern.compile(
            "(((Expected\\stext\\svalue\\s)?(\\sbut\\swas\\s)?)('([\\-0-9.]*[\\s]?){10}')){2}");

    protected static String getXMLString(final String fileName) throws IOException{
        try (InputStream is = TestHelper.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    public static void printIssues(final List<ConversionIssue> issues) {
        if (issues.size() > 0) {
            for (final ConversionIssue item : issues) {
                System.out.println("********************************************************");
                System.out.println(item.getMessage());
            }
        }
    }

    public static void assertEqualsXML(final String input, final String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        final Diff xmlDiff = new Diff(input, actual);
        final DetailedDiff detailedDiff = new DetailedDiff(xmlDiff);

        @SuppressWarnings("unchecked")
        final String diffMessage = ((List<Difference>) detailedDiff.getAllDifferences()).stream()//
                .filter(difference -> !UUID_DIFFERENCE_PATTERN.matcher(difference.toString()).find() //
                        && !COORDINATE_FORMATTING_DIFFERENCE_PATTERN.matcher(difference.toString()).find())//
                .map(difference -> difference.getDescription() + "\n" + difference + "\n")//
                .collect(Collectors.joining("------------------------------------------------\n"));
        assertEquals("", diffMessage);
    }
}
