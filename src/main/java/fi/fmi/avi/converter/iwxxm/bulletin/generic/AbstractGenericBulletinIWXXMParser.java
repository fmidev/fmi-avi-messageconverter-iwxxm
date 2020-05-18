package fi.fmi.avi.converter.iwxxm.bulletin.generic;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.bulletin.MeteorologicalBulletinIWXXMScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;

public abstract class AbstractGenericBulletinIWXXMParser<T>
        extends AbstractBulletinIWXXMParser<T, GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    private final MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> scanner = new IWXXMGenericBulletinScanner();

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
    protected MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> getScanner() {
        return this.scanner;
    }

    public static class DOM extends AbstractGenericBulletinIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
            return input;
        }
    }

    public static class String extends AbstractGenericBulletinIWXXMParser<java.lang.String> {
        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
