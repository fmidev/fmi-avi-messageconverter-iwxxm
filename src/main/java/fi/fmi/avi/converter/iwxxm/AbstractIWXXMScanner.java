package fi.fmi.avi.converter.iwxxm;

import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import net.opengis.gml32.AbstractCurveSegmentType;
import net.opengis.gml32.AbstractCurveType;
import net.opengis.gml32.AbstractRingType;
import net.opengis.gml32.AbstractSurfacePatchType;
import net.opengis.gml32.CircleByCenterPointType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveSegmentArrayPropertyType;
import net.opengis.gml32.CurveType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.MeasureType;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.RingType;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.SurfaceType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;

/**
 * Common functionality for parsing validation of IWXXM messages.
 */
public abstract class AbstractIWXXMScanner extends IWXXMConverterBase {

    protected static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval,
            final ReferredObjectRetrievalContext refCtx) {
        final AerodromeImpl.Builder aerodromeBuilder = AerodromeImpl.builder();
        final List<AirportHeliportTimeSlicePropertyType> slices = airport.getTimeSlice();
        if (slices.isEmpty()) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No time slice (for aerodrome)"));
            return Optional.empty();
        } else if (slices.size() != 1) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one time slice (for aerodrome)"));
            return Optional.empty();
        } else {
            final Optional<AirportHeliportTimeSliceType> slice = resolveProperty(slices.get(0), AirportHeliportTimeSliceType.class, refCtx);
            if (slice.isPresent()) {
                final CodeAirportHeliportDesignatorType designator = slice.get().getDesignator();
                if (designator == null || designator.getValue() == null) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No designator for " + "aerodrome"));
                } else {
                    aerodromeBuilder.setDesignator(designator.getValue());
                }
                if (slice.get().getLocationIndicatorICAO() != null) {
                    aerodromeBuilder.setLocationIndicatorICAO(slice.get().getLocationIndicatorICAO().getValue());
                }
                if (slice.get().getDesignatorIATA() != null) {
                    aerodromeBuilder.setDesignatorIATA(slice.get().getDesignatorIATA().getValue());
                }
                if (slice.get().getPortName() != null) {
                    aerodromeBuilder.setName(slice.get().getPortName().getValue());
                }

                ValDistanceVerticalType elevation = slice.get().getFieldElevation();
                final ElevatedPointPropertyType pointProp = slice.get().getARP();
                if (pointProp != null && pointProp.getNilReason() == null) {
                    final Optional<ElevatedPointType> elPoint = resolveProperty(pointProp, ElevatedPointType.class, refCtx);
                    if (elPoint.isPresent()) {
                        final CoordinateReferenceSystemImpl.Builder crsBuilder = mergeToBuilder(elPoint.get(), CoordinateReferenceSystemImpl.builder());
                        final ElevatedPointImpl.Builder posBuilder = ElevatedPointImpl.builder();
                        boolean canBuildPos = true;
                        //use ref point elevation as fallback for the aerodrome elevation
                        if (elevation == null && elPoint.get().getElevation() != null) {
                            elevation = elPoint.get().getElevation();
                            if (elevation.getNilReason() == null) {
                                posBuilder.setNullableElevationUom(elevation.getUom());
                                if (elevation.getValue() != null) {
                                    posBuilder.setElevationValue(Double.parseDouble(elevation.getValue()));
                                }
                            }
                        }
                        if (elPoint.get().getPos() != null) {
                            final DirectPositionType dp = elPoint.get().getPos();
                            try {
                                posBuilder.setCrs(mergeToBuilder(dp, crsBuilder).build());
                            } catch (final IllegalStateException e) {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No SRS for ARP elevated point position: " + e.getMessage()));
                            }
                            if (dp.getValue() != null) {
                                posBuilder.addAllCoordinates(dp.getValue());
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No value for ARP elevated point position"));
                            }

                            if (canBuildPos) {
                                aerodromeBuilder.setReferencePoint(posBuilder.build());
                            }

                        } else if (elPoint.get().getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                    }
                }

                if (elevation != null && elevation.getNilReason() == null) {
                    aerodromeBuilder.setFieldElevationValue(Double.parseDouble(elevation.getValue()));
                    aerodromeBuilder.setNullableFieldElevationUom(elevation.getUom());
                }
            }
        }
        return Optional.of(aerodromeBuilder.build());
    }

    protected static Optional<NumericMeasure> asNumericMeasure(final MeasureType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(NumericMeasureImpl.builder().setValue(source.getValue()).setUom(source.getUom()).build());
    }

    protected static <T> void withNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        withNthNillableChild(parent, child, clz, childElementName, refCtx, 0, valueHandler, nilReasonHandler);
    }

    protected static <T> void withNthNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final int n, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        final List<String> nilReasons = new ArrayList<>();
        if (child == null) {
            final Optional<String> nilReason = refCtx.getNilReasonForNthChild(parent, childElementName, n);
            nilReason.ifPresent(reason -> nilReasons.addAll(Arrays.asList(reason.split("\\s"))));
        } else {
            try {
                final Class[] params = new Class[0];
                final Method getNilReason = clz.getMethod("getNilReason", params);
                final Object[] paramValues = new Object[0];
                final Object value = getNilReason.invoke(child, paramValues);
                if (value != null) {
                    if (List.class.isAssignableFrom(value.getClass())) {
                        final List<?> values = (List<?>) value;
                        for (final Object o : values) {
                            if (o instanceof String) {
                                nilReasons.add((String) o);
                            }
                        }
                    } else if (String.class.isAssignableFrom(value.getClass())) {
                        nilReasons.addAll(Arrays.asList(((String) value).split("\\s")));
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                //NOOP
            }
        }
        if (child != null && nilReasons.isEmpty()) {
            valueHandler.accept(child);
        } else if (!nilReasons.isEmpty()) {
            nilReasonHandler.accept(nilReasons);
        }
    }

    protected static <T> void withEachNillableChild(final Object parent, final Iterable<T> children, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        int i = 0;
        for (final T child : children) {
            withNthNillableChild(parent, child, clz, childElementName, refCtx, i++, valueHandler, nilReasonHandler);
        }
    }

    protected static Optional<Geometry> getSurfaceGeometry(final SurfaceType surface, final IssueList issueList, final ReferredObjectRetrievalContext refCtx) {
        Optional<Geometry> retval = Optional.empty();
        if (surface.getPatches().isNil()) {
            return retval;
        }
        final List<JAXBElement<? extends AbstractSurfacePatchType>> abstractSurfacePatch = surface.getPatches().getValue().getAbstractSurfacePatch();
        CoordinateReferenceSystemImpl crs;
        try {
            crs = mergeToBuilder(surface, CoordinateReferenceSystemImpl.builder()).build();
        } catch (final IllegalStateException e) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No SRS for geometry; " + e.getMessage()));
            crs = null;
        }

        if (abstractSurfacePatch == null || abstractSurfacePatch.size() == 0) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No surface patch geometry was found."));
            return retval;
        } else if (abstractSurfacePatch.size() > 1) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                    "More than one surface patch geometry was found but not handled."));
        }

        final JAXBElement<? extends AbstractSurfacePatchType> patch = abstractSurfacePatch.get(0);
        if (PolygonPatchType.class.isAssignableFrom(patch.getDeclaredType())) {
            final PolygonPatchType polyPatch = (PolygonPatchType) patch.getValue();
            if (polyPatch.getExterior() == null || polyPatch.getExterior().getAbstractRing() == null) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No exterior provided for PolygonPatch"));
            } else {
                final JAXBElement<? extends AbstractRingType> abstractRing = polyPatch.getExterior().getAbstractRing();
                if (LinearRingType.class.isAssignableFrom(abstractRing.getDeclaredType())) {
                    final LinearRingType linearRing = (LinearRingType) abstractRing.getValue();
                    final PolygonGeometryImpl.Builder polygon = PolygonGeometryImpl.builder();
                    polygon.setNullableCrs(crs);
                    polygon.addAllExteriorRingPositions(linearRing.getPosList().getValue());
                    retval = Optional.of(polygon.build());
                } else if (RingType.class.isAssignableFrom(abstractRing.getDeclaredType())) {
                    final RingType ring = (RingType) abstractRing.getValue();
                    final List<CurvePropertyType> members = ring.getCurveMember();
                    if (members.size() > 0) {
                        if (members.size() > 1) {
                            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                                    "More than one curve member was found, only the first one parsed");
                        }
                        final CurvePropertyType cProp = members.get(0);
                        final JAXBElement<? extends AbstractCurveType> firstMember = cProp.getAbstractCurve();
                        if (CurveType.class.isAssignableFrom(firstMember.getDeclaredType())) {
                            final CurveType curve = (CurveType) firstMember.getValue();
                            final CurveSegmentArrayPropertyType segmentProp = curve.getSegments();
                            final List<JAXBElement<? extends AbstractCurveSegmentType>> segments = segmentProp.getAbstractCurveSegment();
                            if (segments.size() > 0) {
                                if (segments.size() > 1) {
                                    issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                                            "More than one curve segment was found, " + "only the first one parsed");
                                }
                                final JAXBElement<? extends AbstractCurveSegmentType> firstSegment = segments.get(0);
                                if (CircleByCenterPointType.class.isAssignableFrom(firstSegment.getDeclaredType())) {
                                    final CircleByCenterPointType cbct = (CircleByCenterPointType) firstSegment.getValue();

                                    final NumericMeasureImpl.Builder radius = NumericMeasureImpl.builder()//
                                            .setUom(cbct.getRadius().getUom())//
                                            .setValue(cbct.getRadius().getValue());

                                    final CircleByCenterPointImpl.Builder circleRadius = CircleByCenterPointImpl.builder()//
                                            .addAllCenterPointCoordinates(cbct.getPos().getValue())//
                                            .setRadius(radius.build())//
                                            .setNullableCrs(crs);
                                    retval = Optional.of(circleRadius.build());
                                } else {
                                    issueList.add(new ConversionIssue(ConversionIssue.Type.OTHER,
                                            "Unsupported Curve segment type " + firstSegment.getDeclaredType().getCanonicalName()));
                                }
                            } else {
                                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No segments for Curve"));
                            }
                        } else {
                            issueList.add(new ConversionIssue(ConversionIssue.Type.OTHER,
                                    "Unsupported Curve type " + firstMember.getDeclaredType().getCanonicalName()));
                        }
                    } else {
                        issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No curve members for Ring"));
                    }
                } else {
                    issueList.add(new ConversionIssue(ConversionIssue.Type.OTHER,
                            "Unsupported AbstractRing type " + abstractRing.getDeclaredType().getCanonicalName()));
                }
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.OTHER, "Unsupported surface patch type " + patch.getDeclaredType().getCanonicalName()));
        }
        return retval;
    }

    public static void withWeatherBuilderFor(String codeListValue, String valueTitle, final ConversionHints hints,
            final Consumer<WeatherImpl.Builder> resultHandler, final Consumer<ConversionIssue> issueHandler) {
        ConversionIssue issue = null;
        if (codeListValue != null && codeListValue.startsWith(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER)) {
            final String code = codeListValue.substring(AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER.length());
            final String description = valueTitle;
            final WeatherImpl.Builder wBuilder = WeatherImpl.builder();
            boolean codeOk = false;
            if (hints == null || hints.isEmpty() || !hints.containsKey(ConversionHints.KEY_WEATHER_CODES) || ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678
                    .equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                // Only the official codes allowed by default
                if (WEATHER_CODES.containsKey(code)) {
                    wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                    codeOk = true;
                } else {
                    issue = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Illegal weather code " + code + " found with strict WMO 4678 " + "checking");
                }
            } else {
                if (ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    wBuilder.setCode(code);
                    if (description != null) {
                        wBuilder.setDescription(description);
                    } else if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setDescription(WEATHER_CODES.get(code));
                    }
                } else if (ConversionHints.VALUE_WEATHER_CODES_IGNORE_NON_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    if (WEATHER_CODES.containsKey(code)) {
                        wBuilder.setCode(code).setDescription(WEATHER_CODES.get(code));
                        codeOk = true;
                    }
                }
            }
            if (codeOk) {
                resultHandler.accept(wBuilder);
            }
        } else {
            issue = new ConversionIssue(ConversionIssue.Type.SYNTAX,
                    "Weather codelist value does not begin with " + AviationCodeListUser.CODELIST_VALUE_PREFIX_SIG_WEATHER);
        }
        if (issue != null) {
            issueHandler.accept(issue);
        }
    }

    protected static CoordinateReferenceSystemImpl.Builder mergeToBuilder(final net.opengis.gml32.AbstractGeometryType source,
            final CoordinateReferenceSystemImpl.Builder builder) {
        return CRSMappers.abstractGeometryType().mergeToBuilder(source, builder);
    }

    protected static CoordinateReferenceSystemImpl.Builder mergeToBuilder(final DirectPositionType source,
            final CoordinateReferenceSystemImpl.Builder builder) {
        return CRSMappers.directPositionType().mergeToBuilder(source, builder);
    }

    protected static <T> T nullToDefault(final T nullableValue, final T defaultValue) {
        return nullableValue == null ? defaultValue : nullableValue;
    }
}
