package fi.fmi.avi.converter.iwxxm.bulletin;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.opengis.gml32.AbstractFeatureType;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessage;

import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;
import icao.iwxxm21.TAFType;
import wmo.collect2014.MeteorologicalBulletinType;
import wmo.collect2014.MeteorologicalInformationMemberPropertyType;

/**
 * @param <T>
 *         bulletin input format
 * @param <S>
 *         bulletin content model type
 * @param <V>
 *         bulletin content JAXB type
 */
public abstract class AbstractBulletinIWXXMSerializer<T, S extends AviationWeatherMessage, V extends AbstractFeatureType, U extends MeteorologicalBulletin<S>>
        extends AbstractIWXXMSerializer<U, T> {

    private AviMessageSpecificConverter<S, V> contentMessageConverter;

    @Override
    protected InputStream getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        InputStream retval = this.getClass().getResourceAsStream("WMOCollectCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    protected abstract T render(final MeteorologicalBulletinType taf, ConversionHints hints) throws ConversionException;

    protected abstract Class<V> getMessageJAXBClass();

    public void setMessageConverter(final AviMessageSpecificConverter<S, V> converter) {
        this.contentMessageConverter = converter;
    }

    /**
     * Converts a single message.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the {@link ConversionResult} with the contentMessageConverter message and the possible conversion issues
     */
    @Override
    public ConversionResult<T> convertMessage(final U input, final ConversionHints hints) {
        if (this.contentMessageConverter == null) {
            throw new IllegalStateException("No AviMessageConverter set");
        }

        final ConversionResult<T> result = new ConversionResult<>();
        if (input.getMessages().isEmpty()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No messages in bulletin"));
            return result;
        }

        final MeteorologicalBulletinType bulletin = create(MeteorologicalBulletinType.class);
        bulletin.setId("bulletin-" + UUID.randomUUID().toString());

        final GTSExchangeFileInfo.Builder info = new GTSExchangeFileInfo.Builder()//
                .setPFlag(GTSExchangeFileInfo.GTSExchangePFlag.A)//
                .setHeading(input.getHeading())//
                .setMetadataFile(false)//
                .setFileType(GTSExchangeFileInfo.GTSExchangeFileType.XML);
        if (input.getTimeStamp().isPresent()) {
            final ZonedDateTime timeStamp = input.getTimeStamp().get();
            final Set<ChronoField> fieldsToInclude = input.getTimeStampFields();
            if (fieldsToInclude.contains(ChronoField.YEAR)) {
                info.setTimeStampYear(timeStamp.getYear());
            }
            if (fieldsToInclude.contains(ChronoField.MONTH_OF_YEAR)) {
                info.setTimeStampMonth(timeStamp.getMonth());
            }
            if (fieldsToInclude.contains(ChronoField.DAY_OF_MONTH)) {
                info.setTimeStampDay(timeStamp.getDayOfMonth());
            }
            if (fieldsToInclude.contains(ChronoField.HOUR_OF_DAY)) {
                info.setTimeStampHour(timeStamp.getHour());
            }
            if (fieldsToInclude.contains(ChronoField.MINUTE_OF_HOUR)) {
                info.setTimeStampMinute(timeStamp.getMinute());
            }
            if (fieldsToInclude.contains(ChronoField.SECOND_OF_MINUTE)) {
                info.setTimeStampSecond(timeStamp.getSecond());
            }
        } else {
            info.setTimeStamp(LocalDateTime.now(ZoneId.of("UTC")));
        }

        bulletin.setBulletinIdentifier(info.build().toGTSExchangeFileName());

        final List<V> outputMessages = new ArrayList<>();
        ConversionResult<V> messageResult;
        ConversionResult.Status worstStatus = ConversionResult.Status.SUCCESS;
        for (final S inputMessage : input.getMessages()) {
            messageResult = this.contentMessageConverter.convertMessage(inputMessage, hints);
            if (ConversionResult.Status.SUCCESS != messageResult.getStatus()) {
                if (ConversionResult.Status.isMoreCritical(messageResult.getStatus(), worstStatus)) {
                    worstStatus = messageResult.getStatus();
                }
                result.addIssue(messageResult.getConversionIssues());
            }
            result.setStatus(worstStatus);
            if (messageResult.getConvertedMessage().isPresent()) {
                outputMessages.add(messageResult.getConvertedMessage().get());
            }
        }
        MeteorologicalInformationMemberPropertyType memberProp;
        // FIXME: the schema stuff should be given individually for each member, not at the top of the bulletin document. This would be the correct way to
        //  handle the IWXXM versions with bulletins
        for (final V outputMessage : outputMessages) {
            memberProp = create(MeteorologicalInformationMemberPropertyType.class);
            memberProp.setAbstractFeature(wrap(outputMessage, getMessageJAXBClass()));
            bulletin.getMeteorologicalInformation().add(memberProp);
        }
        try {
            result.addIssue(validateDocument(bulletin, MeteorologicalBulletinType.class, getSchemaInfo(), hints));
            result.setConvertedMessage(this.render(bulletin, hints));
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to convert IWXXM message", e));
        }
        return result;
    }

    @Override
    protected XMLSchemaInfo getSchemaInfo() {
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(MeteorologicalBulletinType.class.getResourceAsStream("/int/wmo/collect/1.2/collect.xsd"));
        schemaInfo.addSchemaLocation("http://def.wmo.int/collect/2014", "http://schemas.wmo.int/collect/1.2/collect.xsd");
        schemaInfo.setSchematronRules(TAFType.class.getResource("/schematron/xslt/int/wmo/collect/1.2/rule/collect.xsl"));
        return schemaInfo;
    }
}
