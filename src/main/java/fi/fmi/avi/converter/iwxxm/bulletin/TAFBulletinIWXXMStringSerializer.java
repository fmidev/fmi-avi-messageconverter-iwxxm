package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.taf.TAF;

/**
 * Specialization of {@link AbstractBulletinIWXXMSerializer} for String output.
 */
public class TAFBulletinIWXXMStringSerializer extends AbstractBulletinIWXXMSerializer<String, TAF> {

    @Override
    protected String render(final Document bulletin, final ConversionHints hints) throws ConversionException {
        return renderDOMToString(bulletin, hints);
    }

}
