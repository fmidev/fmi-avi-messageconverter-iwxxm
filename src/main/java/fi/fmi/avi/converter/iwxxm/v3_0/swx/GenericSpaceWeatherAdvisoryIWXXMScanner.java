package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.v3_0.generic.AbstractIWXXM30GenericAviationWeatherMessageScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericSpaceWeatherAdvisoryIWXXMScanner extends AbstractIWXXM30GenericAviationWeatherMessageScanner {
    private static final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> LOCATION_INDICATOR_EXPRESSIONS;

    static {
        final Map<GenericAviationWeatherMessage.LocationIndicatorType, String> speci30LocationIndicatorExpressions = new EnumMap<>(
                GenericAviationWeatherMessage.LocationIndicatorType.class);
        speci30LocationIndicatorExpressions.put(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                "iwxxm30:issuingSpaceWeatherCentre/aixm:Unit/aixm:timeSlice/aixm:UnitTimeSlice/aixm:name");

        LOCATION_INDICATOR_EXPRESSIONS = Collections.unmodifiableMap(speci30LocationIndicatorExpressions);
    }

    @Override
    public IssueList collectMessage(final Element featureElement, final XPath xpath, final GenericAviationWeatherMessageImpl.Builder builder)
            throws XPathExpressionException {
        builder.setMessageType(MessageType.SPACE_WEATHER_ADVISORY);
        final IssueList retval = new IssueList();
        collectReportStatus(featureElement, xpath, builder).ifPresent(issue -> retval.add(issue));
        collectIssueTime(featureElement, xpath, builder, retval);

        collectLocationIndicators(featureElement, xpath, builder, LOCATION_INDICATOR_EXPRESSIONS, retval);

        retval.addAll(collectValidTime(featureElement, "./iwxxm:validPeriod[1]", xpath, builder));

        return retval;
    }
}

