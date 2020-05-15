package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessage;

/**
 * Specialization of {@link AbstractBulletinIWXXMSerializer} for generating DOM output.
 */
public class BulletinIWXXMDOMSerializer<S extends AviationWeatherMessage> extends AbstractBulletinIWXXMSerializer<Document, S> {

    @Override
    protected Document render(final Document bulletin, final ConversionHints hints) throws ConversionException {
        return bulletin;
    }

}
