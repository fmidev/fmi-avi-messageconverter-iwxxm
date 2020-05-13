package fi.fmi.avi.converter.iwxxm.bulletin;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import wmo.collect2014.MeteorologicalBulletinType;

/**
 * Specialization of {@link AbstractBulletinIWXXMSerializer} for String output.
 */
public class TAFBulletinIWXXMStringSerializer extends AbstractTAFBulletinIWXXMSerializer<String> {

    @Override
    protected String render(final MeteorologicalBulletinType bulletin, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        final Document result = renderXMLDocument(bulletin, schemaInfo, hints);
        return renderDOMToString(result, hints);
    }

}
