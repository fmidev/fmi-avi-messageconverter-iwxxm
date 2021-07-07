package fi.fmi.avi.converter.iwxxm.generic;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.MeteorologicalBulletinIWXXMScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;

public abstract class GenericBulletinIWXXMParser<T> extends AbstractBulletinIWXXMParser<T, GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    private final MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> scanner;

    protected GenericBulletinIWXXMParser(IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
        scanner = iwxxmGenericBulletinScanner;
    }

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

    public static class FromDOM extends GenericBulletinIWXXMParser<Document> {
        public FromDOM(final IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
            super(iwxxmGenericBulletinScanner);
        }

        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends GenericBulletinIWXXMParser<java.lang.String> {
        public FromString(final IWXXMGenericBulletinScanner iwxxmGenericBulletinScanner) {
            super(iwxxmGenericBulletinScanner);
        }

        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
