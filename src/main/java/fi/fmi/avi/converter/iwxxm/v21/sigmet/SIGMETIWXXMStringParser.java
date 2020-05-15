package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

/**
 * Specialization of {@link AbstractSIGMETIWXXMParser} for String input.
 */
public class SIGMETIWXXMStringParser extends AbstractSIGMETIWXXMParser<String> {
     /**
     * Returns the TAF input message as A DOM Document.
     *
     * @param input
     *         the XML Document input as a String
     *
     * @return the input parsed as DOM
     *
     * @throws ConversionException
     *         if an exception occurs while converting input to DOM
     */
    @Override
    protected Document parseAsDom(final String input) throws ConversionException {
        return IWXXMConverterBase.parseStringToDOM(input);
    }
}
