package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import fi.fmi.avi.model.swx.amd79.immutable.SpaceWeatherAmd79BulletinImpl;
import org.w3c.dom.Document;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

public abstract class SpaceWeatherAmd79BulletinIWXXMParser<T> extends AbstractBulletinIWXXMParser<T, SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> {

    final private MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();

    public void setMessageConverter(final AviMessageSpecificConverter<Document, SpaceWeatherAdvisoryAmd79> converter) {
        this.scanner.setMessageConverter(converter);
    }

    @Override
    protected SpaceWeatherAmd79Bulletin buildBulletin(final BulletinProperties properties, final ZonedDateTime timestamp, final Set<ChronoField> timestampFields,
                                                      final ConversionHints hints) {
        final SpaceWeatherAmd79BulletinImpl.Builder builder = SpaceWeatherAmd79BulletinImpl.builder();
        properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class).ifPresent(builder::setHeading);
        builder.addAllMessages(properties.getList(BulletinProperties.Name.MESSAGE, SpaceWeatherAdvisoryAmd79.class));
        if (timestamp != null && timestampFields != null) {
            builder.setTimeStamp(timestamp).addAllTimeStampFields(timestampFields);
        }
        return builder.build();
    }

    @Override
    protected MeteorologicalBulletinIWXXMScanner<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> getScanner() {
        return this.scanner;
    }

    public static class FromDOM extends SpaceWeatherAmd79BulletinIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends SpaceWeatherAmd79BulletinIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
