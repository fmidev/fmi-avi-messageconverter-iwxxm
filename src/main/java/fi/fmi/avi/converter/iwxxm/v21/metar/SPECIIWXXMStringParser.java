package fi.fmi.avi.converter.iwxxm.v21.metar;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

/**
 * Specialization of {@link AbstractSPECIIWXXMParser} for String input.
 */
public class SPECIIWXXMStringParser extends AbstractSPECIIWXXMParser<String> {

    /**
     * Returns the SPECI input message as A DOM Document.
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
