package fi.fmi.avi.converter.iwxxm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.MeasureType;

import aero.aixm511.AirportHeliportTimeSlicePropertyType;
import aero.aixm511.AirportHeliportTimeSliceType;
import aero.aixm511.AirportHeliportType;
import aero.aixm511.CodeAirportHeliportDesignatorType;
import aero.aixm511.ElevatedPointPropertyType;
import aero.aixm511.ElevatedPointType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import icao.iwxxm21.RelationalOperatorType;

/**
 * Common functionality for parsing validation of IWXXM messages.
 */
public abstract class AbstractIWXXMScanner extends IWXXMConverterBase {

    protected static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval,
            final ReferredObjectRetrievalContext refCtx) {
        AerodromeImpl.Builder aerodromeBuilder = AerodromeImpl.builder();
        List<AirportHeliportTimeSlicePropertyType> slices = airport.getTimeSlice();
        if (slices.isEmpty()) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No time slice (for aerodrome)"));
            return Optional.empty();
        } else if (slices.size() != 1) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one time slice (for aerodrome)"));
            return Optional.empty();
        } else {
            Optional<AirportHeliportTimeSliceType> slice = resolveProperty(slices.get(0), AirportHeliportTimeSliceType.class, refCtx);
            if (slice.isPresent()) {
                CodeAirportHeliportDesignatorType designator = slice.get().getDesignator();
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
                ElevatedPointPropertyType pointProp = slice.get().getARP();
                if (pointProp != null && pointProp.getNilReason() == null) {
                    Optional<ElevatedPointType> elPoint = resolveProperty(pointProp, ElevatedPointType.class, refCtx);
                    if (elPoint.isPresent()) {
                        String srsName = elPoint.get().getSrsName();
                        GeoPositionImpl.Builder posBuilder = GeoPositionImpl.builder();
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
                            DirectPositionType dp = elPoint.get().getPos();
                            if (dp.getSrsName() != null) {
                                srsName = dp.getSrsName();
                            }
                            if (srsName != null) {
                                posBuilder.setCoordinateReferenceSystemId(srsName);
                            } else {
                                canBuildPos = false;
                                retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                        "No SRS name for ARP elevated point position"));
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

    protected static Optional<AviationCodeListUser.RelationalOperator> asRelationalOperator(final RelationalOperatorType source) {
        if (source == null) {
            return Optional.empty();
        }
        return Optional.of(AviationCodeListUser.RelationalOperator.valueOf(source.name()));
    }

    protected static <T> void withNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        withNthNillableChild(parent, child, clz, childElementName, refCtx, 0, valueHandler, nilReasonHandler);
    }

    protected static <T> void withNthNillableChild(final Object parent, final T child, final Class<T> clz, final QName childElementName,
            final ReferredObjectRetrievalContext refCtx, final int n, final Consumer<T> valueHandler, final Consumer<List<String>> nilReasonHandler) {
        List<String> nilReasons = new ArrayList<>();
        if (child == null) {
            Optional<String> nilReason = refCtx.getNilReasonForNthChild(parent, childElementName, n);
            nilReason.ifPresent(reason -> nilReasons.addAll(Arrays.asList(reason.split("\\s"))));
        } else {
            try {
                Class[] params = new Class[0];
                Method getNilReason = clz.getMethod("getNilReason", params);
                Object[] paramValues = new Object[0];
                Object value = getNilReason.invoke(child, paramValues);
                if (value != null) {
                    if (List.class.isAssignableFrom(value.getClass())) {
                        List<?> values = (List<?>) value;
                        for (Object o : values) {
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
        for (T child:children) {
            withNthNillableChild(parent, child, clz, childElementName, refCtx, i++, valueHandler, nilReasonHandler);
        }
    }
}
