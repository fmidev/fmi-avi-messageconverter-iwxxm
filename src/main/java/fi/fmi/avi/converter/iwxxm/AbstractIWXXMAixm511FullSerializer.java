package fi.fmi.avi.converter.iwxxm;

import aero.aixm511full.SurfacePropertyType;
import aero.aixm511full.SurfaceType;
import aero.aixm511full.ValDistanceVerticalType;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import net.opengis.gml32.*;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Common functionality for serializing aviation messages into IWXXM. Uses the full AIXM 5.1.1 schema.
 */
public abstract class AbstractIWXXMAixm511FullSerializer<T extends AviationWeatherMessageOrCollection, S> extends AbstractIWXXMSerializer<T, S> {

    protected static SurfacePropertyType createAixm511fullSurface(final Geometry geom, final String id) throws IllegalArgumentException {
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
                                        curvet.setId(UUID_PREFIX + UUID.randomUUID());
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
                                        dpl -> dpl.getValue().addAll(polygon.getExteriorRingPositions(Winding.COUNTERCLOCKWISE)));
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

}
