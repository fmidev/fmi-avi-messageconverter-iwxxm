package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePositionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.SurfaceType;
import aero.aixm511.UnitTimeSlicePropertyType;
import aero.aixm511.UnitTimeSliceType;
import aero.aixm511.UnitType;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Scanner;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.EnumSpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.swx.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.immutable.IssuingCenterImpl;
import fi.fmi.avi.model.swx.immutable.NextAdvisoryImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;
import icao.iwxxm30.SpaceWeatherAnalysisPropertyType;
import icao.iwxxm30.SpaceWeatherAnalysisType;
import icao.iwxxm30.SpaceWeatherPhenomenaType;
import icao.iwxxm30.SpaceWeatherRegionPropertyType;
import icao.iwxxm30.SpaceWeatherRegionType;
import icao.iwxxm30.TimeIndicatorType;

public class SpaceWeatherAdvisoryIWXXMScanner extends AbstractIWXXM30Scanner {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceWeatherAdvisoryIWXXMScanner.class);
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;

    public static List<ConversionIssue> collectSpaceWeatherAdvisoryProperties(final SpaceWeatherAdvisoryType input, final ReferredObjectRetrievalContext refCtx,
            final SpaceWeatherAdvisoryProperties properties, final ConversionHints hints) {
        final IssueList issueList = new IssueList();

        // report metadata
        final GenericReportProperties meta = new GenericReportProperties();
        issueList.addAll(AbstractIWXXM30Scanner.collectReportMetadata(input, meta, hints));
        properties.set(SpaceWeatherAdvisoryProperties.Name.REPORT_METADATA, meta);

        if (input.getIssueTime() != null) {
            final Optional<PartialOrCompleteTimeInstant> issueTime = getCompleteTimeInstant(input.getIssueTime(), refCtx);
            if (!issueTime.isPresent()) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Issue time is not valid"));
            } else {
                properties.set(SpaceWeatherAdvisoryProperties.Name.ISSUE_TIME, issueTime.get());
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
        }

        final Optional<UnitType> unitType = resolveProperty(input.getIssuingSpaceWeatherCentre(), UnitType.class, refCtx);
        if (unitType.isPresent()) {
            final List<UnitTimeSlicePropertyType> unitTimeSlicePropertyTypeList = unitType.get().getTimeSlice();
            if (unitTimeSlicePropertyTypeList == null || unitTimeSlicePropertyTypeList.size() == 0) {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Unit time slice list was empty."));
            } else {
                if (unitTimeSlicePropertyTypeList.size() > 1) {
                    issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                            "More than one unit time slice was found, but not handled"));
                }
                final UnitTimeSliceType issuingCenterType = unitTimeSlicePropertyTypeList.get(0).getUnitTimeSlice();
                final IssuingCenterImpl.Builder issuingCenterBuilder = IssuingCenterImpl.builder();
                if (issuingCenterType.getDesignator() != null) {
                    if (issuingCenterType.getDesignator().getValue() != null) {
                        issuingCenterBuilder.setDesignator(issuingCenterType.getDesignator().getValue());
                    }
                }
                if (issuingCenterType.getUnitName() != null) {
                    if (issuingCenterType.getUnitName().getValue() != null) {
                        issuingCenterBuilder.setName(issuingCenterType.getUnitName().getValue());
                    }
                }
                if (issuingCenterType.getType() != null) {
                    if (issuingCenterType.getType().getValue() != null) {
                        issuingCenterBuilder.setType(issuingCenterType.getType().getValue());
                    }
                }
                properties.set(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER, issuingCenterBuilder.build());
            }
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issuing center info is missing"));
        }

        if (input.getAdvisoryNumber() != null) {
            final AdvisoryNumber advisoryNumber = AdvisoryNumberImpl.Builder.from(input.getAdvisoryNumber().getValue()).build();
            properties.set(SpaceWeatherAdvisoryProperties.Name.ADVISORY_NUMBER, advisoryNumber);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Advisory number is missing"));
        }

        if (input.getReplacedAdvisoryNumber() != null) {
            final AdvisoryNumber replaceAdvisoryNumber = AdvisoryNumberImpl.Builder.from(input.getReplacedAdvisoryNumber()).build();
            properties.set(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER, replaceAdvisoryNumber);
        }

        if (input.getPhenomenon() != null) {
            final List<SpaceWeatherPhenomenon> phenomena = parsePhenomenonList(input.getPhenomenon());
            properties.addAllToList(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, phenomena);
        } else {
            issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Space weather phenomena are missing is missing"));
        }

        //Set analysis
        if (input.getAnalysis().size() == REQUIRED_NUMBER_OF_ANALYSES) {
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
            if (nextAdvisory.getNilReason().get(0).equals(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)) {
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

    private static List<SpaceWeatherPhenomenon> parsePhenomenonList(final List<SpaceWeatherPhenomenaType> elements) {
        final List<SpaceWeatherPhenomenon> phenomena = new ArrayList<>();
        for (final SpaceWeatherPhenomenaType element : elements) {
            phenomena.add(EnumSpaceWeatherPhenomenon.fromWMOCodeListValue(element.getHref()));
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
                } else if (nilReason.get().equals("Some other valid string")) { //FIXME??
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
            final SpaceWeatherRegionProperties regionProperties = parseSpaceWeatherRegion(regionPropertyType, issueList, refCtx);
            regions.add(regionProperties);
        }
        return regions;
    }

    private static SpaceWeatherRegionProperties parseSpaceWeatherRegion(final SpaceWeatherRegionPropertyType regionProperty, final IssueList issueList,
            final ReferredObjectRetrievalContext refCtx) {
        final SpaceWeatherRegionProperties properties = new SpaceWeatherRegionProperties();
        SpaceWeatherRegionType regionType = null;
        if (regionProperty.getNilReason().size() > 0) {
            if (regionProperty.getNilReason().size() > 1) {
                issueList.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.OTHER,
                        "More than one nil reason was detected, but " + "only the first used"));
            }
            properties.set(SpaceWeatherRegionProperties.Name.NIL_REASON, regionProperty.getNilReason().get(0));
            return properties;
        } else {
            final Optional<SpaceWeatherRegionType> r = resolveProperty(regionProperty, SpaceWeatherRegionType.class, refCtx);
            if (r.isPresent()) {
                regionType = r.get();
            } else {
                issueList.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Could not find Region information"));
                return properties;
            }
        }

        final AirspaceVolumeImpl.Builder airspaceVolume = AirspaceVolumeImpl.builder();
        if (regionType.getGeographicLocation() != null) {
            final Optional<AirspaceVolumeType> volume = resolveProperty(regionType.getGeographicLocation(), AirspaceVolumeType.class, refCtx);
            if (volume.isPresent()) {
                final Optional<SurfaceType> surface = resolveProperty(volume.get().getHorizontalProjection(), SurfaceType.class, refCtx);
                surface.ifPresent(s -> airspaceVolume.setHorizontalProjection(getSurfaceGeometry(s, issueList, refCtx)));
                if (volume.get().getUpperLimit() != null) {
                    final NumericMeasureImpl.Builder nm = NumericMeasureImpl.builder()
                            .setValue(Double.parseDouble(volume.get().getUpperLimit().getValue()))
                            .setUom(volume.get().getUpperLimit().getUom());
                    airspaceVolume.setUpperLimit(nm.build());
                    airspaceVolume.setUpperLimitReference(volume.get().getUpperLimitReference().getValue());
                }
            }
        }

        final String locationIndicator = regionType.getLocationIndicator().getHref();
        if (locationIndicator != null) {
            properties.set(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR,
                    SpaceWeatherRegion.SpaceWeatherLocation.fromWMOCodeListValue(locationIndicator));
        }

        properties.set(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, airspaceVolume.build());

        return properties;
    }

}
