package fi.fmi.avi.converter.iwxxm;

import aero.aixm511full.*;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.ElevatedPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import net.opengis.gml32.DirectPositionType;

import java.util.List;
import java.util.Optional;

/** Aerodrome scanning with the full AIXM 5.1.1 bindings used since IWXXM 2021-2. */
public abstract class AbstractIWXXMAixm511FullScanner extends AbstractIWXXMScanner {
    protected static Optional<Aerodrome> buildAerodrome(final AirportHeliportType airport, final IssueList retval,
            final ReferredObjectRetrievalContext refCtx) {
        if (airport == null) {
            return Optional.empty();
        }
        final AerodromeImpl.Builder aerodromeBuilder = AerodromeImpl.builder();
        final List<AirportHeliportTimeSlicePropertyType> slices = airport.getTimeSlice();
        if (slices.isEmpty()) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No time slice (for aerodrome)"));
            return Optional.empty();
        } else if (slices.size() != 1) {
            retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "More than one time slice (for aerodrome)"));
            return Optional.empty();
        } else {
            final AirportHeliportTimeSliceType slice = resolveProperty(slices.get(0), AirportHeliportTimeSliceType.class, refCtx).orElse(null);
            if (slice != null) {
                final CodeAirportHeliportDesignatorType designator = slice.getDesignator();
                if (designator == null || designator.getValue() == null) {
                    retval.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No designator for aerodrome"));
                } else {
                    aerodromeBuilder.setDesignator(designator.getValue());
                }
                if (slice.getLocationIndicatorICAO() != null) {
                    aerodromeBuilder.setLocationIndicatorICAO(slice.getLocationIndicatorICAO().getValue());
                }
                if (slice.getDesignatorIATA() != null) {
                    aerodromeBuilder.setDesignatorIATA(slice.getDesignatorIATA().getValue());
                }
                if (slice.getPortName() != null) {
                    aerodromeBuilder.setName(slice.getPortName().getValue());
                }

                final Optional<NumericMeasureImpl> fieldElevation = toNumericMeasure(slice.getFieldElevation(), "fieldElevation", retval);
                aerodromeBuilder.setFieldElevationValue(fieldElevation.map(NumericMeasure::getValue))//
                        .setFieldElevationUom(fieldElevation.map(NumericMeasure::getUom));
                final ElevatedPointPropertyType pointProp = slice.getARP();
                if (pointProp != null && pointProp.getNilReason() == null) {
                    final ElevatedPointType elPoint = resolveProperty(pointProp, ElevatedPointType.class, refCtx).orElse(null);
                    if (elPoint != null) {
                        final CoordinateReferenceSystemImpl.Builder crsBuilder = mergeToBuilder(elPoint, CoordinateReferenceSystemImpl.builder());
                        final ElevatedPointImpl.Builder posBuilder = ElevatedPointImpl.builder();
                        boolean canBuildPos = true;
                        final Optional<NumericMeasureImpl> elevation = toNumericMeasure(elPoint.getElevation(), "elevation", retval);
                        posBuilder.setElevationValue(elevation.map(NumericMeasure::getValue))//
                                .setElevationUom(elevation.map(NumericMeasure::getUom));
                        if (elPoint.getPos() != null) {
                            final DirectPositionType dp = elPoint.getPos();
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
                        } else if (elPoint.getCoordinates() != null) {
                            retval.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                                    "Found elevated position defined using the deprecated GML CoordinatesType, skipping elevated position info"));
                        }
                        final CodeVerticalDatumType verticalDatum = elPoint.getVerticalDatum();
                        if (verticalDatum != null && verticalDatum.getNilReason() == null) {
                            posBuilder.setNullableVerticalDatum(verticalDatum.getValue());
                        }
                        if (canBuildPos) {
                            aerodromeBuilder.setReferencePoint(posBuilder.build());
                        }
                    }
                }
            }
        }
        return Optional.of(aerodromeBuilder.build());
    }

    protected static Optional<NumericMeasureImpl> toNumericMeasure(final ValDistanceVerticalType valDistanceVerticalType, final String elementName,
            final IssueList issueList) {
        if (valDistanceVerticalType == null) {
            return Optional.empty();
        }
        final String value = valDistanceVerticalType.getValue();
        if (value == null) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, elementName + " is missing value"));
            return Optional.empty();
        }
        final String uom = valDistanceVerticalType.getUom();
        if (uom == null) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, elementName + " is missing uom"));
        }

        return Optional.of(NumericMeasureImpl.builder()//
                .setValue(Double.parseDouble(value))//
                .setUom(nullToDefault(uom, ""))//
                .build());
    }
}
