package fi.fmi.avi.converter.iwxxm.metar;

import java.util.Objects;

import javax.xml.bind.JAXBElement;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import icao.iwxxm21.METARType;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractMETARIWXXMParser<T> extends MeteorologicalAerodromeObservationReportIWXXMParserBase<T, METAR, METARImpl.Builder> {

    @Override
    protected METARImpl.Builder getEmptyBuilder() {
        return new METARImpl.Builder();
    }

    @Override
    protected METAR createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<METAR> result, final ConversionHints hints) {
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

}
