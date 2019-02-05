package fi.fmi.avi.converter.iwxxm.airmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.sigmet.AbstractSIGMETIWXXMSerializer;
import icao.iwxxm21.AIRMETType;

public class AIRMETIWXXMDOMSerializer extends AbstractAIRMETIWXXMSerializer<Document> {
    @Override
    protected Document render(final AIRMETType sigmet, final ConversionHints hints) throws ConversionException {
        return null;
    }
}
