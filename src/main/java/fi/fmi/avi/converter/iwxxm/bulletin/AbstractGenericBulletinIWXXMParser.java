package fi.fmi.avi.converter.iwxxm.bulletin;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.immutable.GenericMeteorologicalBulletinImpl;

public abstract class AbstractGenericBulletinIWXXMParser<T> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, GenericMeteorologicalBulletin> {

    @Override
    public ConversionResult<GenericMeteorologicalBulletin> convertMessage(final T input, final ConversionHints hints) {
        final ConversionResult<GenericMeteorologicalBulletin> retval = new ConversionResult<>();
        try {
            final Document doc = parseAsDom(input);
            final BulletinProperties properties = new BulletinProperties();
            retval.addIssue(IWXXMGenericBulletinScanner.collectBulletinProperties(doc, properties, hints));

            //Heading
            if (!properties.contains(BulletinProperties.Name.HEADING)) {
                retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No bulletin heading"));
                return retval;
            }
            final GenericMeteorologicalBulletinImpl.Builder builder = new GenericMeteorologicalBulletinImpl.Builder();
            properties.get(BulletinProperties.Name.HEADING, BulletinHeading.class).ifPresent(builder::setHeading);

            //Messages
            builder.addAllMessages(properties.getList(BulletinProperties.Name.MESSAGE, GenericAviationWeatherMessage.class));

            //Timestamp
            if (properties.containsAny(BulletinProperties.Name.TIMESTAMP_YEAR, BulletinProperties.Name.TIMESTAMP_MONTH, BulletinProperties.Name.TIMESTAMP_DAY,
                    BulletinProperties.Name.TIMESTAMP_HOUR, BulletinProperties.Name.TIMESTAMP_MINUTE, BulletinProperties.Name.TIMESTAMP_SECOND)) {

                ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
                final Set<ChronoField> timestampFields = new HashSet<>();
                final Optional<Integer> year = properties.get(BulletinProperties.Name.TIMESTAMP_YEAR, Integer.class);
                final Optional<Month> month = properties.get(BulletinProperties.Name.TIMESTAMP_MONTH, Month.class);
                final Optional<Integer> day = properties.get(BulletinProperties.Name.TIMESTAMP_DAY, Integer.class);
                final Optional<Integer> hour = properties.get(BulletinProperties.Name.TIMESTAMP_HOUR, Integer.class);
                final Optional<Integer> minute = properties.get(BulletinProperties.Name.TIMESTAMP_MINUTE, Integer.class);
                final Optional<Integer> second = properties.get(BulletinProperties.Name.TIMESTAMP_SECOND, Integer.class);

                if (year.isPresent()) {
                    timestamp = timestamp.withYear(year.get());
                    timestampFields.add(ChronoField.YEAR);
                }
                if (month.isPresent()) {
                    timestamp = timestamp.withMonth(month.get().getValue());
                    timestampFields.add(ChronoField.MONTH_OF_YEAR);
                }
                if (day.isPresent()) {
                    timestamp = timestamp.withDayOfMonth(day.get());
                    timestampFields.add(ChronoField.DAY_OF_MONTH);
                }
                if (hour.isPresent()) {
                    timestamp = timestamp.withHour(hour.get());
                    timestampFields.add(ChronoField.HOUR_OF_DAY);
                }
                if (minute.isPresent()) {
                    timestamp = timestamp.withMinute(minute.get());
                    timestampFields.add(ChronoField.MINUTE_OF_HOUR);
                }
                if (second.isPresent()) {
                    timestamp = timestamp.withSecond(second.get());
                    timestampFields.add(ChronoField.SECOND_OF_MINUTE);
                }

                builder.setTimeStamp(timestamp)//
                        .addAllTimeStampFields(timestampFields);

            }

            retval.setStatus(ConversionResult.Status.SUCCESS);
            retval.setConvertedMessage(builder.build());
        } catch (final ConversionException ce) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Error in parsing input", ce));
        }
        return retval;
    }

    protected abstract Document parseAsDom(T input) throws ConversionException;
}
