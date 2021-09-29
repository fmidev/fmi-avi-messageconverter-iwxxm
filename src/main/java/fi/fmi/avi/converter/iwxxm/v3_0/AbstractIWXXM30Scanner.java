package fi.fmi.avi.converter.iwxxm.v3_0;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import javax.xml.datatype.XMLGregorianCalendar;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.immutable.WeatherImpl;
import icao.iwxxm30.AerodromeForecastWeatherType;
import icao.iwxxm30.ReportType;

public class AbstractIWXXM30Scanner extends AbstractIWXXMScanner {

    public static IssueList collectReportMetadata(final ReportType input, final GenericReportProperties properties, final ConversionHints hints) {
        final IssueList retval = new IssueList();

        //Issues for the permissibleUsage and reportStatus reported already by XML Schema or Schematron validation, so not checking them here:
        if (input.getReportStatus() != null) {
            properties.set(GenericReportProperties.Name.REPORT_STATUS, AviationWeatherMessage.ReportStatus.valueOf(input.getReportStatus().name()));
        }
        if (input.getPermissibleUsage() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.valueOf(input.getPermissibleUsage().name()));
        }
        if (input.getPermissibleUsageReason() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON,
                    AviationCodeListUser.PermissibleUsageReason.valueOf(input.getPermissibleUsageReason().name()));
        }
        properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, input.getPermissibleUsageSupplementary());
        properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, input.getTranslatedBulletinID());

        XMLGregorianCalendar cal = input.getTranslatedBulletinReceptionTime();
        if (cal != null) {
            final ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, time);
        }

        cal = input.getTranslationTime();
        if (cal != null) {
            final ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATION_TIME, time);
        }

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());

        if (!properties.contains(GenericReportProperties.Name.TRANSLATION_TIME)) {
            if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)) {
                final Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
                if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.now());
                } else if (value instanceof ZonedDateTime) {
                    properties.set(GenericReportProperties.Name.TRANSLATION_TIME, value);
                }
            }
        }
        return retval;
    }

    public static void withWeatherBuilderFor(final AerodromeForecastWeatherType weather, final ConversionHints hints,
            final Consumer<WeatherImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        if (weather.getNilReason().isEmpty()) {
            withWeatherBuilderFor(weather.getHref(), weather.getTitle(), hints, resultHandler, issueHandler);
        }
    }
}
