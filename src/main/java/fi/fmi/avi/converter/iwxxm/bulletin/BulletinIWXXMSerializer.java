package fi.fmi.avi.converter.iwxxm.bulletin;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;
import wmo.collect2014.MeteorologicalBulletinType;

/**
 * @param <T> bulletin input format
 * @param <S> bulletin content model type
 */
public abstract class BulletinIWXXMSerializer<T, U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<S, T> {

    private AviMessageSpecificConverter<U, Document> contentMessageConverter;

    protected abstract T render(final Document bulletin, ConversionHints hints) throws ConversionException;

    public void setMessageConverter(final AviMessageSpecificConverter<U, Document> converter) {
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
    public ConversionResult<T> convertMessage(final S input, final ConversionHints hints) {
        if (this.contentMessageConverter == null) {
            throw new IllegalStateException("No AviMessageConverter set");
        }

        final ConversionResult<T> result = new ConversionResult<>();
        if (input.getMessages().isEmpty()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No messages in bulletin"));
            return result;
        }
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo(F_SECURE_PROCESSING);
        schemaInfo.addSchemaSource(MeteorologicalBulletinType.class.getResourceAsStream("/int/wmo/collect/1.2/collect.xsd"));
        schemaInfo.addSchematronRule(MeteorologicalBulletinType.class.getResource("/schematron/xslt/int/wmo/collect/1.2/rule/collect.xsl"));
        schemaInfo.addSchemaLocation("http://def.wmo.int/collect/2014", "http://schemas.wmo.int/collect/1.2/collect.xsd");

        try {
            final Document dom;
            try {
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                final DocumentBuilder db = dbf.newDocumentBuilder();
                dom = db.parse(this.getClass().getResourceAsStream("collect-template.xml"));
                final Element collect = dom.getDocumentElement();
                final Attr id = dom.createAttributeNS(IWXXMNamespaceContext.getURI("gml"), "id");
                id.setPrefix("gml");
                id.setValue(UUID_PREFIX + UUID.randomUUID().toString());
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
                    result.setStatus(worstStatus);
                    if (messageResult.getConvertedMessage().isPresent()) {
                        outputMessages.add(messageResult.getConvertedMessage().get());
                    }
                }

                for (final Document outputMessage : outputMessages) {
                    final Node toAdd = dom.importNode(outputMessage.getDocumentElement(), true);
                    final Element metInfo = dom.createElementNS(IWXXMNamespaceContext.getURI("collect"), "meteorologicalInformation");
                    metInfo.appendChild(toAdd);
                    metInfo.setPrefix("collect");
                    collect.appendChild(metInfo);
                }

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
                final Element identifier = dom.createElementNS(IWXXMNamespaceContext.getURI("collect"), "bulletinIdentifier");
                identifier.setPrefix("collect");
                identifier.setTextContent(info.build().toGTSExchangeFileName());
                collect.appendChild(identifier);

            } catch (final IOException | SAXException | ParserConfigurationException e) {
                throw new ConversionException("Error in creating bulletin document", e);
            }

            //Should we validate the produced DOM against XML Schema? The individual messages have already been validated

            //Check against the collect schematron rules:
            result.addIssue(validateAgainstIWXXMSchematron(dom, schemaInfo, hints));
            result.setConvertedMessage(this.render(dom, hints));

        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to convert IWXXM message", e));
        }

        return result;
    }

    static public class ToDOM<U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>> extends BulletinIWXXMSerializer<Document, U, S> {

        @Override
        protected Document render(final Document bulletin, final ConversionHints hints) throws ConversionException {
            return bulletin;
        }

    }

    public static class ToString<U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>>
            extends BulletinIWXXMSerializer<java.lang.String, U, S> {

        @Override
        protected java.lang.String render(final Document bulletin, final ConversionHints hints) throws ConversionException {
            return renderDOMToString(bulletin, hints);
        }

    }
}
