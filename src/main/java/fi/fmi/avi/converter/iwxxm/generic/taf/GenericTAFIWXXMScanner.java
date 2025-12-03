package fi.fmi.avi.converter.iwxxm.generic.taf;

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
import java.util.Map;

/**
 * Version-agnostic generic IWXXM scanner for TAF messages.
 */
public class GenericTAFIWXXMScanner extends AbstractGenericAviationWeatherMessageScanner {

    public GenericTAFIWXXMScanner(final FieldXPathProvider fieldXPathProvider) {
        super(fieldXPathProvider);
    }

    @Override
    public IssueList collectMessage(final Element featureElement,
                                    final XPath xpath,
                                    final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        final IssueList retval = new IssueList();

        builder.setMessageType(MessageType.TAF);

        collectStatus(featureElement, xpath, builder, retval);
        collectIssueTimeUsingFieldProvider(featureElement, xpath, builder, retval);

        // Validity time via field-centric provider (3.0 validPeriod, 3.0 cancellation,
        // then 2.1 validTime), configured in TAFFieldXPathProvider.
        collectValidityTimeUsingFieldProvider(featureElement, xpath, builder, retval);

        // AERODROME location indicator via field-centric provider
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, IWXXMField> fieldByLocationType =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                        IWXXMField.AERODROME);
        collectLocationIndicatorsUsingFieldProvider(featureElement, xpath, builder, fieldByLocationType, retval);

        return retval;
    }

    private void collectStatus(final Element element,
                               final XPath xpath,
                               final GenericAviationWeatherMessageImpl.Builder builder,
                               final IssueList issues) throws XPathExpressionException {
        // IWXXM 3.0+ uses @reportStatus on the root TAF element.
        final fi.fmi.avi.model.AviationWeatherMessage.ReportStatus reportStatus =
                evaluateEnumeration(element, xpath, "@reportStatus",
                        fi.fmi.avi.model.AviationWeatherMessage.ReportStatus.class).orElse(null);
        if (reportStatus != null) {
            builder.setReportStatus(reportStatus);
            return;
        }

        // IWXXM 2.1 used @status with TAFStatus; keep that as a fallback.
        final AviationCodeListUser.TAFStatus tafStatus =
                evaluateEnumeration(element, xpath, "@status", AviationCodeListUser.TAFStatus.class).orElse(null);
        if (tafStatus == null) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        } else {
            builder.setReportStatus(tafStatus.getReportStatus());
        }
    }
}
