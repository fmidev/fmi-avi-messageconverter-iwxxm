package fi.fmi.avi.converter.iwxxm.sigmet;

import org.w3c.dom.Document;

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
    protected Document parseAsDom(final Document input) {
        return input;
    }
}
