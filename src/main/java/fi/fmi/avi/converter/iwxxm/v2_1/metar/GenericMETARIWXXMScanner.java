package fi.fmi.avi.converter.iwxxm.v2_1.metar;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractIWXXM21METARSPECIScanner;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericMETARIWXXMScanner extends AbstractIWXXM21METARSPECIScanner {

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.METAR);
        final IssueList retval = new IssueList();
        collectStatus(featureElement, xpath, builder, retval);
        collectIssueTime(xpath, featureElement, builder, retval);
        collectLocationIndicators(featureElement, xpath, builder, retval);

        return retval;
    }

}
