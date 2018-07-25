package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;

import javax.xml.datatype.XMLGregorianCalendar;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.AviationCodeListUser;
import icao.iwxxm21.ReportType;

/**
 * Common functionality for parsing validation of IWXXM messages.
 */
public abstract class AbstractIWXXMScanner extends IWXXMConverterBase {

    protected static IssueList collectReportMetadata(final ReportType input, final GenericReportProperties properties, final ConversionHints hints) {
        IssueList retval = new IssueList();

        //Issues for these reported already by XML Schema or Schematron validation, so not checking them here:
        if (input.getPermissibleUsage() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.valueOf(input.getPermissibleUsage().name()));
        }
        if (input.getPermissibleUsageReason() != null) {
            properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.valueOf(input.getPermissibleUsageReason().name()));
        }
        properties.set(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, input.getPermissibleUsageSupplementary());
        properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, input.getTranslatedBulletinID());

        XMLGregorianCalendar cal = input.getTranslatedBulletinReceptionTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, time);
        }

        cal = input.getTranslationTime();
        if (cal != null) {
            ZonedDateTime time = cal.toGregorianCalendar().toZonedDateTime();
            properties.set(GenericReportProperties.Name.TRANSLATION_TIME, time);
        }

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());
        return retval;
    }


}
