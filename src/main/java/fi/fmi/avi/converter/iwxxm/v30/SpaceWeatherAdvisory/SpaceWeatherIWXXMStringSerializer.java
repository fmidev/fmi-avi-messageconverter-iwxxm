package fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public class SpaceWeatherIWXXMStringSerializer extends AbstractSpaceWeatherIWXXMSerializer<String> {
    @Override
    protected String render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException {
        Document result = renderXMLDocument(swx, hints);
        return renderDOMToString(result, hints);
    }
}
