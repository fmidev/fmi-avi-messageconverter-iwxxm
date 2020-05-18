package fi.fmi.avi.converter.iwxxm.v21.metar;

import java.util.Objects;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import icao.iwxxm21.METARType;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class METARIWXXMParser<T> extends MeteorologicalAerodromeObservationReportIWXXMParserBase<T, METAR, METARImpl.Builder> {

    @Override
    protected METARImpl.Builder getEmptyBuilder() {
        return METARImpl.builder();
    }

    @Override
    protected METAR createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<METAR> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        final METARType input;
        if (METARType.class.isAssignableFrom(source.getClass())) {
            input = (METARType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            final JAXBElement<?> je = (JAXBElement<?>) source;
            if (METARType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (METARType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a METAR JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a METAR JAXB element");
        }
        final METARImpl.Builder retval = getBuilder(input, refCtx, result, hints);
        if (retval != null) {
            return retval.build();
        } else {
            return null;
        }
    }

    public static class AsDOM extends METARIWXXMParser<Document> {

        /**
         * Returns the METAR input message as A DOM Document.
         *
         * @param input the XML Document input as a String
         * @return the input parsed as DOM
         * @throws ConversionException if an exception occurs while converting input to DOM
         */
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
            return input;
        }
    }

    public static class AsString extends METARIWXXMParser<String> {

        /**
         * Returns the METAR input message as A DOM Document.
         *
         * @param input the XML Document input as a String
         * @return the input parsed as DOM
         * @throws ConversionException if an exception occurs while converting input to DOM
         */
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }

}
