package fi.fmi.avi.converter.iwxxm.bulletin.v1_2;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.bulletin.AbstractIWXXMParser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

public abstract class GenericAviationWeatherMessageParser<T> extends AbstractIWXXMParser<T, GenericAviationWeatherMessage> {

    @Override
    public ConversionResult<GenericAviationWeatherMessage> convertMessage(final T input, final ConversionHints hints) {
        ConversionResult<GenericAviationWeatherMessage> retval = new ConversionResult<>();
        try {
            final Document doc = parseAsDom(input);

            retval = new GenericAviationWeatherMessageScanner().createAviationWeatherMessage(doc.getDocumentElement(), hints);

        } catch (Exception ce) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Error in parsing input", ce));
        }

        return retval;
    }

    public static class FromString extends GenericAviationWeatherMessageParser<String> {
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return parseStringToDOM(input);
        }
    }

    public static class FromDOM extends GenericAviationWeatherMessageParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }
}
