package fi.fmi.avi.converter.iwxxm.v21.metar;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;

/**
 * Specialization of {@link AbstractMETARIWXXMParser} for String input.
 */
public class METARIWXXMStringParser extends AbstractMETARIWXXMParser<String> {

    /**
     * Returns the METAR input message as A DOM Document.
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
