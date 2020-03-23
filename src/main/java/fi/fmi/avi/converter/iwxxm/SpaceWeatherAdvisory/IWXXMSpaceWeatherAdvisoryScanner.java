package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.opengis.gml32.CircleByCenterPointType;
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
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.NextAdvisoryImpl;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.PolygonsGeometryImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
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
            String issuingCenter = unitType.get().getTimeSlice().get(0).getUnitTimeSlice().getUnitName().getValue();
            properties.set(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER_NAME, issuingCenter);
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
        if (nextAdvisory.getNilReason().size() > 0) {
            //TODO: move string to constants
            if (nextAdvisory.getNilReason().get(0).equals("http://codes.wmo.int/common/nil/inapplicable")) {
                na.setTime(Optional.empty());
                na.setTimeSpecifier(NextAdvisory.Type.NO_FURTHER_ADVISORIES);
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory nil reason missing"));
            }
        } else {
            na.setTimeSpecifier(NextAdvisory.Type.NEXT_ADVISORY_AT);
            Optional<TimePositionType> timePosition = resolveProperty(nextAdvisory, TimePositionType.class, refCtx);
            if (timePosition.isPresent() && timePosition.get().getValue().size() == 1) {
                String time = timePosition.get().getValue().get(0);
                PartialOrCompleteTimeInstant completeTime = PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(time)).build();
                na.setTime(completeTime);
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Next advisory time is expected, but is missing"));
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

    private static List<SpaceWeatherRegionProperties> getSpaceWeatherRegion(SpaceWeatherAnalysisType spaceWeatherAnalysisType, IssueList issueList,
            ReferredObjectRetrievalContext refCtx) {
        PartialOrCompleteTimeInstant.Builder analysisTime = null;
        TimeInstantType timeInstantType = (TimeInstantType) spaceWeatherAnalysisType.getPhenomenonTime().getAbstractTimeObject().getValue();
        if (timeInstantType != null) {
            analysisTime = PartialOrCompleteTimeInstant.builder().setCompleteTime(ZonedDateTime.parse(timeInstantType.getTimePosition().getValue().get(0)));
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis time is missing"));
        }

        List<SpaceWeatherRegionProperties> regions = new ArrayList<>();
        for (SpaceWeatherRegionPropertyType regionPropertyType : spaceWeatherAnalysisType.getRegion()) {
            SpaceWeatherRegionProperties regionProperties = parseSpaceWeatherRegion(regionPropertyType, issueList, analysisTime.build(), refCtx);
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
            properties.set(SpaceWeatherRegionProperties.Name.NIL_REASON, regionProperty.getNilReason().get(0));
            return properties;
        } else {
            regionType = regionProperty.getSpaceWeatherRegion();
        }

        PhenomenonGeometryWithHeightImpl.Builder geometryWithHeight = new PhenomenonGeometryWithHeightImpl.Builder();
        if (analysisTime != null) {
            geometryWithHeight.setTime(analysisTime);
        }

        PolygonPatchType polygonPatchType = (PolygonPatchType) regionType.getGeographicLocation()
                .getAirspaceVolume()
                .getHorizontalProjection()
                .getSurface()
                .getValue()
                .getPatches()
                .getValue()
                .getAbstractSurfacePatch()
                .get(0)
                .getValue();
        Object obj = polygonPatchType.getExterior().getAbstractRing().getValue();

        if (obj instanceof LinearRingType) {
            LinearRingType linearRingType = (LinearRingType) obj;
            List<List<Double>> latlonPairs = parseLinearType(linearRingType.getPosList().getValue());
            geometryWithHeight.setGeometry(
                    TacOrGeoGeometryImpl.builder().setGeoGeometry(PolygonsGeometryImpl.builder().setPolygons(latlonPairs).build()).build());
        } else if (obj instanceof RingType) {
            CircleByCenterPoint cbcp = parseRingType((RingType) obj);
            geometryWithHeight.setGeometry(TacOrGeoGeometryImpl.builder().setGeoGeometry(cbcp).build());
        }
        AirspaceVolumeType volume = regionType.getGeographicLocation().getAirspaceVolume();

        if (volume.getUpperLimit() != null) {
            NumericMeasureImpl.Builder nm = NumericMeasureImpl.builder()
                    .setValue(Double.parseDouble(volume.getUpperLimit().getValue()))
                    .setUom(volume.getUpperLimit().getUom());
            geometryWithHeight.setUpperLimit(nm.build());
        }

        if (volume.getLowerLimit() != null) {
            NumericMeasureImpl.Builder nm = NumericMeasureImpl.builder()
                    .setValue(Double.parseDouble(volume.getLowerLimit().getValue()))
                    .setUom(volume.getLowerLimit().getUom());
            geometryWithHeight.setLowerLimit(nm.build());
        }

        properties.set(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR, regionType.getLocationIndicator().getHref());
        properties.set(SpaceWeatherRegionProperties.Name.PHENOMENON_LOCATION, geometryWithHeight.build());

        return properties;
    }

    private static List<List<Double>> parseLinearType(final List<Double> posList) {
        List<List<Double>> pairList = new ArrayList<>();

        for (int i = 0; i < posList.size(); i++) {
            List<Double> latlonPair = new ArrayList<>();
            latlonPair.add(posList.get(i));
            i = i + 1;
            latlonPair.add(posList.get(i));
            pairList.add(latlonPair);
        }

        return pairList;
    }

    private static CircleByCenterPoint parseRingType(final RingType ringType) {
        if (ringType.getCurveMember().size() != 1) {
            //TODO: throw error
        }
        CurveType curveType = (CurveType) ringType.getCurveMember().get(0).getAbstractCurve().getValue();
        CircleByCenterPointType cbct = (CircleByCenterPointType) curveType.getSegments().getAbstractCurveSegment().get(0).getValue();

        NumericMeasureImpl.Builder radius = NumericMeasureImpl.builder().setUom(cbct.getRadius().getUom()).setValue(cbct.getRadius().getValue());

        CircleByCenterPointImpl.Builder circleRadius = CircleByCenterPointImpl.builder().addAllCoordinates(cbct.getPos().getValue()).setRadius(radius.build());

        return circleRadius.build();
    }
}
