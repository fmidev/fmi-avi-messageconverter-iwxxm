package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;

/**
 * Specialization of {@link AbstractSIGMETIWXXMParser} for DOM Document content.
 */
public class SIGMETIWXXMDOMParser extends AbstractSIGMETIWXXMParser<Document> {

    /**
     * This implementation simple passes the input Document through.
     *
     * @param input
     *         the raw input format
     *
     * @return the parsed DOM
     *
     */
    @Override
    protected Document parseAsDom(final Document input)  throws ConversionException {
        return input;
    }
}
