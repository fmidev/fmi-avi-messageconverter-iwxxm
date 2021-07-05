package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import org.w3c.dom.Element;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

public class IWXXMGenericBulletinScanner extends MeteorologicalBulletinIWXXMScanner<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {
    final GenericAviationWeatherMessageScanner scanner;

    public IWXXMGenericBulletinScanner(GenericAviationWeatherMessageScanner scanner) {
        this.scanner = scanner;
    }


    @Override
    protected ConversionResult<GenericAviationWeatherMessage> createAviationWeatherMessage(final Element featureElement, final ConversionHints hints) {
        return scanner.createAviationWeatherMessage(featureElement, hints);
    }
}
