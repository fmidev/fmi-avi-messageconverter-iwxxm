package fi.fmi.avi.converter.iwxxm.generic;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

public interface GenericAviationWeatherMessageScanner {
    IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder) throws
            XPathExpressionException;
}
