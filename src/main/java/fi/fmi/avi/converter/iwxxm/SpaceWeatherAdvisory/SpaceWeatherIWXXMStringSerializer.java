package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public class SpaceWeatherIWXXMStringSerializer extends AbstractSpaceWeatherIWXXMSerializer<String> {
    @Override
    protected String render(final SpaceWeatherAdvisoryType swx, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        Document result = renderXMLDocument(swx, schemaInfo, hints);
        return renderDOMToString(result, hints);
    }
}
