package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.*;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.taf.TAF;
import net.opengis.gml32.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Common functionality for serializing aviation messages into IWXXM.
 */
public abstract class AbstractIWXXMSerializer<T extends AviationWeatherMessageOrCollection, S> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S> {

    protected static <T extends MeasureType> T asMeasure(final NumericMeasure input, final Class<T> measureType) {
        return create(measureType, measure -> {
            measure.setValue(input.getValue());
            measure.setUom("km".equalsIgnoreCase(input.getUom()) ? "km" : input.getUom());
        });
    }

    protected static void setCrsToType(final AbstractGeometryType type, final CoordinateReferenceSystem crs) {
        CRSMappers.abstractGeometryType().setCrsToType(type, crs);
    }

    protected static void setCrsToType(final DirectPositionType type, final CoordinateReferenceSystem crs) {
        CRSMappers.directPositionType().setCrsToType(type, crs);
    }

    protected static String toIWXXMDateTime(final ZonedDateTime time) {
        return time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    protected static Optional<String> toIWXXMDateTime(final PartialOrCompleteTimeInstant instant) {
        return instant.getCompleteTime().map(AbstractIWXXMSerializer::toIWXXMDateTime);
    }

    protected static Optional<String> startToIWXXMDateTime(final PartialOrCompleteTimePeriod period) {
        return period.getStartTime().flatMap(AbstractIWXXMSerializer::toIWXXMDateTime);
    }

    protected static Optional<String> endToIWXXMDateTime(final PartialOrCompleteTimePeriod period) {
        return period.getEndTime().flatMap(AbstractIWXXMSerializer::toIWXXMDateTime);
    }

    public static boolean checkCompleteTimeReferences(final TAF input, final ConversionResult<?> result) {
        boolean referencesComplete = true;
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            referencesComplete = false;
        }
        return referencesComplete;
    }

    protected static TimeInstantType createTimeInstant(final PartialOrCompleteTimeInstant time) {
        final TimeInstantType timeInstant = create(TimeInstantType.class);
        timeInstant.setId(getUUID());
        final TimePositionType timePosition = create(TimePositionType.class);
        toIWXXMDateTime(time).ifPresent(t -> timePosition.getValue().add(t));
        timeInstant.setTimePosition(timePosition);
        return timeInstant;
    }

    public static void createTimePeriodPropertyType(final TimePeriodPropertyType prop, final PartialOrCompleteTimeInstant start,
                                                    final PartialOrCompleteTimeInstant end, final String id) {
        final TimePeriodType tp = create(TimePeriodType.class);
        tp.setId(id);
        final TimePositionType beginPos = create(TimePositionType.class);
        start.getCompleteTime()//
                .map(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .ifPresent(time -> beginPos.getValue().add(time));
        final TimePositionType endPos = create(TimePositionType.class);
        end.getCompleteTime()//
                .map(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .ifPresent(time -> endPos.getValue().add(time));
        tp.setBeginPosition(beginPos);
        tp.setEndPosition(endPos);
        prop.setTimePeriod(tp);
    }

    protected abstract Document renderXMLDocument(final Object input, final ConversionHints hints) throws ConversionException;

    protected Document asCleanedUpXML(final String input, final ConversionHints hints) throws ConversionException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, F_SECURE_PROCESSING);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(input));
            final Document dom3Doc = db.parse(is);
            final DOMResult cleanedResult = new DOMResult();
            final TransformerFactory tFactory = TransformerFactory.newInstance();
            final Transformer transformer = tFactory.newTransformer(new DOMSource(db.parse(getCleanupTransformationStylesheet(hints))));
            final DOMSource dSource = new DOMSource(dom3Doc);
            transformer.transform(dSource, cleanedResult);
            return (Document) cleanedResult.getNode();
        } catch (final ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new ConversionException("Exception in cleaning up", e);
        }
    }

    protected abstract InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException;

    public abstract XMLSchemaInfo getSchemaInfo();

    protected abstract IWXXMNamespaceContext getNamespaceContext();
}
