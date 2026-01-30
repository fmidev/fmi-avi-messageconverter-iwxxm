package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.iwxxm.AbstractPropertyContainer;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.BulletinHeading;

import java.time.Month;

public class BulletinProperties extends AbstractPropertyContainer {

    public BulletinProperties() {
    }

    public enum Name implements PropertyName {
        HEADING(BulletinHeading.class),
        TIMESTAMP_YEAR(Integer.class),
        TIMESTAMP_MONTH(Month.class),
        TIMESTAMP_DAY(Integer.class),
        TIMESTAMP_HOUR(Integer.class),
        TIMESTAMP_MINUTE(Integer.class),
        TIMESTAMP_SECOND(Integer.class),
        MESSAGE(AviationWeatherMessage.class),
        COLLECT_IDENTIFIER(String.class);

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
