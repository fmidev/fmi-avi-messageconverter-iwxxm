package fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;

public class SpaceWeatherIWXXMDOMParser extends AbstractSpaceWeatherIWXXMParser<Document> {
    @Override
    protected Document parseAsDom(final Document input) throws ConversionException {
        return input;
    }
}
