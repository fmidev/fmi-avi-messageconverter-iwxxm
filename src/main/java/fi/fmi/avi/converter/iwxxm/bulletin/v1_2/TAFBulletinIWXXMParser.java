package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractBulletinIWXXMParser;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;
import org.w3c.dom.Document;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Set;

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
        properties.get(BulletinProperties.Name.COLLECT_IDENTIFIER, String.class).ifPresent(builder::setCollectIdentifier);
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

    public static class FromDOM extends TAFBulletinIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends TAFBulletinIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final java.lang.String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }
}
