package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

public class GenericBulletinIWXXMScanner extends MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    @Override
    protected ConversionResult<GenericAviationWeatherMessage> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        return new GenericAviationMessageScanner().createAviationWeatherMessage(featureElement, hints);
    }
}
