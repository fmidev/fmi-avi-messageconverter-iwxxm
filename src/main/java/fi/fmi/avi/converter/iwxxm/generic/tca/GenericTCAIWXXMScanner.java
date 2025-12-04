package fi.fmi.avi.converter.iwxxm.generic.tca;

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
import java.util.EnumMap;
import java.util.Map;

/**
 * Version-agnostic generic IWXXM scanner for Tropical Cyclone Advisory messages.
 */
public class GenericTCAIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public GenericTCAIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList issues = new IssueList();

        builder.setMessageType(MessageType.TROPICAL_CYCLONE_ADVISORY);

        collectOptionalReportStatus(featureElement, xpath, builder);
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                new EnumMap<>(GenericAviationWeatherMessage.LocationIndicatorType.class);
        fieldByLocationType.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                IWXXMField.ISSUING_CENTRE);
        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, issues);

        return issues;
    }
}
