package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;

import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;

/**
 * Container class for (metadata) properties parsed from IWXXM ReportType.
 */
public class GenericReportProperties extends AbstractPropertyContainer {

    public enum Name implements PropertyName {
        REPORT_STATUS(AviationWeatherMessage.ReportStatus.class),//
        PERMISSIBLE_USAGE(AviationCodeListUser.PermissibleUsage.class),//
        PERMISSIBLE_USAGE_REASON(AviationCodeListUser.PermissibleUsageReason.class),//
        PERMISSIBLE_USAGE_SUPPLEMENTARY(String.class),//
        TRANSLATED_BULLETIN_ID(String.class),//
        TRANSLATED_BULLETIN_RECEPTION_TIME(ZonedDateTime.class),//
        TRANSLATION_CENTRE_DESIGNATOR(String.class),//
        TRANSLATION_CENTRE_NAME(String.class),//
        TRANSLATION_TIME(ZonedDateTime.class),//
        TRANSLATION_FAILED_TAC(String.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public GenericReportProperties() {
    }
}
