package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

public class SpaceWeatherIWXXMStringParser extends AbstractSpaceWeatherIWXXMParser<String> {
    @Override
    protected Document parseAsDom(final String input) throws ConversionException {
        return IWXXMConverterBase.parseStringToDOM(input);
    }
}
