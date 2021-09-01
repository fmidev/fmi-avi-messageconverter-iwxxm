package fi.fmi.avi.converter.iwxxm.generic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public abstract class AbstractIWXXM21METARSPECIScanner extends AbstractGenericAviationWeatherMessageScanner {

    protected static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> METAR_SPECI_LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> metarSpeciLocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        metarSpeciLocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "iwxxm:observation/om:OM_Observation/om"
                + ":featureOfInterest/sams:SF_SpatialSamplingFeature/sam:sampledFeature/aixm:AirportHeliport/aixm:timeSlice/aixm:AirportHeliportTimeSlice/aixm"
                + ":designator");
        METAR_SPECI_LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(metarSpeciLocationIndicatorExpressions);
    }

    protected void collectStatus(Element element, XPath xpath, GenericAviationWeatherMessageImpl.Builder builder, IssueList issues)
            throws XPathExpressionException {
        Optional<AviationCodeListUser.MetarStatus> status = evaluateEnumeration(element, xpath, "@status", AviationCodeListUser.MetarStatus.class);
        if (status.isPresent()) {
            builder.setReportStatus(status.get().getReportStatus());
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "status could not be parsed"));
        }
    }
    protected void collectIssueTime(XPath xpath, Element element, GenericAviationWeatherMessageImpl.Builder builder, IssueList issues) throws  XPathExpressionException {
        collectIssueTime(xpath, "./iwxxm:observation/om:OM_Observation/om:phenomenonTime/gml:TimeInstant/gml:timePosition", element, builder, issues);
    }

    protected void collectLocationIndicators(Element element, XPath xpath, GenericAviationWeatherMessageImpl.Builder builder, IssueList issues) throws XPathExpressionException {
        collectLocationIndicators(element, xpath, builder, METAR_SPECI_LOCATION_INDICATOR_EXPRESSIONS, issues);
    }

}
