package fi.fmi.avi.converter.iwxxm.metar;

import java.util.Objects;

import javax.xml.bind.JAXBElement;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import icao.iwxxm21.SPECIType;

/**
 * Created by rinne on 01/08/2018.
 */
public abstract class AbstractSPECIIWXXMParser<T> extends MeteorologicalAerodromeObservationReportIWXXMParserBase<T, SPECI> {

    @Override
    protected SPECI createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<SPECI> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        SPECIType input;
        if (SPECIType.class.isAssignableFrom(source.getClass())) {
            input = (SPECIType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (SPECIType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SPECIType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SPECI JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SPECI JAXB element");
        }
        METARImpl.Builder retval = getBuilder(input, refCtx, result, hints);
        if (retval != null) {
            return retval.buildAsSPECI();
        } else {
            return null;
        }
    }
}
