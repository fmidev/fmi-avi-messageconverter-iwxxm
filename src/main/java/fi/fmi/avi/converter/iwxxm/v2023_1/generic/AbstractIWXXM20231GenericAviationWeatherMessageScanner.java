package fi.fmi.avi.converter.iwxxm.v2023_1.generic;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Optional;

public abstract class AbstractIWXXM20231GenericAviationWeatherMessageScanner extends AbstractGenericAviationWeatherMessageScanner {

    protected static Optional<ConversionIssue> collectReportStatus(final Element element, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder) throws XPathExpressionException {
        final Optional<AviationWeatherMessage.ReportStatus> reportStatus = evaluateEnumeration(element, xpath, "@reportStatus",
                AviationWeatherMessage.ReportStatus.class);
        if (reportStatus.isPresent()) {
            builder.setReportStatus(reportStatus.get());
            return Optional.empty();
        } else {
            return Optional.of(new ConversionIssue(ConversionIssue.Severity.ERROR, "The report status could not be parsed"));
        }
    }

    protected static void collectIssueTime(final Element element, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder,
            final IssueList issues) throws XPathExpressionException {
        collectIssueTime(xpath, "./iwxxm2023_1:issueTime/gml:TimeInstant/gml:timePosition", element, builder, issues);
    }
}
