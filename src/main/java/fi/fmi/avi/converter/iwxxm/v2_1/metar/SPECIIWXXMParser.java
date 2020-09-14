package fi.fmi.avi.converter.iwxxm.v2_1.metar;

import java.util.Objects;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.SPECIImpl;
import icao.iwxxm21.SPECIType;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class SPECIIWXXMParser<T> extends MeteorologicalAerodromeObservationReportIWXXMParserBase<T, SPECI, SPECIImpl.Builder> {

    @Override
    protected SPECIImpl.Builder getEmptyBuilder() {
        return SPECIImpl.builder();
    }

    @Override
    protected SPECI createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<SPECI> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        final SPECIType input;
        if (SPECIType.class.isAssignableFrom(source.getClass())) {
            input = (SPECIType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            final JAXBElement<?> je = (JAXBElement<?>) source;
            if (SPECIType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SPECIType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SPECI JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SPECI JAXB element");
        }
        final SPECIImpl.Builder retval = getBuilder(input, refCtx, result, hints);
        if (retval != null) {
            return retval.build();
        } else {
            return null;
        }
    }

    public static class FromDOM extends SPECIIWXXMParser<Document> {

        /**
         * Returns the SPECI input message as A DOM Document.
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

    public static class FromString extends SPECIIWXXMParser<String> {

        /**
         * Returns the SPECI input message as A DOM Document.
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
