package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm21.SIGMETType;

public class SIGMETIWXXMDOMSerializer  extends AbstractSIGMETIWXXMSerializer<Document> {
    @Override
    protected Document render(final SIGMETType sigmet, final ConversionHints hints) throws ConversionException {
        return renderXMLDocument(sigmet, hints);
    }
}
