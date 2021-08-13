package fi.fmi.avi.converter.iwxxm;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public interface GenericAviationWeatherMessageScanner {
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder) throws
            XPathExpressionException;
}
