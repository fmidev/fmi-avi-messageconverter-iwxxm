package fi.fmi.avi.converter.iwxxm.taf;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm21.TAFType;

/**
 * Specialization of {@link AbstractTAFIWXXMSerializer} for generating DOM output.
 */
public class TAFIWXXMDOMSerializer extends AbstractTAFIWXXMSerializer<Document> {

    @Override
    protected Document render(final TAFType taf, final ConversionHints hints) throws ConversionException {
        return this.renderXMLDocument(taf, hints);
    }

}
