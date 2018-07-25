package fi.fmi.avi.converter.iwxxm.metar;

import java.util.Optional;

import javax.xml.bind.JAXBElement;

import com.google.common.base.Preconditions;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMParser;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import icao.iwxxm21.METARType;

/**
 * Created by rinne on 25/07/2018.
 */
public abstract class AbstractMETARIWXXMParser<T> extends AbstractIWXXMParser<T, METAR> {

    protected METAR createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<METAR> result,
            final ConversionHints hints) {
        Preconditions.checkNotNull(source, "source cannot be null");
        METARType input;
        if (METARType.class.isAssignableFrom(source.getClass())) {
            input = (METARType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (METARType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (METARType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a METAR JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a METAR JAXB element");
        }

        METARProperties properties = new METARProperties(input);

        //Other specific validation (using JAXB elements)
        result.addIssue(IWXXMMETARScanner.collectMETARProperties(input, refCtx, properties, hints));

        //Build the METAR:
        Optional<AviationCodeListUser.MetarStatus> status = properties.get(METARProperties.Name.STATUS, AviationCodeListUser.MetarStatus.class);
        if (!status.isPresent()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "METAR status not known, unable to " + "proceed"));
            return null;
        }
        METARImpl.Builder metarBuilder = new METARImpl.Builder();
        metarBuilder.setStatus(status.get());

        //TODO: build from properties

        return metarBuilder.build();
    }
}
