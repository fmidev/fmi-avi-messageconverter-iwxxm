package fi.fmi.avi.converter.iwxxm.taf;

import org.w3c.dom.Document;

/**
 * Specialization of {@link AbstractTAFIWXXMParser} for DOM Document content.
 */
public class TAFIWXXMDOMParser extends AbstractTAFIWXXMParser<Document> {

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
