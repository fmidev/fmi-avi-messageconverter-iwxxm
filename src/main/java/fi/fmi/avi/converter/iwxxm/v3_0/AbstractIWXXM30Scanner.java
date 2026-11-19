package fi.fmi.avi.converter.iwxxm.v3_0;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
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

        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, input.getTranslationCentreDesignator());
        properties.set(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, input.getTranslationCentreName());
        properties.set(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, input.getTranslationFailedTAC());

        collectTranslationTimes(input.getTranslatedBulletinReceptionTime(), input.getTranslationTime(), properties, hints);
        return retval;
    }
}
