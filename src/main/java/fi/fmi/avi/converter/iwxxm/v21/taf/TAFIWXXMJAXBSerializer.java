package fi.fmi.avi.converter.iwxxm.v21.taf;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import icao.iwxxm21.TAFType;

/**
 * Specialization of {@link AbstractTAFIWXXMSerializer} for generating DOM output.
 */
public class TAFIWXXMJAXBSerializer extends AbstractTAFIWXXMSerializer<TAFType> {

    @Override
    protected TAFType render(final TAFType taf, final XMLSchemaInfo schemaInfo, final ConversionHints hints) {
        return taf;
    }

}
