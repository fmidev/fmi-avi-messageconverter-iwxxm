package fi.fmi.avi.converter.iwxxm.airmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm21.AIRMETType;

public class AIRMETIWXXMStringSerializer extends AbstractAIRMETIWXXMSerializer<String> {

    @Override
    protected String render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException {
        final Document result = renderXMLDocument(airmet, hints);
        return renderDOMToString(result, hints);
    }
}