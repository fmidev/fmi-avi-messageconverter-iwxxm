package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.taf.TAF;

/**
 * Specialization of {@link AbstractBulletinIWXXMSerializer} for generating DOM output.
 */
public class TAFBulletinIWXXMDOMSerializer extends AbstractBulletinIWXXMSerializer<Document, TAF> {

    @Override
    protected Document render(final Document bulletin, final ConversionHints hints) throws ConversionException {
        return bulletin;
    }

}
