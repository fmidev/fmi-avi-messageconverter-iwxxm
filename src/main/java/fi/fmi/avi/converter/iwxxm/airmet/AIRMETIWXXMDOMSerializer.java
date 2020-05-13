package fi.fmi.avi.converter.iwxxm.airmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm21.AIRMETType;

public class AIRMETIWXXMDOMSerializer extends AbstractAIRMETIWXXMSerializer<Document> {
    @Override
    protected Document render(final AIRMETType airmet, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        return this.renderXMLDocument(airmet, schemaInfo, hints);
    }
}
