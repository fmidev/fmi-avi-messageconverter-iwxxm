package fi.fmi.avi.converter.iwxxm.generic;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class IWXXM30GenericAviationWeatherMessageParser<T> extends AbstractIWXXMParser<T, GenericAviationWeatherMessage> {

    protected static ConversionIssue collectReportStatus(final Element element, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder) throws
            XPathExpressionException {
        try {
            builder.setReportStatus(AviationWeatherMessage.ReportStatus.valueOf(xpath.compile("@reportStatus").evaluate(element)));
        } catch (IllegalArgumentException e) {
            return new ConversionIssue(ConversionIssue.Severity.ERROR, "The report status could not be parsed");
        }
        return null;
    }

}
