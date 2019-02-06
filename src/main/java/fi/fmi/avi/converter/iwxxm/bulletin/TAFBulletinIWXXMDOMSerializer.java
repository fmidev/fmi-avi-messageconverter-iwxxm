package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.taf.AbstractTAFIWXXMSerializer;
import wmo.collect2014.MeteorologicalBulletinType;

/**
 * Specialization of {@link AbstractTAFIWXXMSerializer} for generating DOM output.
 */
public class TAFBulletinIWXXMDOMSerializer extends AbstractTAFBulletinIWXXMSerializer<Document> {

    @Override
    protected Document render(final MeteorologicalBulletinType bulletin, final ConversionHints hints) throws ConversionException {
        return this.renderXMLDocument(bulletin, hints);
    }

}
