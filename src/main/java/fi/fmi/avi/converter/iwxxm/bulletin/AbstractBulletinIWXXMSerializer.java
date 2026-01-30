package fi.fmi.avi.converter.iwxxm.bulletin;

import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @param <S> bulletin content model type
 */
public abstract class AbstractBulletinIWXXMSerializer<T, U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<S, T> {

    private AbstractIWXXMSerializer<U, Document> contentMessageConverter;

    public void setMessageConverter(final AbstractIWXXMSerializer<U, Document> converter) {
        this.contentMessageConverter = converter;
    }

    /**
     * Converts a single message.
     *
     * @param input input message
     * @param hints parsing hints
     * @return the {@link ConversionResult} with the contentMessageConverter message and the possible conversion issues
     */
    @Override
    public ConversionResult<T> convertMessage(final S input, final ConversionHints hints) {
        if (this.contentMessageConverter == null) {
            throw new IllegalStateException("No AviMessageConverter set");
        }

        final ConversionResult<T> result = new ConversionResult<>();
        if (input.getMessages().isEmpty()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No messages in bulletin"));
            return result;
        }
        final XMLSchemaInfo schemaInfo = getSchemaInfo();
        schemaInfo.addAllFrom(this.contentMessageConverter.getSchemaInfo());

        final T retval;
        try {
            final Document dom;
            try {
                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(true);
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                try (final InputStream inputStream = this.getClass().getResourceAsStream("collect-template.xml")) {
                    dom = documentBuilder.parse(inputStream);
                }
                final Element collect = dom.getDocumentElement();
                final Attr id = dom.createAttributeNS(IWXXMNamespaceContext.getDefaultURI("gml"), "id");
                id.setPrefix("gml");
                id.setValue(getUUID());
                collect.setAttributeNodeNS(id);

                final List<Document> outputMessages = new ArrayList<>();
                ConversionResult<Document> messageResult;
                ConversionResult.Status worstStatus = ConversionResult.Status.SUCCESS;

                for (final U inputMessage : input.getMessages()) {
                    messageResult = this.contentMessageConverter.convertMessage(inputMessage, hints);
                    if (ConversionResult.Status.SUCCESS != messageResult.getStatus()) {
                        if (ConversionResult.Status.isMoreCritical(messageResult.getStatus(), worstStatus)) {
                            worstStatus = messageResult.getStatus();
                        }
                        result.addIssue(messageResult.getConversionIssues());
                    }
                    if (ConversionResult.Status.isMoreCritical(worstStatus, ConversionResult.Status.SUCCESS)) {
                        result.setStatus(worstStatus);
                    }
                    if (messageResult.getConvertedMessage().isPresent()) {
                        outputMessages.add(messageResult.getConvertedMessage().get());
                    }
                }

                final String collectIdentifier;
                if (input.getCollectIdentifier().isPresent()) {
                    collectIdentifier = input.getCollectIdentifier().get();
                } else {
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
                    collectIdentifier = info.build().toGTSExchangeFilename();
                }
                final Element identifier = dom.createElementNS(IWXXMNamespaceContext.getDefaultURI("collect"), "bulletinIdentifier");
                identifier.setPrefix("collect");
                identifier.setTextContent(collectIdentifier);
                collect.appendChild(identifier);
                retval = aggregateAsBulletin(dom, outputMessages, hints);

            } catch (final IOException | SAXException | ParserConfigurationException e) {
                throw new ConversionException("Error in creating bulletin document", e);
            }
            result.addIssue(validate(retval, schemaInfo, hints));
            result.setConvertedMessage(retval);

        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to convert IWXXM message", e));
        }

        return result;
    }

    protected abstract XMLSchemaInfo getSchemaInfo();

    protected abstract T aggregateAsBulletin(final Document collection, final List<Document> messages, final ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;
}
