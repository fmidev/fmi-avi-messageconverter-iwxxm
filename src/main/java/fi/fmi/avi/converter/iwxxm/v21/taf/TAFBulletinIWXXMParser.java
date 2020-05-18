package fi.fmi.avi.converter.iwxxm.v21.taf;

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
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;

public abstract class TAFBulletinIWXXMParser<T> extends AbstractBulletinIWXXMParser<T, TAF, TAFBulletin> {

    final private MeteorologicalBulletinIWXXMScanner<TAF, TAFBulletin> scanner = new MeteorologicalBulletinIWXXMScanner<>();

    public void setMessageConverter(final AviMessageSpecificConverter<Document, TAF> converter) {
        this.scanner.setMessageConverter(converter);
    }

    @Override
    protected TAFBulletin buildBulletin(final BulletinProperties properties, final ZonedDateTime timestamp, final Set<ChronoField> timestampFields,
            final ConversionHints hints) {
        final TAFBulletinImpl.Builder builder = TAFBulletinImpl.builder();
        properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class).ifPresent(builder::setHeading);
        builder.addAllMessages(properties.getList(BulletinProperties.Name.MESSAGE, TAF.class));
        if (timestamp != null && timestampFields != null) {
            builder.setTimeStamp(timestamp).addAllTimeStampFields(timestampFields);
        }
        return builder.build();
    }

    @Override
    protected MeteorologicalBulletinIWXXMScanner<TAF, TAFBulletin> getScanner() {
        return this.scanner;
    }

    public static class AsDOM extends TAFBulletinIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
            return input;
        }
    }

    public static class AsString extends TAFBulletinIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
