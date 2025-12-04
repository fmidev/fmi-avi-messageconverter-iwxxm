package fi.fmi.avi.converter.iwxxm.generic.vaa;

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
 * Version-agnostic generic IWXXM scanner for Volcanic Ash Advisory messages.
 */
public class GenericVAAIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public GenericVAAIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList issues = new IssueList();

        // Message type is always VOLCANIC_ASH_ADVISORY for these roots
        builder.setMessageType(MessageType.VOLCANIC_ASH_ADVISORY);

        // For VAA, IWXXM 3.x uses @reportStatus; IWXXM 2.1 may not. We are lenient and
        // do not add an error if status is missing.
        collectOptionalReportStatus(featureElement, xpath, builder);

        // Issue time via provider (root issueTime covers both 2.1 and 3.x)
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        // Location indicators: only ISSUING_CENTRE for VAA
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                new EnumMap<>(GenericAviationWeatherMessage.LocationIndicatorType.class);
        fieldByLocationType.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                IWXXMField.ISSUING_CENTRE);
        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, issues);

        return issues;
    }
}
