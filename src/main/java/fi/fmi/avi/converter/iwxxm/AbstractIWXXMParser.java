package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import org.w3c.dom.Document;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractIWXXMParser<T, S extends AviationWeatherMessageOrCollection> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S> {

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
    protected abstract Document parseAsDom(final T input) throws ConversionException;

    protected abstract S createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<S> result,
            final ConversionHints hints);

    protected abstract XMLSchemaInfo getSchemaInfo();

    public static class IWXXMValidationEventHandler implements ValidationEventHandler {

        private final List<ValidationEvent> events = new ArrayList<>();

        @Override
        public boolean handleEvent(final ValidationEvent event) {
            events.add(event);
            return true;
        }

        public List<ValidationEvent> getEvents() {
            return events;
        }

    }
}
