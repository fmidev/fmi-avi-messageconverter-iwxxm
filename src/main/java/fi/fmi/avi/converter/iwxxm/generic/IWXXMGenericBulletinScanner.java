package fi.fmi.avi.converter.iwxxm.generic;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.bulletin.v1_2.MeteorologicalBulletinIWXXMScanner;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

public class IWXXMGenericBulletinScanner extends MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {
    final GenericAviationWeatherMessageParser messageParser;

    public IWXXMGenericBulletinScanner(final GenericAviationWeatherMessageParser messageParser) {
        this.messageParser = messageParser;
    }


    @Override
    protected ConversionResult<GenericAviationWeatherMessage> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        return messageParser.createAviationWeatherMessage(featureElement, hints);
    }
}
