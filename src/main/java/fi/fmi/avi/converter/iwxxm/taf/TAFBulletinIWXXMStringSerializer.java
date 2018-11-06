package fi.fmi.avi.converter.iwxxm.taf;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import wmo.collect2014.MeteorologicalBulletinType;

/**
 * Specialization of {@link AbstractTAFBulletinIWXXMSerializer} for String output.
 */
public class TAFBulletinIWXXMStringSerializer extends AbstractTAFBulletinIWXXMSerializer<String> {

    @Override
    protected String render(final MeteorologicalBulletinType bulletin, ConversionHints hints) throws ConversionException {
        Document result = renderXMLDocument(bulletin, hints);
        return renderDOMToString(result, hints);
    }

}
