package fi.fmi.avi.converter.iwxxm.v2025_2.taf;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.taf.TAFModelBuilder;
import fi.fmi.avi.converter.iwxxm.taf.TAFProperties;
import fi.fmi.avi.converter.iwxxm.v2025_2.AbstractIWXXM20252Parser;
import fi.fmi.avi.model.taf.TAF;
import icao.iwxxm2025_2.TAFType;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;

public abstract class TAFIWXXMParser<T> extends AbstractIWXXM20252Parser<T, TAF> {

    @Override
    protected TAF createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx, final ConversionResult<TAF> result,
            final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        final TAFType input;

        if (source instanceof TAFType) {
            input = (TAFType) source;
        } else if (source instanceof JAXBElement<?>) {
            final JAXBElement<?> je = (JAXBElement<?>) source;
            if (TAFType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (TAFType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a TAF JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a TAF JAXB element");
        }

        final TAFProperties properties = new TAFProperties();

        final List<ConversionIssue> issues = TAFIWXXMScanner.collectTAFProperties(input, refCtx, properties, hints);
        result.addIssue(issues);

        if (!result.getConversionIssues().isEmpty()) {
            return null;
        }

        return TAFModelBuilder.build(properties);
    }

    public static class FromDOM extends TAFIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends TAFIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }
}
