package fi.fmi.avi.converter.iwxxm.v3_0.SpaceWeatherAdvisory;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.gml32.AbstractRingPropertyType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.CircleByCenterPointType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveSegmentArrayPropertyType;
import net.opengis.gml32.DirectPositionListType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.RingType;
import net.opengis.gml32.SurfacePatchArrayPropertyType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;

import org.w3c.dom.Document;

import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.CodeUnitType;
import aero.aixm511.CodeVerticalReferenceType;
import aero.aixm511.CurveType;
import aero.aixm511.SurfacePropertyType;
import aero.aixm511.SurfaceType;
import aero.aixm511.TextNameType;
import aero.aixm511.UnitTimeSlicePropertyType;
import aero.aixm511.UnitTimeSliceType;
import aero.aixm511.UnitType;
import aero.aixm511.ValDistanceVerticalType;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AirspaceVolume;
import fi.fmi.avi.model.SpaceWeatherAdvisory.IssuingCenter;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
import icao.iwxxm30.AbstractTimeObjectPropertyType;
import icao.iwxxm30.AirspaceVolumePropertyType;
import icao.iwxxm30.PermissibleUsageReasonType;
import icao.iwxxm30.PermissibleUsageType;
import icao.iwxxm30.ReportStatusType;
import icao.iwxxm30.SpaceWeatherAdvisoryType;
import icao.iwxxm30.SpaceWeatherAnalysisPropertyType;
import icao.iwxxm30.SpaceWeatherAnalysisType;
import icao.iwxxm30.SpaceWeatherLocationType;
import icao.iwxxm30.SpaceWeatherPhenomenaType;
import icao.iwxxm30.SpaceWeatherRegionPropertyType;
import icao.iwxxm30.SpaceWeatherRegionType;
import icao.iwxxm30.StringWithNilReasonType;
import icao.iwxxm30.TimeIndicatorType;
import icao.iwxxm30.UnitPropertyType;

public abstract class SpaceWeatherIWXXMSerializer<T> extends AbstractIWXXM30Serializer<SpaceWeatherAdvisory, T> {
    public static final String UUID_PREFIX = "uuid.";
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;
    private static final aero.aixm511.ObjectFactory AIXM_OF = new aero.aixm511.ObjectFactory();
    private static final net.opengis.gml32.ObjectFactory GML_OF = new net.opengis.gml32.ObjectFactory();

