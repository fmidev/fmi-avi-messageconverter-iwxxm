package fi.fmi.avi.converter.iwxxm.generic.swx;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.Map;

/**
 * Version-agnostic generic IWXXM scanner for Space Weather Advisories.
 */
public class GenericSWXIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public GenericSWXIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.SPACE_WEATHER_ADVISORY);
        final IssueList retval = new IssueList();

        collectReportStatus(featureElement, xpath, builder, retval);
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, retval);

        // Map location indicators -> semantic fields for this message type
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                        IWXXMField.ISSUING_CENTRE);

        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, retval);

        return retval;
    }
}
