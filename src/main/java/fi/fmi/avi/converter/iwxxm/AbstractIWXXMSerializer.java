package fi.fmi.avi.converter.iwxxm;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
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

import net.opengis.gml32.AbstractGeometryType;
import net.opengis.gml32.AbstractRingPropertyType;
import net.opengis.gml32.CircleByCenterPointType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveSegmentArrayPropertyType;
import net.opengis.gml32.CurveType;
import net.opengis.gml32.DirectPositionListType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.RingType;
import net.opengis.gml32.SurfacePatchArrayPropertyType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.CodeIATAType;
import aero.aixm511.CodeICAOType;
import aero.aixm511.CodeVerticalDatumType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.SurfacePropertyType;
import aero.aixm511.SurfaceType;
import aero.aixm511.TextNameType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.v2_1.airmet.AIRMETIWXXMSerializer;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.CoordinateReferenceSystem;
import fi.fmi.avi.model.ElevatedPoint;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.taf.TAF;

/**
 * Common functionality for serializing aviation messages into IWXXM.
 */
public abstract class AbstractIWXXMSerializer<T extends AviationWeatherMessageOrCollection, S> extends IWXXMConverterBase
        implements AviMessageSpecificConverter<T, S> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIWXXMSerializer.class);

    protected static SurfacePropertyType createSurface(final Geometry geom, final String id) throws IllegalArgumentException {
        SurfacePropertyType retval = null;
        if (geom != null) {
            retval = create(SurfacePropertyType.class, spt -> spt.setSurface(createAndWrap(SurfaceType.class, sft -> {
                geom.getCrs().ifPresent(crs -> setCrsToType(sft, crs));
                sft.setId(id);
                final JAXBElement<SurfacePatchArrayPropertyType> spapt;
                if (CircleByCenterPoint.class.isAssignableFrom(geom.getClass()) || PointGeometry.class.isAssignableFrom(geom.getClass())) {
                    final List<Double> centerPointCoords;
                    final LengthType radius;
                    if (CircleByCenterPoint.class.isAssignableFrom(geom.getClass())) {
                        final CircleByCenterPoint cbcp = (CircleByCenterPoint) geom;
                        centerPointCoords = cbcp.getCenterPointCoordinates();
                        radius = asMeasure(cbcp.getRadius(), LengthType.class);
                    } else {
                        //Create a zero-radius circle if a point geometry is given
                        final PointGeometry point = (PointGeometry) geom;
                        centerPointCoords = point.getCoordinates();
                        radius = asMeasure(NumericMeasureImpl.of(0.0, "[nmi_i]"), LengthType.class);
                    }

                    final JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> poly.setExterior(
                            create(AbstractRingPropertyType.class, arpt -> arpt.setAbstractRing(createAndWrap(RingType.class, rt -> rt.getCurveMember()
                                    .add(create(CurvePropertyType.class, curvept -> curvept.setAbstractCurve(createAndWrap(CurveType.class, curvet -> {
                                        curvet.setId(UUID_PREFIX + UUID.randomUUID().toString());
                                        curvet.setSegments(create(CurveSegmentArrayPropertyType.class,
                                                curvesat -> curvesat.getAbstractCurveSegment().add(createAndWrap(CircleByCenterPointType.class, cbcpt -> {
                                                    cbcpt.setPos(create(DirectPositionType.class, dpt -> dpt.getValue().addAll(centerPointCoords)));
                                                    cbcpt.setNumArc(BigInteger.valueOf(1));
                                                    cbcpt.setRadius(radius);
                                                }))));
                                    })))))))));
                    spapt = createAndWrap(SurfacePatchArrayPropertyType.class, "createPatches", _spapt -> _spapt.getAbstractSurfacePatch().add(ppt));
                } else if (PolygonGeometry.class.isAssignableFrom(geom.getClass())) { //Polygon
                    final PolygonGeometry polygon = (PolygonGeometry) geom;
                    final JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> poly.setExterior(
                            create(AbstractRingPropertyType.class, arpt -> arpt.setAbstractRing(createAndWrap(LinearRingType.class, lrt -> {
                                final DirectPositionListType dplt = create(DirectPositionListType.class,
                                        dpl -> dpl.getValue().addAll(polygon.getExteriorRingPositions()));
                                lrt.setPosList(dplt);
                            })))));
                    spapt = createAndWrap(SurfacePatchArrayPropertyType.class, "createPatches", _spapt -> _spapt.getAbstractSurfacePatch().add(ppt));
                } else {
                    throw new IllegalArgumentException("Unable to create a Surface from geometry of type " + geom.getClass().getCanonicalName());
                }
                if (spapt != null) {
                    sft.setPatches(spapt);
                }
            })));
        }
        return retval;
    }

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
        return instant.getCompleteTime().map(AIRMETIWXXMSerializer::toIWXXMDateTime);
    }

    protected static Optional<String> startToIWXXMDateTime(final PartialOrCompleteTimePeriod period) {
        //noinspection RedundantTypeArguments
        return period.getStartTime().<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime);
    }

    protected static Optional<String> endToIWXXMDateTime(final PartialOrCompleteTimePeriod period) {
        //noinspection RedundantTypeArguments
        return period.getEndTime().<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime);
    }

    protected static Optional<ValDistanceVerticalType> elevationToValDistanceVertical(final ElevatedPoint elevatedPoint) {
        return elevatedPoint == null
                ? Optional.empty()
                : valDistanceVertical(elevatedPoint.getElevationValue().orElse(Double.NaN), elevatedPoint.getElevationUom().orElse(""));
    }

    protected static Optional<ValDistanceVerticalType> toValDistanceVertical(final NumericMeasure numericMeasure) {
        return numericMeasure == null ? Optional.empty() : valDistanceVertical(numericMeasure.getValue(), numericMeasure.getUom());
    }

    protected static Optional<ValDistanceVerticalType> valDistanceVertical(final Double value, final String uom) {
        return valDistanceVertical(value == null ? Double.NaN : value, uom);
    }

    protected static Optional<ValDistanceVerticalType> valDistanceVertical(final double value, final String uom) {
        if (Double.isNaN(value)) {
            return Optional.empty();
        }
        final ValDistanceVerticalType type = create(ValDistanceVerticalType.class);
        final DecimalFormat format = new DecimalFormat("", DecimalFormatSymbols.getInstance(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(4);
        format.setGroupingUsed(false);
        type.setValue(format.format(value));
        if (uom != null && !uom.isEmpty()) {
            type.setUom(uom.toUpperCase(Locale.US));
        }
        return Optional.of(type);
    }

    protected static ValDistanceVerticalType nilValDistanceVertical() {
        final ValDistanceVerticalType type = create(ValDistanceVerticalType.class);
        // TODO: how to set xsi:nil="true"?
        type.setNilReason("unknown");
        type.setUom("OTHER");
        return type;
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

    protected void setAerodromeData(final AirportHeliportType aerodrome, final Aerodrome input, final String aerodromeId, final String timeSliceIdPrefix,
            final String elevatedPointIdPrefix) {
        if (input == null) {
            return;
        }
        aerodrome.setId(aerodromeId);
        aerodrome.getTimeSlice()
                .add(create(AirportHeliportTimeSlicePropertyType.class,
                        prop -> prop.setAirportHeliportTimeSlice(create(AirportHeliportTimeSliceType.class, timeSlice -> {
                            timeSlice.setId(timeSliceIdPrefix + UUID.randomUUID());
                            timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                            timeSlice.setInterpretation("SNAPSHOT");
                            timeSlice.setDesignator(create(CodeAirportHeliportDesignatorType.class, designator -> designator.setValue(input.getDesignator())));
                            input.getName()
                                    .ifPresent(inputName -> timeSlice.setPortName(
                                            create(TextNameType.class, name -> name.setValue(inputName.toUpperCase(Locale.US)))));
                            input.getLocationIndicatorICAO()
                                    .ifPresent(inputLocator -> timeSlice.setLocationIndicatorICAO(
                                            create(CodeICAOType.class, locator -> locator.setValue(inputLocator))));

                            input.getDesignatorIATA()
                                    .ifPresent(inputDesignator -> timeSlice.setDesignatorIATA(
                                            create(CodeIATAType.class, designator -> designator.setValue(inputDesignator))));
                            valDistanceVertical(input.getFieldElevationValue().orElse(Double.NaN), input.getFieldElevationUom().orElse(""))//
                                    .ifPresent(timeSlice::setFieldElevation);

                            input.getReferencePoint()
                                    .ifPresent(inputPosition -> timeSlice.setARP(create(ElevatedPointPropertyType.class,
                                            elevatedPointProp -> elevatedPointProp.setElevatedPoint(create(ElevatedPointType.class, point -> {
                                                point.setId(elevatedPointIdPrefix + UUID.randomUUID());
                                                inputPosition.getCrs().ifPresent(crs -> setCrsToType(point, crs));
                                                if (inputPosition.getCoordinates() != null) {
                                                    point.setPos(
                                                            create(DirectPositionType.class, pos -> pos.getValue().addAll(inputPosition.getCoordinates())));
                                                }
                                                elevationToValDistanceVertical(inputPosition).ifPresent(point::setElevation);
                                                inputPosition.getVerticalDatum()
                                                        .ifPresent(verticalDatum -> point.setVerticalDatum(
                                                                create(CodeVerticalDatumType.class, verticalCode -> verticalCode.setValue(verticalDatum))));
                                            })))));
                        }))));
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
                .<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
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

    protected abstract InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException;

    public abstract XMLSchemaInfo getSchemaInfo();

    protected abstract IWXXMNamespaceContext getNamespaceContext();
}
