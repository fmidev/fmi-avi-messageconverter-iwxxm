package fi.fmi.avi.converter.iwxxm.generic.metar;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.generic.AbstractGenericAviationWeatherMessageScanner;
import fi.fmi.avi.converter.iwxxm.generic.FieldXPathProvider;
import fi.fmi.avi.converter.iwxxm.generic.IWXXMField;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Version-agnostic generic IWXXM scanner for METAR and SPECI messages.
 */
public class GenericMETARSPECIIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {
    private static final Map<String, MessageType> MESSAGE_TYPES_BY_ELEMENT_NAME;

    static {
        final Map<String, MessageType> map = new HashMap<>();
        map.put("METAR", MessageType.METAR);
        map.put("SPECI", MessageType.SPECI);
        MESSAGE_TYPES_BY_ELEMENT_NAME = Collections.unmodifiableMap(map);
    }

    public GenericMETARSPECIIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();

        final MessageType messageType = MESSAGE_TYPES_BY_ELEMENT_NAME.get(featureElement.getLocalName());
        if (messageType == null) {
            retval.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                    "Document element is not METAR or SPECI");
            return retval;
        }
        builder.setMessageType(messageType);

        collectStatus(featureElement, xpath, builder, retval);
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, retval);

        // Collect AERODROME location indicator via field-centric provider
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                        IWXXMField.AERODROME);
        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, retval);

        return retval;
    }

    protected void collectStatus(final Element element,
                                 final XPath xpath,
                                 final GenericAviationWeatherMessageImpl.Builder builder,
                                 final IssueList issues)
            throws XPathExpressionException {
        // Prefer IWXXM-style @reportStatus first
        final fi.fmi.avi.model.AviationWeatherMessage.ReportStatus reportStatus =
                evaluateEnumeration(element, xpath, "@reportStatus",
                        fi.fmi.avi.model.AviationWeatherMessage.ReportStatus.class).orElse(null);
        if (reportStatus != null) {
            builder.setReportStatus(reportStatus);
            return;
        }

        // Fallback to legacy METAR 2.1 @status mapping
        final AviationCodeListUser.MetarStatus metarStatus =
                evaluateEnumeration(element, xpath, "@status", AviationCodeListUser.MetarStatus.class).orElse(null);
        if (metarStatus == null) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        } else {
            builder.setReportStatus(metarStatus.getReportStatus());
        }
    }
}
