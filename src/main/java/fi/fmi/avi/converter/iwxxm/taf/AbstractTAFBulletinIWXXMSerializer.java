package fi.fmi.avi.converter.iwxxm.taf;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.util.GTSExchangeFileInfo;
import icao.iwxxm21.TAFType;
import wmo.collect2014.MeteorologicalBulletinType;
import wmo.collect2014.MeteorologicalInformationMemberPropertyType;

public abstract class AbstractTAFBulletinIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<TAFBulletin, T> {

    private AviMessageSpecificConverter<TAF, TAFType> converter;

    @Override
    protected InputStream getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        InputStream retval = this.getClass().getResourceAsStream("TAFCollectCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    protected abstract T render(final MeteorologicalBulletinType taf, ConversionHints hints) throws ConversionException;

    public void setMessageConverter(final AviMessageSpecificConverter<TAF, TAFType> converter) {
        this.converter = converter;
    }

    /**
     * Converts a single message.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the {@link ConversionResult} with the converter message and the possible conversion issues
     */
    @Override
    public ConversionResult<T> convertMessage(final TAFBulletin input, final ConversionHints hints) {
        if (this.converter == null) {
            throw new IllegalStateException("No AviMessageConverter set");
        }

        ConversionResult<T> result = new ConversionResult<>();
        if (input.getMessages().isEmpty()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No TAF messages in bulletin"));
            return result;
        }
        LocalDateTime issue;
        if (!input.getIssueTime().getCompleteTime().isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "TAFBulletin issue time is not complete, " + "cannot serialize as WMO Collect / IWXXM"));
            return result;
        } else {
            issue = input.getIssueTime().getCompleteTime().get().toLocalDateTime();
        }

        MeteorologicalBulletinType bulletin = create(MeteorologicalBulletinType.class);
        bulletin.setId("bulletin-" + UUID.randomUUID().toString());

        bulletin.setBulletinIdentifier(new GTSExchangeFileInfo.Builder()//
                .setPFlag(GTSExchangeFileInfo.GTSExchangePFlag.A)//
                .setHeading(input.getHeading())//
                .setMetadataFile(false)//
                .setTimeStamp(issue)//
                .setFileType(GTSExchangeFileInfo.GTSExchangeFileType.XML)//
                .build()//
                .toGTSExchangeFileName());

        List<TAFType> tafs = new ArrayList<>();
        ConversionResult<TAFType> tafResult;
        ConversionResult.Status worstStatus = ConversionResult.Status.SUCCESS;
        for (TAF taf : input.getMessages()) {
            tafResult = this.converter.convertMessage(taf, hints);
            if (ConversionResult.Status.SUCCESS != tafResult.getStatus()) {
                if (ConversionResult.Status.isMoreCritical(tafResult.getStatus(), worstStatus)) {
                    worstStatus = tafResult.getStatus();
                }
                result.addIssue(tafResult.getConversionIssues());
            }
            result.setStatus(worstStatus);
            if (tafResult.getConvertedMessage().isPresent()) {
                tafs.add(tafResult.getConvertedMessage().get());
            }
        }
        MeteorologicalInformationMemberPropertyType tafProp;
        for (TAFType taf : tafs) {
            tafProp = create(MeteorologicalInformationMemberPropertyType.class);
            tafProp.setAbstractFeature(wrap(taf, TAFType.class));
            bulletin.getMeteorologicalInformation().add(tafProp);
        }
        try {
            ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(result);
            validateDocument(bulletin, MeteorologicalBulletinType.class, hints, eventHandler);
            if (eventHandler.errorsFound()) {
                result.setStatus(ConversionResult.Status.FAIL);
            } else {
                result.setConvertedMessage(this.render(bulletin, hints));
            }
        } catch (ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

}
