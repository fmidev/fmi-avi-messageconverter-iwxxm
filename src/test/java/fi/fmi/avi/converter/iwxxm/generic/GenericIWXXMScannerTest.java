package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterTests;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericIWXXMScannerTest implements IWXXMConverterTests {

    private Element rootElement;
    private XPath xpath;

    @Before
    public void setUp() throws Exception {
        final Document document = readDocumentFromResource("taf/iwxxm-30-taf-A5-1.xml");
        rootElement = document.getDocumentElement();
        xpath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void first_expression_fails_second_succeeds() {
        final FieldXPathProvider fieldProvider = field -> {
            if (field == IWXXMField.ISSUE_TIME) {
                return Arrays.asList(
                        "this[is[invalid[xpath",
                        XPathBuilder.text("./iwxxm:issueTime/gml:TimeInstant/gml:timePosition")
                );
            }
            if (field == IWXXMField.GML_ID) {
                return Collections.singletonList(XPathBuilder.text("@gml:id"));
            }
            return Collections.emptyList();
        };

        final GenericIWXXMScanner scanner = GenericIWXXMScanner.builder()
                .fieldProvider(fieldProvider)
                .build();

        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        final IssueList issues = scanner.collectMessage(rootElement, xpath, builder);

        assertThat(builder.getIssueTime()).isPresent();
        assertThat(builder.getGmlId()).isPresent();
        assertThat(issues).isEmpty();
    }

    @Test
    public void first_expression_empty_second_succeeds() {
        final FieldXPathProvider fieldProvider = field -> {
            if (field == IWXXMField.ISSUE_TIME) {
                return Arrays.asList(
                        XPathBuilder.text("./iwxxm:nonExistent/gml:TimeInstant/gml:timePosition"),
                        XPathBuilder.text("./iwxxm:issueTime/gml:TimeInstant/gml:timePosition")
                );
            }
            if (field == IWXXMField.GML_ID) {
                return Collections.singletonList(XPathBuilder.text("@gml:id"));
            }
            return Collections.emptyList();
        };

        final GenericIWXXMScanner scanner = GenericIWXXMScanner.builder()
                .fieldProvider(fieldProvider)
                .build();

        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        final IssueList issues = scanner.collectMessage(rootElement, xpath, builder);

        assertThat(builder.getIssueTime()).isPresent();
        assertThat(builder.getGmlId()).isPresent();
        assertThat(issues).isEmpty();
    }

    @Test
    public void all_empty_reports_missing_data() {
        final FieldXPathProvider fieldProvider = field -> {
            if (field == IWXXMField.ISSUE_TIME) {
                return Arrays.asList(
                        XPathBuilder.text("./iwxxm:nonExistent1/gml:TimeInstant/gml:timePosition"),
                        XPathBuilder.text("./iwxxm:nonExistent2/gml:TimeInstant/gml:timePosition")
                );
            }
            if (field == IWXXMField.GML_ID) {
                return Collections.singletonList(XPathBuilder.text("@gml:id"));
            }
            return Collections.emptyList();
        };

        final GenericIWXXMScanner scanner = GenericIWXXMScanner.builder()
                .fieldProvider(fieldProvider)
                .build();

        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        final IssueList issues = scanner.collectMessage(rootElement, xpath, builder);

        assertThat(builder.getIssueTime()).isEmpty();
        assertThat(issues.stream()
                .filter(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA)
                .filter(issue -> issue.getMessage().contains("issue time")))
                .hasSize(1);
    }

    @Test
    public void exceptions_collected_as_suppressed() {
        final FieldXPathProvider fieldProvider = field -> {
            if (field == IWXXMField.ISSUE_TIME) {
                return Arrays.asList(
                        "this[is[invalid[xpath1",
                        "this[is[invalid[xpath2",
                        "this[is[invalid[xpath3"
                );
            }
            if (field == IWXXMField.GML_ID) {
                return Collections.singletonList(XPathBuilder.text("@gml:id"));
            }
            return Collections.emptyList();
        };

        final GenericIWXXMScanner scanner = GenericIWXXMScanner.builder()
                .fieldProvider(fieldProvider)
                .build();

        final GenericAviationWeatherMessageImpl.Builder builder = GenericAviationWeatherMessageImpl.builder();
        final IssueList issues = scanner.collectMessage(rootElement, xpath, builder);

        assertThat(builder.getIssueTime()).isEmpty();

        assertThat(issues)
                .extracting(ConversionIssue::getCause)
                .filteredOn(Objects::nonNull)
                .first()
                .satisfies(cause -> assertThat(cause.getSuppressed()).hasSize(2));
    }
}