    protected abstract T render(SpaceWeatherAdvisoryType swx, ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisory input, final ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();

        SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        swxType.setId(UUID_PREFIX + UUID.randomUUID().toString());

        if (input.getIssueTime().isPresent()) {
            swxType.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> getIssueTime(prop, input.getIssueTime().get())));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
            return result;
        }

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, (prop) -> getIssuingCenter(prop, input.getIssuingCenter())));

        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, (prop) -> getAdvisoryNumber(prop, input.getAdvisoryNumber())));

        if (input.getReplaceAdvisoryNumber().isPresent()) {
            swxType.setReplacedAdvisoryNumber(getAdvisoryNumber(input.getReplaceAdvisoryNumber().get()));
        }

        if (input.getRemarks().isPresent() && input.getRemarks().get().size() > 0) {
            if (input.getRemarks().get().size() > 1) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, "More than remark was found, but only one was handled"));
            }
            StringWithNilReasonType remark = create(StringWithNilReasonType.class);
            remark.setValue(input.getRemarks().get().get(0));
            swxType.setRemarks(remark);
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Remark is missing"));
        }

        for (String phenomenon : input.getPhenomena()) {
            SpaceWeatherPhenomenaType spaceWeatherPhenomenaType = create(SpaceWeatherPhenomenaType.class);
            spaceWeatherPhenomenaType.setHref(phenomenon);
            swxType.getPhenomenon().add(spaceWeatherPhenomenaType);
        }

        if (input.getAnalyses().size() == REQUIRED_NUMBER_OF_ANALYSES) {
            final SpaceWeatherRegionIdMapper regionIdList = new SpaceWeatherRegionIdMapper(input.getAnalyses());
            for (int i = 0; i < input.getAnalyses().size(); i++) {
                final SpaceWeatherAdvisoryAnalysis analysis = input.getAnalyses().get(i);
                swxType.getAnalysis().add(getSpaceWeatherAnalyses(analysis, regionIdList.getRegionList(i)));
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Expected 5 analysis objects, but found " + input.getAnalyses().size()));
            return result;
        }

        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, (prop) -> getNextAdvisory(prop, input.getNextAdvisory())));

        try {
            this.updateMessageMetadata(input, result, swxType);
            this.validateDocument(swxType, SpaceWeatherAdvisoryType.class, getSchemaInfo(), hints);
            result.setConvertedMessage(this.render(swxType, hints));
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    protected void updateMessageMetadata(final SpaceWeatherAdvisory source, final ConversionResult<?> results, final SpaceWeatherAdvisoryType target)
            throws ConversionException {
        try {
            DatatypeFactory f = DatatypeFactory.newInstance();

            if (source.getReportStatus().isPresent()) {
                if (source.getReportStatus().get().equals(AviationWeatherMessage.ReportStatus.AMENDMENT)) {
                    target.setReportStatus(ReportStatusType.AMENDMENT);
                } else if (source.getReportStatus().get().equals(AviationWeatherMessage.ReportStatus.CORRECTION)) {
                    target.setReportStatus(ReportStatusType.CORRECTION);
                } else if (source.getReportStatus().get().equals(AviationWeatherMessage.ReportStatus.NORMAL)) {
                    target.setReportStatus(ReportStatusType.NORMAL);
                }
            } else {
                results.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Report status is required"));
            }
            target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
            target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
            source.getPermissibleUsage().ifPresent((us) -> {
                if (us == AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL) {
                    target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
                    if (source.getPermissibleUsageReason().isPresent()) {
                        target.setPermissibleUsageReason(PermissibleUsageReasonType.valueOf(source.getPermissibleUsageReason().get().name()));
                    }
                    if ((source.getPermissibleUsageSupplementary() != null) && (source.getPermissibleUsageSupplementary().isPresent())) {
                        target.setPermissibleUsageSupplementary(source.getPermissibleUsageSupplementary().get());
                    }
                }
            });

            if (source.isTranslated()) {
                if (source.getTranslatedBulletinID().isPresent()) {
                    target.setTranslatedBulletinID(source.getTranslatedBulletinID().get());
                }
                if (source.getTranslatedBulletinReceptionTime().isPresent()) {
                    target.setTranslatedBulletinReceptionTime(
                            f.newXMLGregorianCalendar(source.getTranslatedBulletinReceptionTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                if (source.getTranslationCentreDesignator().isPresent()) {
                    target.setTranslationCentreDesignator(source.getTranslationCentreDesignator().get());
                }
                if (source.getTranslationCentreName().isPresent()) {
                    target.setTranslationCentreName(source.getTranslationCentreName().get());
                }
                if (source.getTranslationTime().isPresent()) {
                    target.setTranslationTime(f.newXMLGregorianCalendar(source.getTranslationTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                if (results.getStatus() != ConversionResult.Status.SUCCESS && source.getTranslatedTAC().isPresent()) {
                    target.setTranslationFailedTAC(source.getTranslatedTAC().get());
                }
            }
        } catch (DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
    }

    private SpaceWeatherAnalysisPropertyType getSpaceWeatherAnalyses(final SpaceWeatherAdvisoryAnalysis analysis,
            final List<SpaceWeatherRegionIdMapper.RegionId> regionList) {
        SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
        SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);

        if (analysis.getAnalysisType().isPresent()) {
            if (analysis.getAnalysisType().get() == SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION) {
                analysisType.setTimeIndicator(TimeIndicatorType.OBSERVATION);
            } else {
                analysisType.setTimeIndicator(TimeIndicatorType.FORECAST);
            }

        }
        analysisType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, (prop) -> getAnalysisTime(prop, analysis.getTime())));

        for (int i = 0; i < analysis.getRegion().get().size(); i++) {
            final SpaceWeatherRegionIdMapper.RegionId regionId = regionList.get(i);
            final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
            if (!regionId.isDuplicate()) {
                final SpaceWeatherRegion region = analysis.getRegion().get().get(i);
                if (!region.getAirSpaceVolume().isPresent() && !region.getLocationIndicator().isPresent() && !region.getTac().isPresent()) {
                    regionProperty.getNilReason().add("http://codes.wmo.int/common/nil/nothingOfOperationalSignificance");
                } else {
                    final SpaceWeatherRegionType regionType = create(SpaceWeatherRegionType.class);
                    regionType.setId(regionId.getId());

                    final SpaceWeatherLocationType locationType = create(SpaceWeatherLocationType.class);
                    if (region.getLocationIndicator().isPresent()) {
                        locationType.setHref(region.getLocationIndicator().get());
                    }
                    regionType.setLocationIndicator(locationType);

                    if (region.getAirSpaceVolume().isPresent()) {
                        regionType.setGeographicLocation(
                                create(AirspaceVolumePropertyType.class, (prop) -> getAirspaceVolumeProperty(prop, region.getAirSpaceVolume().get())));
                    }

                    regionProperty.setSpaceWeatherRegion(regionType);
                }
            } else {
                regionProperty.setHref("#" + regionId.getId());
            }
            analysisType.getRegion().add(regionProperty);
        }

        propertyType.setSpaceWeatherAnalysis(analysisType);
        return propertyType;
    }

    private void getAnalysisTime(final AbstractTimeObjectPropertyType prop, final PartialOrCompleteTimeInstant time) {
        final TimeInstantType timeInstantType = create(TimeInstantType.class);
        timeInstantType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        final TimePositionType timePositionType = create(TimePositionType.class);
        timePositionType.getValue().add(time.getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        timeInstantType.setTimePosition(timePositionType);
        JAXBElement<?> jaxbTimeInstant = GML_OF.createTimeInstant(timeInstantType);

        prop.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) jaxbTimeInstant);
    }

    private void getIssuingCenter(final UnitPropertyType prop, final IssuingCenter issuingCenter) {
        final UnitType unitType = create(UnitType.class);
        unitType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        final UnitTimeSlicePropertyType unitTimeSlicePropertyType = create(UnitTimeSlicePropertyType.class);
        final UnitTimeSliceType unitTimeSliceType = create(UnitTimeSliceType.class);
        unitTimeSliceType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        final TextNameType textNameType = create(TextNameType.class);
        if (issuingCenter.getName().isPresent()) {
            textNameType.setValue(issuingCenter.getName().get());
            unitTimeSliceType.setUnitName(textNameType);
        }
        final CodeUnitType codeUnitType = create(CodeUnitType.class);
        if (issuingCenter.getType().isPresent()) {
            codeUnitType.setValue(issuingCenter.getType().get());
            unitTimeSliceType.setType(codeUnitType);
        }
        if (issuingCenter.getInterpretation().isPresent()) {
            unitTimeSliceType.setInterpretation(issuingCenter.getInterpretation().get());
        }
        unitTimeSliceType.setValidTime(new TimePrimitivePropertyType());
        unitTimeSlicePropertyType.setUnitTimeSlice(unitTimeSliceType);
        unitType.getTimeSlice().add(unitTimeSlicePropertyType);
        prop.setUnit(unitType);
    }

    private void getIssueTime(final TimeInstantPropertyType prop, final PartialOrCompleteTimeInstant time) {
        final TimeInstantType ti = create(TimeInstantType.class);
        final TimePositionType tp = create(TimePositionType.class);
        tp.getValue().add(time.getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ti.setTimePosition(tp);
        ti.setId(UUID_PREFIX + UUID.randomUUID().toString());
        prop.setTimeInstant(ti);
    }

    private void getAirspaceVolumeProperty(final AirspaceVolumePropertyType prop, final AirspaceVolume volume) {
        final AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);
        airspaceVolumeType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        final SurfacePropertyType surfaceProperty = create(SurfacePropertyType.class, (surfacePropertyType) -> getSurfaceProperty(surfacePropertyType, volume));

        airspaceVolumeType.setHorizontalProjection(surfaceProperty);

        if (volume.getUpperLimit().isPresent()) {
            final CodeVerticalReferenceType codeVerticalReferenceType = create(CodeVerticalReferenceType.class);
            codeVerticalReferenceType.setValue(volume.getUpperLimitReference().get());
            airspaceVolumeType.setUpperLimitReference(codeVerticalReferenceType);

            final ValDistanceVerticalType valDistanceVerticalType = create(ValDistanceVerticalType.class);
            valDistanceVerticalType.setUom(volume.getUpperLimit().get().getUom());
            valDistanceVerticalType.setValue(volume.getUpperLimit().get().getValue().toString());
            airspaceVolumeType.setUpperLimit(valDistanceVerticalType);
        }

        prop.setAirspaceVolume(airspaceVolumeType);
    }

    private void getSurfaceProperty(final SurfacePropertyType surfacePropertyType, final AirspaceVolume volume) {
        final SurfaceType surfaceType = create(SurfaceType.class);
        surfaceType.setSrsDimension(volume.getSrsDimension().get());
        surfaceType.setSrsName(volume.getSrsName().get());
        surfaceType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        for (String label : volume.getAxisLabels().get()) {
            surfaceType.getAxisLabels().add(label);
        }

        SurfacePatchArrayPropertyType surfacePatchArrayPropertyType = new SurfacePatchArrayPropertyType();
        PolygonPatchType polygonPatchType = create(PolygonPatchType.class);
        AbstractRingPropertyType ringPropertyType = create(AbstractRingPropertyType.class);

        if (volume.getGeometry().get() instanceof PointGeometry) {
            getPointGeometry(ringPropertyType, (PointGeometry) volume.getGeometry().get());
        } else if (volume.getGeometry().get() instanceof CircleByCenterPoint) {
            getCircleByCenterPointGeometry(ringPropertyType, (CircleByCenterPoint) volume.getGeometry().get());
        }
        polygonPatchType.setExterior(ringPropertyType);
        surfacePatchArrayPropertyType.getAbstractSurfacePatch().add(GML_OF.createPolygonPatch(polygonPatchType));
        surfaceType.setPatches(GML_OF.createPatches(surfacePatchArrayPropertyType));

        surfacePropertyType.setSurface(AIXM_OF.createSurface(surfaceType));

    }

    private void getPointGeometry(final AbstractRingPropertyType prop, final PointGeometry geometry) {
        LinearRingType ring = create(LinearRingType.class);
        DirectPositionListType posList = create(DirectPositionListType.class);
        for (Double coordinate : geometry.getPoint()) {
            posList.getValue().add(coordinate);
        }
        ring.setPosList(posList);
        prop.setAbstractRing(GML_OF.createLinearRing(ring));
    }

    private void getCircleByCenterPointGeometry(final AbstractRingPropertyType prop, final CircleByCenterPoint geometry) {
        final RingType ring = create(RingType.class);
        final CurvePropertyType curvePropertyType = create(CurvePropertyType.class);
        final CurveType curveType = create(CurveType.class);
        curveType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        final CurveSegmentArrayPropertyType curveSegmentArrayPropertyType = create(CurveSegmentArrayPropertyType.class);
        final CircleByCenterPointType circleByCenterPointType = create(CircleByCenterPointType.class);
        final LengthType lengthType = create(LengthType.class);
        lengthType.setUom((geometry.getRadius().getUom()));
        lengthType.setValue(geometry.getRadius().getValue());
        if (geometry.getNumarc().isPresent()) {
            circleByCenterPointType.setNumArc(geometry.getNumarc().get());
        }
        circleByCenterPointType.setRadius(lengthType);

        final DirectPositionType directPosition = create(DirectPositionType.class);
        for (Double value : geometry.getCoordinates()) {
            directPosition.getValue().add(value);
        }
        circleByCenterPointType.setPos(directPosition);
        curveSegmentArrayPropertyType.getAbstractCurveSegment().add(GML_OF.createCircleByCenterPoint(circleByCenterPointType));
        curveType.setSegments(curveSegmentArrayPropertyType);
        curvePropertyType.setAbstractCurve(GML_OF.createCurve(curveType));
        ring.getCurveMember().add(curvePropertyType);
        prop.setAbstractRing(GML_OF.createRing(ring));
    }

    private void getNextAdvisory(final TimeInstantPropertyType prop, final NextAdvisory nextAdvisory) {
        if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_AT || nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
            final TimeInstantType timeInstant = create(TimeInstantType.class);
            final TimePositionType timePosition = create(TimePositionType.class);
            timePosition.getValue().add(nextAdvisory.getTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            timeInstant.setId(UUID_PREFIX + UUID.randomUUID().toString());
            timeInstant.setTimePosition(timePosition);
            prop.setTimeInstant(timeInstant);
        } else {
            prop.getNilReason().add("http://codes.wmo.int/common/nil/inapplicable");
        }
    }

    private void getAdvisoryNumber(final StringWithNilReasonType prop, final AdvisoryNumber advisoryNumber) {
        prop.setValue(getAdvisoryNumber(advisoryNumber));
    }

    private String getAdvisoryNumber(final AdvisoryNumber advisoryNumber) {
        final StringBuilder sb = new StringBuilder();
        sb.append(advisoryNumber.getYear());
        sb.append("/");
        sb.append(advisoryNumber.getSerialNumber());

        return sb.toString();
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        InputStream retval = this.getClass().getResourceAsStream("SpaceWeatherAdvisoryCleanUp.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class AsDOM extends SpaceWeatherIWXXMSerializer<Document> {

        @Override
        protected Document render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException {
            return this.renderXMLDocument(swx, hints);

        }
    }

    public static class AsString extends SpaceWeatherIWXXMSerializer<String> {
        @Override
        protected String render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException {
            Document result = renderXMLDocument(swx, hints);
            return renderDOMToString(result, hints);
        }
    }

}
