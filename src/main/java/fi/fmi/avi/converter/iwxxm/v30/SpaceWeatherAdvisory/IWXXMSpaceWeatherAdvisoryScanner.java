package fi.fmi.avi.converter.iwxxm.v30.SpaceWeatherAdvisory;

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
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.NextAdvisoryImpl;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;
import icao.iwxxm30.SpaceWeatherAnalysisPropertyType;
import icao.iwxxm30.SpaceWeatherAnalysisType;
import icao.iwxxm30.SpaceWeatherPhenomenaType;
import icao.iwxxm30.SpaceWeatherRegionPropertyType;
import icao.iwxxm30.SpaceWeatherRegionType;
import icao.iwxxm30.TimeIndicatorType;

public class IWXXMSpaceWeatherAdvisoryScanner extends AbstractIWXXMScanner {
    private static final Logger LOG = LoggerFactory.getLogger(IWXXMSpaceWeatherAdvisoryScanner.class);

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
                if(unitTimeSlicePropertyTypeList.size() > 1) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one unit time slice was found,"
                            + " but not handled"));
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
            AdvisoryNumber replaceAdvisoryNumber = parseAdvisoryNumber(input.getReplacedAdvisoryNumber());
            properties.set(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER, replaceAdvisoryNumber);
        }

        if (input.getPhenomenon() != null) {
            List<String> phenomena = parsePhenomenonList(input.getPhenomenon());
            properties.set(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, phenomena);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Space weather phenomena are missing is missing"));
        }

        //Set analysis
        if (input.getAnalysis().size() == 5) {
            List<SpaceWeatherAnalysisProperties> analyses = getAnalyses(input.getAnalysis(), issueList, refCtx);
            properties.set(SpaceWeatherAdvisoryProperties.Name.ANALYSES, analyses);
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

    private static NextAdvisory getNextAdvisory(TimeInstantPropertyType nextAdvisory, ReferredObjectRetrievalContext refCtx, IssueList issueList) {
        NextAdvisoryImpl.Builder na = NextAdvisoryImpl.builder();
        if (nextAdvisory.getNilReason() != null && nextAdvisory.getNilReason().size() > 0) {
            if(nextAdvisory.getNilReason().size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one nil reason was found,"
                        + " but not reported"));
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
            TimePositionType timePosition =nextAdvisory.getTimeInstant().getTimePosition();
            if (timePosition != null && timePosition.getValue().size() == 1) {
                String time = timePosition.getValue().get(0);
                PartialOrCompleteTimeInstant completeTime = PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(time)).build();
                na.setTime(completeTime);
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory time is expected, but is missing"));
            }
        }
        return na.build();
    }

    private static AdvisoryNumber parseAdvisoryNumber(String advisoryNumber) {
        List<Integer> i = new ArrayList<>();
        for (String element : advisoryNumber.split("/")) {
            i.add(Integer.parseInt(element));
        }

        return AdvisoryNumberImpl.builder().setYear(i.get(0)).setSerialNumber(i.get(1)).build();
    }

    private static List<String> parsePhenomenonList(final List<SpaceWeatherPhenomenaType> elements) {
        List<String> phenomena = new ArrayList<>();
        for (SpaceWeatherPhenomenaType element : elements) {
            phenomena.add(element.getHref());
        }

        return phenomena;
    }

    private static List<SpaceWeatherAnalysisProperties> getAnalyses(final List<SpaceWeatherAnalysisPropertyType> elements, IssueList issueList,
            final ReferredObjectRetrievalContext refCtx) {
        List<SpaceWeatherAnalysisProperties> analyses = new ArrayList<>();
        for (SpaceWeatherAnalysisPropertyType spaceWeatherAnalysisElement : elements) {

            SpaceWeatherAnalysisProperties properties = new SpaceWeatherAnalysisProperties();

            SpaceWeatherAnalysisType spaceWeatherAnalysis = spaceWeatherAnalysisElement.getSpaceWeatherAnalysis();

            properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TIME, getAnalysisTime(spaceWeatherAnalysis, issueList));

            List<SpaceWeatherRegionProperties> regions = getSpaceWeatherRegion(spaceWeatherAnalysis, issueList, refCtx);
            properties.set(SpaceWeatherAnalysisProperties.Name.REGION, regions);

            TimeIndicatorType timIndicator = spaceWeatherAnalysis.getTimeIndicator();
            if (timIndicator != null) {
                if (timIndicator.name().toUpperCase().equals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION.toString())) {
                    properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
                } else {
                    properties.set(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
                }
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis type is missing"));
            }

            //TODO: Make better
            Optional<String> nilReason = Optional.empty();
            Optional<List> analysisProp = properties.get(SpaceWeatherAnalysisProperties.Name.REGION, List.class);
            if (analysisProp.isPresent() && analysisProp.get().size() == 1) {
                nilReason = ((List<SpaceWeatherRegionProperties>) analysisProp.get()).get(0).get(SpaceWeatherRegionProperties.Name.NIL_REASON, String.class);
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, false);
                properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, false);
            }
            if (nilReason.isPresent()) {
                if (nilReason.get().equals("http://codes.wmo.int/common/nil/nothingOfOperationalSignificance")) {
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, false);
                    properties.set(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, true);
                } else if (nilReason.get().equals("Some other valid string")) {
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

    private static PartialOrCompleteTimeInstant getAnalysisTime(SpaceWeatherAnalysisType spaceWeatherAnalysisType, IssueList issueList) {
        TimeInstantType timeInstantType = (TimeInstantType) spaceWeatherAnalysisType.getPhenomenonTime().getAbstractTimeObject().getValue();
        if (timeInstantType != null) {
            List<String> timePositionList = timeInstantType.getTimePosition().getValue();
            if(timePositionList == null || timePositionList.size() == 0) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No time position is missing from list."));
            } else {
                if(timePositionList.size() > 1) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one time position was found."));
                }
                return PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(timePositionList.get(0))).build();
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis time is missing"));
        }
        return null;
    }


    private static List<SpaceWeatherRegionProperties> getSpaceWeatherRegion(SpaceWeatherAnalysisType spaceWeatherAnalysisType, IssueList issueList,
            ReferredObjectRetrievalContext refCtx) {
        List<SpaceWeatherRegionProperties> regions = new ArrayList<>();
        for (SpaceWeatherRegionPropertyType regionPropertyType : spaceWeatherAnalysisType.getRegion()) {
            SpaceWeatherRegionProperties regionProperties = parseSpaceWeatherRegion(regionPropertyType, issueList, null, refCtx);
            regions.add(regionProperties);
        }
        return regions;
    }

    private static SpaceWeatherRegionProperties parseSpaceWeatherRegion(final SpaceWeatherRegionPropertyType regionProperty, IssueList issueList,
            PartialOrCompleteTimeInstant analysisTime, ReferredObjectRetrievalContext refCtx) {
        SpaceWeatherRegionProperties properties = new SpaceWeatherRegionProperties();
        SpaceWeatherRegionType regionType = null;

        if (regionProperty.getHref() != null) {
            Optional<SpaceWeatherRegionType> optional = refCtx.getReferredObject(regionProperty.getHref(), SpaceWeatherRegionType.class);
            if (optional.isPresent()) {
                regionType = optional.get();
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not find Region information"));
                return properties;
            }
        } else if (regionProperty.getNilReason().size() > 0) {
            if(regionProperty.getNilReason().size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than on nil reason was detected."));
            }
            properties.set(SpaceWeatherRegionProperties.Name.NIL_REASON, regionProperty.getNilReason().get(0));
            return properties;
        } else {
            regionType = regionProperty.getSpaceWeatherRegion();
        }

        //TODO: fix bulder method
        AirspaceVolumeImpl.Builder airspaceVolume = AirspaceVolumeImpl.builder();
        SurfaceType surface = regionType.getGeographicLocation()
                .getAirspaceVolume()
                .getHorizontalProjection()
                .getSurface().getValue();

        if(surface.getAxisLabels() != null && surface.getAxisLabels().size() > 0) {
            airspaceVolume.setAxisLabels(surface.getAxisLabels());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Axis labels are missing");
        }

        if(surface.getSrsName() != null) {
            airspaceVolume.setSrsName(surface.getSrsName());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Srs name is missing");
        }

        if(surface.getSrsDimension() != null) {
            airspaceVolume.setSrsDimension(surface.getSrsDimension());
        } else {
            issueList.add(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "Srs dimension is missing");
        }

        List<?> abstractSurfacePatch = surface
                .getPatches()
                .getValue()
                .getAbstractSurfacePatch();

        if(abstractSurfacePatch == null || abstractSurfacePatch.size() == 0) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "No surface patch geometry was found."));
            return null;
        } else if(abstractSurfacePatch.size() > 1) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one surfacpatch geometry was found "
                    + "but not handled."));
        }

        PolygonPatchType polygonPatchType = (PolygonPatchType) ((JAXBElement<AbstractSurfacePatchType>)abstractSurfacePatch.get(0)).getValue();
        Object obj = polygonPatchType.getExterior().getAbstractRing().getValue();

        if (obj instanceof LinearRingType) {
            PointGeometryImpl.Builder pointGeometry = PointGeometryImpl.builder();
            pointGeometry.setPoint(((LinearRingType) obj).getPosList().getValue());
            airspaceVolume.setGeometry(pointGeometry.build());
        } else if (obj instanceof RingType) {
            CircleByCenterPoint cbcp = parseRingType((RingType) obj, issueList);
            airspaceVolume.setGeometry(cbcp);
        }
        AirspaceVolumeType volume = regionType.getGeographicLocation().getAirspaceVolume();

        if (volume.getUpperLimit() != null) {
            NumericMeasureImpl.Builder nm = NumericMeasureImpl.builder()
                    .setValue(Double.parseDouble(volume.getUpperLimit().getValue()))
                    .setUom(volume.getUpperLimit().getUom());
            airspaceVolume.setUpperLimit(nm.build());
            airspaceVolume.setUpperLimitReference(volume.getUpperLimitReference().getValue());
        }

        properties.set(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR, regionType.getLocationIndicator().getHref());
        properties.set(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, airspaceVolume.build());

        return properties;
    }

    private static CircleByCenterPoint parseRingType(final RingType ringType, IssueList issueList) {
        CurveType curveType = null;
        List<CurvePropertyType> curvePropertyTypeList = ringType.getCurveMember();
        if(curvePropertyTypeList == null) {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "CurveMemeber list was empty"));
            return null;
        }
        curveType = (CurveType)curvePropertyTypeList.get(0).getAbstractCurve().getValue();

        if(curvePropertyTypeList.size() > 1) {
            issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one curve type property was found"));
        }

        List<?> abstractCurveSegmentTypeList = curveType.getSegments().getAbstractCurveSegment();
        if(abstractCurveSegmentTypeList != null && abstractCurveSegmentTypeList.size() > 0) {
            if(abstractCurveSegmentTypeList.size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER, "More than one curve segment found in "
                        + "analysis, but not handled"));
            }
            CircleByCenterPointType cbct = (CircleByCenterPointType) ((JAXBElement<AbstractCurveSegmentType>)abstractCurveSegmentTypeList.get(0)).getValue();

            NumericMeasureImpl.Builder radius = NumericMeasureImpl.builder().setUom(cbct.getRadius().getUom()).setValue(cbct.getRadius().getValue());

            CircleByCenterPointImpl.Builder circleRadius = CircleByCenterPointImpl.builder().addAllCoordinates(cbct.getPos().getValue()).setRadius(radius.build());
            return circleRadius.build();
        }
        issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Curve segment missing from analysis."));
        return null;
    }
}
