package fi.fmi.avi.converter.iwxxm.generic;

import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class AbstractIWXXM30GenericAviationWeatherMessageScanner extends AbstractGenericAviationWeatherMessageScanner {

    protected static Optional<ConversionIssue> collectReportStatus(final Element element, final XPath xpath,
            final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        Optional<ConversionIssue> retval = Optional.empty();
            Optional<AviationWeatherMessage.ReportStatus> reportStatus = evaluateEnumeration(element, xpath, "@reportStatus", AviationWeatherMessage.ReportStatus.class);
            if(reportStatus.isPresent()) {
                builder.setReportStatus(reportStatus.get());
            } else {
                Optional.of(new ConversionIssue(ConversionIssue.Severity.ERROR, "The report status could not be parsed"));
            }

        return retval;
    }

    protected static void collectIssueTime(final Element element, final XPath xpath, GenericAviationWeatherMessageImpl.Builder builder, IssueList issues) throws XPathExpressionException {
        collectIssueTime(xpath, "./iwxxm30:issueTime/gml:TimeInstant/gml:timePosition", element, builder, issues);
    }
}
