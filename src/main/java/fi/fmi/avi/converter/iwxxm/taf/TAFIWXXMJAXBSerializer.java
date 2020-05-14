package fi.fmi.avi.converter.iwxxm.taf;

import fi.fmi.avi.converter.ConversionHints;
import icao.iwxxm21.TAFType;

/**
 * Specialization of {@link AbstractTAFIWXXMSerializer} for generating DOM output.
 */
public class TAFIWXXMJAXBSerializer extends AbstractTAFIWXXMSerializer<TAFType> {

    @Override
    protected TAFType render(final TAFType taf, final ConversionHints hints) {
        return taf;
    }

}
