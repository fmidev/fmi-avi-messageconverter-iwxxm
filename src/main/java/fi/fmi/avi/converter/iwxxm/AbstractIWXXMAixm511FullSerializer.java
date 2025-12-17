package fi.fmi.avi.converter.iwxxm;

import aero.aixm511full.SurfacePropertyType;
import aero.aixm511full.SurfaceType;
import aero.aixm511full.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import net.opengis.gml32.*;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Common functionality for serializing aviation messages into IWXXM. Uses the full AIXM 5.1.1 schema.
 */
public abstract class AbstractIWXXMAixm511FullSerializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMSerializer<T, S> {

    private static JAXBContext aixm511FullJaxbContext = null;

    /**
     * Singleton for accessing the shared JAXBContext for IWXXM JAXB handling. Uses the full AIXM 5.1.1 schema.
     * <p>
     * NOTE: this can take several seconds when done for the first time after JVM start,
     * needs to scan all the jars in classpath.
     *
     * @return the context
     * @throws JAXBException if the context cannot be created
     */
    public static synchronized JAXBContext getAixm511FullJAXBContext() throws JAXBException {
        if (aixm511FullJaxbContext == null) {
            aixm511FullJaxbContext = JAXBContext.newInstance("icao.iwxxm2025_2:icao.iwxxm2023_1:aero.aixm511full:net.opengis.gml32:org.iso19139.ogc2007.gmd:org.iso19139.ogc2007.gco:org"
                    + ".iso19139.ogc2007.gss:org.iso19139.ogc2007.gts:org.iso19139.ogc2007.gsr:net.opengis.om20:net.opengis.sampling:net.opengis.sampling"
                    + ".spatial:wmo.metce2013:wmo.opm2013:wmo.collect2014:org.w3c.xlink11");
        }
        return aixm511FullJaxbContext;
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

    /**
     * Creates a surface property from a geometry with a specific winding order enforced.
     *
     * @param geom    the geometry
     * @param id      the surface ID
     * @param winding the winding order to enforce
     * @return the surface property
     */
    protected static SurfacePropertyType createSurface(final Geometry geom, final String id, final Winding winding) throws IllegalArgumentException {
        return createSurfaceInternal(geom, id, winding);
    }

    /**
     * Creates a surface property from a geometry, preserving the original winding order.
     * <p>
     * Use this method when the polygon coordinates should not be reordered,
     * for example when the input data is already in the correct order and
     * winding detection may be unreliable (e.g., SWX polygons crossing the antimeridian).
     * </p>
     *
     * @param geom the geometry
     * @param id   the surface ID
     * @return the surface property
     */
    protected static SurfacePropertyType createSurface(final Geometry geom, final String id) throws IllegalArgumentException {
        return createSurfaceInternal(geom, id, null);
    }

    private static SurfacePropertyType createSurfaceInternal(final Geometry geom, final String id, final Winding winding) throws IllegalArgumentException {
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
                                        curvet.setId(getUUID());
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
                    final List<Double> positions = winding != null ? polygon.getExteriorRingPositions(winding) : polygon.getExteriorRingPositions();
                    final JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> poly.setExterior(
                            create(AbstractRingPropertyType.class, arpt -> arpt.setAbstractRing(createAndWrap(LinearRingType.class, lrt -> {
                                final DirectPositionListType dplt = create(DirectPositionListType.class,
                                        dpl -> dpl.getValue().addAll(positions));
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

    protected Document renderXMLDocument(final Object input, final ConversionHints hints) throws ConversionException {
        final StringWriter sw = new StringWriter();
        try {
            final Marshaller marshaller = getAixm511FullJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, getSchemaInfo().getCombinedSchemaLocations());
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", getNamespaceContext());
            marshaller.marshal(wrap(input, (Class<Object>) input.getClass()), sw);
            return asCleanedUpXML(sw.toString(), hints);
        } catch (final JAXBException e) {
            throw new ConversionException("Exception in rendering to DOM", e);
        }
    }

}
