package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import net.opengis.gml32.AbstractCurveSegmentType;
import net.opengis.gml32.AbstractSurfacePatchType;
import net.opengis.gml32.CircleByCenterPointType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.RingType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePositionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.SurfaceType;
import aero.aixm511.UnitTimeSlicePropertyType;
import aero.aixm511.UnitType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMScanner;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.swx.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.immutable.NextAdvisoryImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;
import icao.iwxxm30.SpaceWeatherAnalysisPropertyType;
import icao.iwxxm30.SpaceWeatherAnalysisType;
import icao.iwxxm30.SpaceWeatherPhenomenaType;
import icao.iwxxm30.SpaceWeatherRegionPropertyType;
import icao.iwxxm30.SpaceWeatherRegionType;
import icao.iwxxm30.TimeIndicatorType;

public class SpaceWeatherAdvisoryIWXXMScanner extends AbstractIWXXMScanner {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceWeatherAdvisoryIWXXMScanner.class);

    public static List<ConversionIssue> collectSpaceWeatherAdvisoryProperties(final SpaceWeatherAdvisoryType input, final ReferredObjectRetrievalContext refCtx,
            final SpaceWeatherAdvisoryProperties properties, final ConversionHints hints) {
        IssueList issueList = new IssueList();

        if (input.getIssueTime() != null) {
            Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
            if (!issueTime.isPresent()) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(SpaceWeatherAdvisoryProperties.Name.ISSUE_TIME, issueTime.get());
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
        }

        Optional<UnitType> unitType = resolveProperty(input.getIssuingSpaceWeatherCentre(), UnitType.class, refCtx);
        if (unitType.isPresent()) {
            List<UnitTimeSlicePropertyType> unitTimeSlicePropertyTypeList = unitType.get().getTimeSlice();
            if(unitTimeSlicePropertyTypeList == null || unitTimeSlicePropertyTypeList.size() == 0) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Unit time slice list was empty."));
            } else {
                if (unitTimeSlicePropertyTypeList.size() > 1) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                            "More than one unit time slice was found," + " but not handled"));
                }
                String issuingCenter = unitTimeSlicePropertyTypeList.get(0).getUnitTimeSlice().getUnitName().getValue();
                properties.set(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER_NAME, issuingCenter);
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issuing center name is missing"));
        }

        if (input.getAdvisoryNumber() != null) {
            AdvisoryNumber advisoryNumber = parseAdvisoryNumber(input.getAdvisoryNumber().getValue());
            properties.set(SpaceWeatherAdvisoryProperties.Name.ADVISORY_NUMBER, advisoryNumber);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Advisory number is missing"));
        }

        if (input.getReplacedAdvisoryNumber() != null) {
            final AdvisoryNumber replaceAdvisoryNumber = parseAdvisoryNumber(input.getReplacedAdvisoryNumber());
            properties.set(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER, replaceAdvisoryNumber);
        }

        if (input.getPhenomenon() != null) {
            final List<SpaceWeatherPhenomenon> phenomena = parsePhenomenonList(input.getPhenomenon());
            properties.addAllToList(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, phenomena);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Space weather phenomena are missing is missing"));
        }

        //Set analysis
        if (input.getAnalysis().size() == 5) {
            final List<SpaceWeatherAnalysisProperties> analyses = getAnalyses(input.getAnalysis(), issueList, refCtx);
            properties.addAllToList(SpaceWeatherAdvisoryProperties.Name.ANALYSES, analyses);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.OTHER, "Found incorrect number of analyses."));
        }

        if (input.getRemarks() != null) {
            properties.set(SpaceWeatherAdvisoryProperties.Name.REMARKS, input.getRemarks().getValue());
        }

        if (input.getNextAdvisoryTime() != null) {
            properties.set(SpaceWeatherAdvisoryProperties.Name.NEXT_ADVISORY, getNextAdvisory(input.getNextAdvisoryTime(), refCtx, issueList));
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory is expected, but is missing"));
        }

        return issueList;
    }

    private static NextAdvisory getNextAdvisory(final TimeInstantPropertyType nextAdvisory, final ReferredObjectRetrievalContext refCtx,
            final IssueList issueList) {
        final NextAdvisoryImpl.Builder na = NextAdvisoryImpl.builder();
        if (nextAdvisory.getNilReason() != null && nextAdvisory.getNilReason().size() > 0) {
            if (nextAdvisory.getNilReason().size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                        "More than one nil reason was found," + " but not reported"));
            }
            //TODO: move string to constants
            if (nextAdvisory.getNilReason().get(0).equals("http://codes.wmo.int/common/nil/inapplicable")) {
                na.setTime(Optional.empty());
                na.setTimeSpecifier(NextAdvisory.Type.NO_FURTHER_ADVISORIES);
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory nil reason missing"));
            }
        } else {
            na.setTimeSpecifier(NextAdvisory.Type.NEXT_ADVISORY_AT);
            final TimePositionType timePosition = nextAdvisory.getTimeInstant().getTimePosition();
            if (timePosition != null && timePosition.getValue().size() == 1) {
                final String time = timePosition.getValue().get(0);
                final PartialOrCompleteTimeInstant completeTime = PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(time)).build();
                na.setTime(completeTime);
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory time is expected, but is missing"));
            }
        }
        return na.build();
    }

    private static AdvisoryNumber parseAdvisoryNumber(final String advisoryNumber) {
        final List<Integer> i = new ArrayList<>();
        for (final String element : advisoryNumber.split("/")) {
            i.add(Integer.parseInt(element));
        }

        return AdvisoryNumberImpl.builder().setYear(i.get(0)).setSerialNumber(i.get(1)).build();
    }

    private static List<SpaceWeatherPhenomenon> parsePhenomenonList(final List<SpaceWeatherPhenomenaType> elements) {
        final List<SpaceWeatherPhenomenon> phenomena = new ArrayList<>();
        for (final SpaceWeatherPhenomenaType element : elements) {
            phenomena.add(SpaceWeatherPhenomenon.fromWMOCodeListValue(element.getHref()));
        }

        return phenomena;
    }

    private static List<SpaceWeatherAnalysisProperties> getAnalyses(final List<SpaceWeatherAnalysisPropertyType> elements, final IssueList issueList,
            final ReferredObjectRetrievalContext refCtx) {
        final List<SpaceWeatherAnalysisProperties> analyses = new ArrayList<>();
        for (final SpaceWeatherAnalysisPropertyType spaceWeatherAnalysisElement : elements) {

            final SpaceWeatherAnalysisProperties properties = new SpaceWeatherAnalysisProperties();

            final SpaceWeatherAnalysisType spaceWeatherAnalysis = spaceWeatherAnalysisElement.getSpaceWeatherAnalysis();

            properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TIME, getAnalysisTime(spaceWeatherAnalysis, issueList));

            final List<SpaceWeatherRegionProperties> regions = getSpaceWeatherRegion(spaceWeatherAnalysis, issueList, refCtx);
            properties.addAllToList(SpaceWeatherAnalysisProperties.Name.REGION, regions);

            final TimeIndicatorType timeIndicator = spaceWeatherAnalysis.getTimeIndicator();
            if (timeIndicator != null) {
                if (timeIndicator.name().toUpperCase().equals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION.toString())) {
                    properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
                } else {
                    properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
                }
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis type is missing"));
            }

            //TODO: Make better
            Optional<String> nilReason = Optional.empty();
            final List<SpaceWeatherRegionProperties> analysisProp = properties.getList(SpaceWeatherAnalysisProperties.Name.REGION,
                    SpaceWeatherRegionProperties.class);
            if (analysisProp.size() == 1) {
                nilReason = analysisProp.get(0).get(SpaceWeatherRegionProperties.Name.NIL_REASON, String.class);
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, false);
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, false);
            }
            if (nilReason.isPresent()) {
                if (nilReason.get().equals(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE)) {
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, false);
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, true);
                } else if (nilReason.get().equals("Some other valid string")) { //FIXME!!
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, true);
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, false);
                } else {
                    issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No reason found for missing forecast information"));
                }
            } else {
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, false);
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, false);
            }

            analyses.add(properties);

        }

        return analyses;
    }

    private static PartialOrCompleteTimeInstant getAnalysisTime(final SpaceWeatherAnalysisType spaceWeatherAnalysisType, final IssueList issueList) {
        final TimeInstantType timeInstantType = (TimeInstantType) spaceWeatherAnalysisType.getPhenomenonTime().getAbstractTimeObject().getValue();
        if (timeInstantType != null) {
            final List<String> timePositionList = timeInstantType.getTimePosition().getValue();
            if (timePositionList == null || timePositionList.size() == 0) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No time position is missing from list."));
            } else {
                if (timePositionList.size() > 1) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one time position was found."));
                }
                return PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(timePositionList.get(0))).build();
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis time is missing"));
        }
        return null;
    }

    private static List<SpaceWeatherRegionProperties> getSpaceWeatherRegion(final SpaceWeatherAnalysisType spaceWeatherAnalysisType, final IssueList issueList,
            final ReferredObjectRetrievalContext refCtx) {
        final List<SpaceWeatherRegionProperties> regions = new ArrayList<>();
        for (final SpaceWeatherRegionPropertyType regionPropertyType : spaceWeatherAnalysisType.getRegion()) {
            final SpaceWeatherRegionProperties regionProperties = parseSpaceWeatherRegion(regionPropertyType, issueList, null, refCtx);
            regions.add(regionProperties);
        }
        return regions;
    }

    private static SpaceWeatherRegionProperties parseSpaceWeatherRegion(final SpaceWeatherRegionPropertyType regionProperty, final IssueList issueList,
            final PartialOrCompleteTimeInstant analysisTime, final ReferredObjectRetrievalContext refCtx) {
        final SpaceWeatherRegionProperties properties = new SpaceWeatherRegionProperties();
        SpaceWeatherRegionType regionType = null;

        if (regionProperty.getHref() != null) {
            final Optional<SpaceWeatherRegionType> optional = refCtx.getReferredObject(regionProperty.getHref(), SpaceWeatherRegionType.class);
            if (optional.isPresent()) {
                regionType = optional.get();
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not find Region information"));
                return properties;
            }
        } else if (regionProperty.getNilReason().size() > 0) {
            if (regionProperty.getNilReason().size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than on nil reason was detected."));
            }
            properties.set(SpaceWeatherRegionProperties.Name.NIL_REASON, regionProperty.getNilReason().get(0));
            return properties;
        } else {
            regionType = regionProperty.getSpaceWeatherRegion();
        }

        //TODO: fix bulder method
        final AirspaceVolumeImpl.Builder airspaceVolume = AirspaceVolumeImpl.builder();
        final SurfaceType surface = regionType.getGeographicLocation().getAirspaceVolume().getHorizontalProjection().getSurface().getValue();

        if (surface.getAxisLabels() != null && surface.getAxisLabels().size() > 0) {
            airspaceVolume.setAxisLabels(surface.getAxisLabels());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Axis labels are missing");
        }

        if (surface.getSrsName() != null) {
            airspaceVolume.setSrsName(surface.getSrsName());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Srs name is missing");
        }

        if(surface.getSrsDimension() != null) {
            airspaceVolume.setSrsDimension(surface.getSrsDimension());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Srs dimension is missing");
        }

        final List<?> abstractSurfacePatch = surface.getPatches().getValue().getAbstractSurfacePatch();

        if (abstractSurfacePatch == null || abstractSurfacePatch.size() == 0) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No surface patch geometry was found."));
            return null;
        } else if (abstractSurfacePatch.size() > 1) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                    "More than one surfacpatch geometry was found " + "but not handled."));
        }

        final PolygonPatchType polygonPatchType = (PolygonPatchType) ((JAXBElement<AbstractSurfacePatchType>) abstractSurfacePatch.get(0)).getValue();
        final Object obj = polygonPatchType.getExterior().getAbstractRing().getValue();

        if (obj instanceof LinearRingType) {
            final PointGeometryImpl.Builder pointGeometry = PointGeometryImpl.builder();
            pointGeometry.setPoint(((LinearRingType) obj).getPosList().getValue());
            airspaceVolume.setGeometry(pointGeometry.build());
        } else if (obj instanceof RingType) {
            final CircleByCenterPoint cbcp = parseRingType((RingType) obj, issueList);
            airspaceVolume.setGeometry(cbcp);
        }
        final AirspaceVolumeType volume = regionType.getGeographicLocation().getAirspaceVolume();

        if (volume.getUpperLimit() != null) {
            final NumericMeasureImpl.Builder nm = NumericMeasureImpl.builder()
                    .setValue(Double.parseDouble(volume.getUpperLimit().getValue()))
                    .setUom(volume.getUpperLimit().getUom());
            airspaceVolume.setUpperLimit(nm.build());
            airspaceVolume.setUpperLimitReference(volume.getUpperLimitReference().getValue());
        }

        String locationIndicator = regionType.getLocationIndicator().getHref();
        if (locationIndicator != null) {
            properties.set(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR,
                    SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue(locationIndicator));
        }

        properties.set(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, airspaceVolume.build());

        return properties;
    }

    private static CircleByCenterPoint parseRingType(final RingType ringType, final IssueList issueList) {
        CurveType curveType = null;
        final List<CurvePropertyType> curvePropertyTypeList = ringType.getCurveMember();
        if (curvePropertyTypeList == null) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "CurveMemeber list was empty"));
            return null;
        }
        curveType = (CurveType) curvePropertyTypeList.get(0).getAbstractCurve().getValue();

        if (curvePropertyTypeList.size() > 1) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one curve type property was found"));
        }

        final List<?> abstractCurveSegmentTypeList = curveType.getSegments().getAbstractCurveSegment();
        if(abstractCurveSegmentTypeList != null && abstractCurveSegmentTypeList.size() > 0) {
            if (abstractCurveSegmentTypeList.size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                        "More than one curve segment found in " + "analysis, but not handled"));
            }
            final CircleByCenterPointType cbct = (CircleByCenterPointType) ((JAXBElement<AbstractCurveSegmentType>) abstractCurveSegmentTypeList.get(
                    0)).getValue();

            final NumericMeasureImpl.Builder radius = NumericMeasureImpl.builder().setUom(cbct.getRadius().getUom()).setValue(cbct.getRadius().getValue());

            final CircleByCenterPointImpl.Builder circleRadius = CircleByCenterPointImpl.builder()
                    .addAllCoordinates(cbct.getPos().getValue())
                    .setRadius(radius.build());
            return circleRadius.build();
        }
        issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Curve segment missing from analysis."));
        return null;
    }
}
