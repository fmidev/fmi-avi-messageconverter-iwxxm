package fi.fmi.avi.converter.iwxxm.v21.taf;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm21.TAFType;

/**
 * Specialization of {@link AbstractTAFIWXXMSerializer} for String output.
 */
public class TAFIWXXMStringSerializer extends AbstractTAFIWXXMSerializer<String> {

    @Override
    protected String render(final TAFType taf, final ConversionHints hints) throws ConversionException {
        final Document result = renderXMLDocument(taf, hints);
        return renderDOMToString(result, hints);
    }

}
