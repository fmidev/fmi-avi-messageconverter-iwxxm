package fi.fmi.avi.converter.iwxxm.v3_0.SpaceWeatherAdvisory;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.converter.iwxxm.bulletin.BulletinProperties;
import fi.fmi.avi.converter.iwxxm.bulletin.MeteorologicalBulletinIWXXMScanner;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherBulletin;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherBulletinImpl;
import fi.fmi.avi.model.bulletin.BulletinHeading;

public abstract class SpaceWeatherBulletinIWXXMParser<T> extends AbstractBulletinIWXXMParser<T, SpaceWeatherAdvisory, SpaceWeatherBulletin> {

    final private MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisory, SpaceWeatherBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();

    public void setMessageConverter(final AviMessageSpecificConverter<Document, SpaceWeatherAdvisory> converter) {
        this.scanner.setMessageConverter(converter);
    }

    @Override
    protected SpaceWeatherBulletin buildBulletin(final BulletinProperties properties, final ZonedDateTime timestamp, final Set<ChronoField> timestampFields,
            final ConversionHints hints) {
        final SpaceWeatherBulletinImpl.Builder builder = SpaceWeatherBulletinImpl.builder();
        properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class).ifPresent(builder::setHeading);
        builder.addAllMessages(properties.getList(BulletinProperties.Name.MESSAGE, SpaceWeatherAdvisory.class));
        if (timestamp != null && timestampFields != null) {
            builder.setTimeStamp(timestamp).addAllTimeStampFields(timestampFields);
        }
        return builder.build();
    }

    @Override
    protected MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisory, SpaceWeatherBulletin> getScanner() {
        return this.scanner;
    }

    public static class AsDOM extends SpaceWeatherBulletinIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
            return input;
        }
    }

    public static class AsString extends SpaceWeatherBulletinIWXXMParser<java.lang.String> {
        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
