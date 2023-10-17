package fi.fmi.avi.converter.iwxxm;

import fi.fmi.avi.converter.*;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.taf.TAF;
import net.opengis.gml32.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Common functionality for serializing aviation messages into IWXXM.
 */
public abstract class AbstractIWXXMSerializer<T extends AviationWeatherMessageOrCollection, S> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S> {

    protected static <T extends MeasureType> T asMeasure(final NumericMeasure input, final Class<T> measureType) {
        return create(measureType, measure -> {
            measure.setValue(input.getValue());
            measure.setUom(input.getUom());
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

    @SuppressWarnings("unchecked")
    protected Document renderXMLDocument(final Object input, final ConversionHints hints) throws ConversionException {
        final StringWriter sw = new StringWriter();
        try {
            final Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, getSchemaInfo().getCombinedSchemaLocations());
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", getNamespaceContext());
            marshaller.marshal(wrap(input, (Class<Object>) input.getClass()), sw);
            return asCleanedUpXML(sw.toString(), hints);
        } catch (final JAXBException e) {
            throw new ConversionException("Exception in rendering to DOM", e);
        }
    }

    private Document asCleanedUpXML(final String input, final ConversionHints hints) throws ConversionException {
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

    public boolean checkCompleteTimeReferences(final TAF input, final ConversionResult<?> result) {
        boolean referencesComplete = true;
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            referencesComplete = false;
        }
        return referencesComplete;
    }

    public void checkAerodromeReferencePositions(final TAF input, final ConversionResult<?> result) {
        if (!input.allAerodromeReferencesContainPosition()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.INFO, ConversionIssue.Type.MISSING_DATA,
                    "At least one of the Aerodrome references does not contain reference point location"));
        }
    }

    public void createTimeInstantProperty(final TAF input, final TimeInstantPropertyType prop, final String id) {
        final TimeInstantType ti = create(TimeInstantType.class);
        final TimePositionType tp = create(TimePositionType.class);
        input.getIssueTime()//
                .flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .ifPresent(time -> tp.getValue().add(time));
        ti.setTimePosition(tp);
        ti.setId(id);
        prop.setTimeInstant(ti);
    }

    public void createTimePeriodPropertyType(final TimePeriodPropertyType prop, final PartialOrCompleteTimeInstant start,
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

    protected static String getUUID() {
        return "uuid." + UUID.randomUUID();
    }

    protected abstract InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException;

    public abstract XMLSchemaInfo getSchemaInfo();

    protected abstract IWXXMNamespaceContext getNamespaceContext();
}
