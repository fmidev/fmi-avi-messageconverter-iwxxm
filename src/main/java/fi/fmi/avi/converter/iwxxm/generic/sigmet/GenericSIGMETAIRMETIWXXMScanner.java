package fi.fmi.avi.converter.iwxxm.generic.sigmet;

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
 * Version-agnostic generic IWXXM scanner for SIGMET and AIRMET messages.
 */
public class GenericSIGMETAIRMETIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public GenericSIGMETAIRMETIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList issues = new IssueList();

        // Determine message type (SIGMET vs AIRMET) based on root local-name
        final String localName = featureElement.getLocalName();
        if ("AIRMET".equals(localName)) {
            builder.setMessageType(MessageType.AIRMET);
        } else {
            builder.setMessageType(MessageType.SIGMET);
        }

        collectReportStatus(featureElement, xpath, builder, issues);

        // Issue time via provider
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        // Validity time via provider
        collectValidityTimeUsingFieldProvider(featureElement, xpath, builder, issues);

        // Location indicators via provider: MWO, ATS unit, ATS region
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                new EnumMap<>(GenericAviationWeatherMessage.LocationIndicatorType.class);
        fieldByLocationType.put(GenericAviationWeatherMessage.LocationIndicatorType.ORIGINATING_METEOROLOGICAL_WATCH_OFFICE,
                IWXXMField.ORIGINATING_MWO);
        fieldByLocationType.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_UNIT,
                IWXXMField.ISSUING_ATS_UNIT);
        fieldByLocationType.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_AIR_TRAFFIC_SERVICES_REGION,
                IWXXMField.ISSUING_ATS_REGION);
        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, issues);

        return issues;
    }
}
