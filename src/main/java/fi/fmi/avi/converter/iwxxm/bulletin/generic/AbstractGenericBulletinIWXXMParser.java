package fi.fmi.avi.converter.iwxxm.bulletin.generic;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinScanner;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinProperties;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;

public abstract class AbstractGenericBulletinIWXXMParser<T> extends AbstractBulletinIWXXMParser<T, GenericMeteorologicalBulletin> {

    private final AbstractBulletinScanner<GenericMeteorologicalBulletin> scanner = new IWXXMGenericBulletinScanner();

    @Override
    protected GenericMeteorologicalBulletin buildBulletin(final BulletinProperties properties, final ZonedDateTime timestamp,
            final Set<ChronoField> timestampFields, final ConversionHints hints) {
        final GenericMeteorologicalBulletinImpl.Builder builder = GenericMeteorologicalBulletinImpl.builder();

        properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class).ifPresent(builder::setHeading);

        builder.addAllMessages(properties.getList(BulletinProperties.Name.MESSAGE, GenericAviationWeatherMessage.class));
        if (timestamp != null && timestampFields != null) {
            builder.setTimeStamp(timestamp).addAllTimeStampFields(timestampFields);
        }
        return builder.build();
    }

    @Override
    protected AbstractBulletinScanner<GenericMeteorologicalBulletin> getScanner() {
        return this.scanner;
    }

}
