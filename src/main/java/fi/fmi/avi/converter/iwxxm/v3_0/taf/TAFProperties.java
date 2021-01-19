package fi.fmi.avi.converter.iwxxm.v3_0.taf;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class TAFProperties extends AbstractPropertyContainer {

    public TAFProperties() { }

    public enum Name implements PropertyName {

        ISSUE_TIME(PartialOrCompleteTimeInstant .class),
        VALID_TIME(PartialOrCompleteTimePeriod .class),
        STATUS(AviationCodeListUser.TAFStatus.class),
        REPORT_METADATA(GenericReportProperties.class);

        private final Class<?> acceptedType;

        Name(final Class<?> type) {
            this.acceptedType = type;
        }

        @Override
        public Class<?> getAcceptedType() {
            return this.acceptedType;
        }
    }


}
