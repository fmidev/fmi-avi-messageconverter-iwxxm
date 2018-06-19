package fi.fmi.avi.converter.iwxxm;

import java.time.ZonedDateTime;

import fi.fmi.avi.model.AviationCodeListUser;
import icao.iwxxm21.ReportType;

/**
 * Container class for (metadata) properties parsed from IWXXM ReportType.
 */
public class GenericReportProperties extends AbstractPropertyContainer<ReportType> {

    public enum Name {
        PERMISSIBLE_USAGE(AviationCodeListUser.PermissibleUsage.class), PERMISSIBLE_USAGE_REASON(AviationCodeListUser.PermissibleUsageReason.class),
        PERMISSIBLE_USAGE_SUPPLEMENTARY(String.class),
        TRANSLATED_BULLETIN_ID(String.class),
        TRANSLATED_BULLETIN_RECEPTION_TIME(ZonedDateTime.class),
        TRANSLATION_CENTRE_DESIGNATOR(String.class),
        TRANSLATION_CENTRE_NAME(String.class),
        TRANSLATION_TIME(ZonedDateTime.class),
        TRANSLATION_FAILED_TAC(String.class);

        private Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }

    public GenericReportProperties(ReportType parent) {
        super(parent);
    }

    @Override
    protected Class<?> getAcceptedType(final Object key) {
        if (Name.class.isAssignableFrom(key.getClass())) {
            return ((Name) key).getAcceptedType();
        } else {
            throw new IllegalArgumentException("Key for TAF must be of type " + Name.class.getCanonicalName());
        }
    }
}
