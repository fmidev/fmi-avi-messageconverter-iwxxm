package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.io.InputStream;
import java.math.BigInteger;
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
import net.opengis.gml32.TimeIndeterminateValueType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;

import org.w3c.dom.Document;

import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.CodeOrganisationDesignatorType;
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
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.IssuingCenter;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
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
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;
    private static final aero.aixm511.ObjectFactory AIXM_OF = new aero.aixm511.ObjectFactory();
    private static final net.opengis.gml32.ObjectFactory GML_OF = new net.opengis.gml32.ObjectFactory();

    protected abstract T render(SpaceWeatherAdvisoryType swx, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisory input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        final SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        swxType.setId(UUID_PREFIX + UUID.randomUUID().toString());

        if (input.getIssueTime().isPresent()) {
            swxType.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> getIssueTime(prop, input.getIssueTime().get())));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
            return result;
        }

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, (prop) -> getIssuingCenter(prop, input.getIssuingCenter())));

        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, (prop) -> prop.setValue(input.getAdvisoryNumber().asAdvisoryNumber())));

        if (input.getReplaceAdvisoryNumber().isPresent()) {
            swxType.setReplacedAdvisoryNumber(input.getReplaceAdvisoryNumber().get().asAdvisoryNumber());
        }

        final StringWithNilReasonType remarkType = create(StringWithNilReasonType.class);
        if (input.getRemarks().isPresent() && input.getRemarks().get().size() > 0) {
            remarkType.setValue(String.join(" ", input.getRemarks().get()));
        } else {
            remarkType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
        }
        swxType.setRemarks(remarkType);

        for (final SpaceWeatherPhenomenon phenomenon : input.getPhenomena()) {
            final SpaceWeatherPhenomenaType spaceWeatherPhenomenaType = create(SpaceWeatherPhenomenaType.class);
            spaceWeatherPhenomenaType.setHref(phenomenon.asWMOCodeListValue());
            swxType.getPhenomenon().add(spaceWeatherPhenomenaType);
        }

        //Is the fixed number of analysis elements really a specific requirement for IWXXM? Seems oddly precise to me / Ilkka
        if (input.getAnalyses().size() == REQUIRED_NUMBER_OF_ANALYSES) {
            final SpaceWeatherRegionIdMapper regionIdList = new SpaceWeatherRegionIdMapper(input.getAnalyses());
            for (int i = 0; i < input.getAnalyses().size(); i++) {
                final SpaceWeatherAdvisoryAnalysis analysis = input.getAnalyses().get(i);
                swxType.getAnalysis().add(toSpaceWeatherAnalysisPropertyType(analysis, regionIdList.getRegionList(i)));
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Expected 5 analysis objects, but found " + input.getAnalyses().size()));
            return result;
        }

        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, (prop) -> getNextAdvisory(prop, input.getNextAdvisory())));

        try {
            this.updateMessageMetadata(input, result, swxType);
            //validateDocument(swxType, SpaceWeatherAdvisoryType.class, getSchemaInfo(), hints);
            final T rendered = this.render(swxType, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    protected void updateMessageMetadata(final SpaceWeatherAdvisory source, final ConversionResult<?> results, final SpaceWeatherAdvisoryType target)
            throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();

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

            if (source.getPermissibleUsage().isPresent()) {
                if (source.getPermissibleUsage().get().equals(AviationCodeListUser.PermissibleUsage.OPERATIONAL)) {
                    target.setPermissibleUsage(PermissibleUsageType.OPERATIONAL);
                } else {
                    target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
                }
            } else {
                results.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "PermissibleUsage is required"));
            }

            if (source.getPermissibleUsageReason().isPresent()) {
                if (source.getPermissibleUsageReason().get().equals(AviationCodeListUser.PermissibleUsageReason.TEST)) {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
                } else {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.EXERCISE);
                }
            }

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
        } catch (final DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }
    }

    private SpaceWeatherAnalysisPropertyType toSpaceWeatherAnalysisPropertyType(final SpaceWeatherAdvisoryAnalysis analysis,
            final List<SpaceWeatherRegionIdMapper.RegionId> regionList) {
        final SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
        final SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);

        if (analysis.getAnalysisType() == SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION) {
            analysisType.setTimeIndicator(TimeIndicatorType.OBSERVATION);
        } else {
            analysisType.setTimeIndicator(TimeIndicatorType.FORECAST);
        }

        analysisType.setId(UUID_PREFIX + UUID.randomUUID().toString());
        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, (prop) -> getAnalysisTime(prop, analysis.getTime())));

        final int regionsAmount = analysis.getRegions().size();
        if (regionsAmount == 0) {
            final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
            regionProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
            analysisType.getRegion().add(regionProperty);
        }
        for (int i = 0; i < regionsAmount; i++) {
            final SpaceWeatherRegionIdMapper.RegionId regionId = regionList.get(i);
            final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
            if (!regionId.isDuplicate()) {
                final SpaceWeatherRegion region = analysis.getRegions().get(i);
                if (!region.getAirSpaceVolume().isPresent() && !region.getLocationIndicator().isPresent()) {
                    regionProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                } else {
                    final SpaceWeatherRegionType regionType = create(SpaceWeatherRegionType.class);
                    regionType.setId(regionId.getId());

                    final SpaceWeatherLocationType locationType = create(SpaceWeatherLocationType.class);
                    if (region.getLocationIndicator().isPresent()) {
                        locationType.setHref(region.getLocationIndicator().get().asWMOCodeListValue());
                    }
                    regionType.setLocationIndicator(locationType);

                    if (region.getAirSpaceVolume().isPresent()) {
                        regionType.setGeographicLocation(
                                create(AirspaceVolumePropertyType.class, (prop) -> getAirspaceVolumeProperty(prop, region.getAirSpaceVolume().get())));
                    } else {
                        final AirspaceVolumePropertyType airspaceVolumePropertyType = create(AirspaceVolumePropertyType.class);
                        airspaceVolumePropertyType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN);
                        regionType.setGeographicLocation(airspaceVolumePropertyType);
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
        time.getCompleteTime().ifPresent(t -> timePositionType.getValue().add(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        timeInstantType.setTimePosition(timePositionType);
        final JAXBElement<?> jaxbTimeInstant = GML_OF.createTimeInstant(timeInstantType);

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
        if (issuingCenter.getDesignator().isPresent()) {
            final CodeOrganisationDesignatorType designator = new CodeOrganisationDesignatorType();
            designator.setValue(issuingCenter.getDesignator().get());
            unitTimeSliceType.setDesignator(designator);
        }
        unitTimeSliceType.setInterpretation("SNAPSHOT");
        unitTimeSliceType.setValidTime(new TimePrimitivePropertyType());
        unitTimeSlicePropertyType.setUnitTimeSlice(unitTimeSliceType);
        unitType.getTimeSlice().add(unitTimeSlicePropertyType);
        prop.setUnit(unitType);
    }

    private void getIssueTime(final TimeInstantPropertyType prop, final PartialOrCompleteTimeInstant time) {
        final TimeInstantType ti = create(TimeInstantType.class);
        final TimePositionType tp = create(TimePositionType.class);
        time.getCompleteTime().ifPresent(t -> tp.getValue().add(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        ti.setTimePosition(tp);
        ti.setId(UUID_PREFIX + UUID.randomUUID().toString());
        prop.setTimeInstant(ti);
    }

    private void getAirspaceVolumeProperty(final AirspaceVolumePropertyType prop, final AirspaceVolume volume) {
        final AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);
        if (volume.getHorizontalProjection().isPresent()) {
            airspaceVolumeType.setId(UUID_PREFIX + UUID.randomUUID().toString());
            final SurfacePropertyType surfaceProperty = createSurface(volume.getHorizontalProjection().get(), UUID_PREFIX + UUID.randomUUID().toString());
            airspaceVolumeType.setHorizontalProjection(surfaceProperty);
        }

        if (volume.getLowerLimit().isPresent()) {
            // TODO: Once xsi:nil can be set, setLowerLimit can be moved outside presence test
            airspaceVolumeType.setLowerLimit(toValDistanceVerticalType(volume.getLowerLimit().orElse(null)));
            final CodeVerticalReferenceType codeVerticalReferenceType = create(CodeVerticalReferenceType.class);
            volume.getLowerLimitReference().ifPresent(codeVerticalReferenceType::setValue);
            airspaceVolumeType.setLowerLimitReference(codeVerticalReferenceType);
        }
        if (volume.getUpperLimit().isPresent()) {
            // TODO: Once xsi:nil can be set, setLowerLimit can be moved outside presence test
            airspaceVolumeType.setUpperLimit(toValDistanceVerticalType(volume.getUpperLimit().orElse(null)));
            final CodeVerticalReferenceType codeVerticalReferenceType = create(CodeVerticalReferenceType.class);
            volume.getUpperLimitReference().ifPresent(codeVerticalReferenceType::setValue);
            airspaceVolumeType.setUpperLimitReference(codeVerticalReferenceType);
        }

        prop.setAirspaceVolume(airspaceVolumeType);
    }

    private ValDistanceVerticalType toValDistanceVerticalType(final NumericMeasure nullableVerticalDistance) {
        final ValDistanceVerticalType valDistanceVerticalType = create(ValDistanceVerticalType.class);
        if (nullableVerticalDistance == null) {
            // TODO: how to set xsi:nil="true"?
            valDistanceVerticalType.setNilReason("unknown");
            valDistanceVerticalType.setUom("OTHER");
        } else {
            valDistanceVerticalType.setUom(nullableVerticalDistance.getUom());
            valDistanceVerticalType.setValue(nullableVerticalDistance.getValue().toString());
        }
        return valDistanceVerticalType;
    }

    private void getSurfaceProperty(final SurfacePropertyType surfacePropertyType, final AirspaceVolume volume) {
        if (volume.getHorizontalProjection().isPresent()) {
            final SurfaceType surfaceType = create(SurfaceType.class);
            final Geometry geom = volume.getHorizontalProjection().get();
            geom.getCrs().ifPresent(crs -> setCrsToType(surfaceType, crs));
            surfaceType.setId(UUID_PREFIX + UUID.randomUUID().toString());

            final SurfacePatchArrayPropertyType surfacePatchArrayPropertyType = new SurfacePatchArrayPropertyType();
            final PolygonPatchType polygonPatchType = create(PolygonPatchType.class);
            final AbstractRingPropertyType ringPropertyType = create(AbstractRingPropertyType.class);
            if (volume.getHorizontalProjection().isPresent()) {
                if (volume.getHorizontalProjection().get() instanceof PolygonGeometry) {
                    getPolygonGeometry(ringPropertyType, (PolygonGeometry) volume.getHorizontalProjection().get());
                } else if (volume.getHorizontalProjection().get() instanceof CircleByCenterPoint) {
                    getCircleByCenterPointGeometry(ringPropertyType, (CircleByCenterPoint) volume.getHorizontalProjection().get());
                }
            }
            polygonPatchType.setExterior(ringPropertyType);
            surfacePatchArrayPropertyType.getAbstractSurfacePatch().add(GML_OF.createPolygonPatch(polygonPatchType));
            surfaceType.setPatches(GML_OF.createPatches(surfacePatchArrayPropertyType));

            surfacePropertyType.setSurface(AIXM_OF.createSurface(surfaceType));
        }
    }

    private void getPolygonGeometry(final AbstractRingPropertyType prop, final PolygonGeometry geometry) {
        final LinearRingType ring = create(LinearRingType.class);
        final DirectPositionListType posList = create(DirectPositionListType.class);
        posList.getValue().addAll(geometry.getExteriorRingPositions());
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

        //Number of arcs for the circle by center point is always 1:
        circleByCenterPointType.setNumArc(BigInteger.ONE);

        circleByCenterPointType.setRadius(lengthType);

        final DirectPositionType directPosition = create(DirectPositionType.class);
        directPosition.getValue().addAll(geometry.getCenterPointCoordinates());
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
            if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
                timePosition.setIndeterminatePosition(TimeIndeterminateValueType.BEFORE);
            }
            // TODO: 'after' not supported in model; temporarily omit
            nextAdvisory.getTime()
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .ifPresent(t -> timePosition.getValue().add(t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            timeInstant.setId(UUID_PREFIX + UUID.randomUUID().toString());
            timeInstant.setTimePosition(timePosition);
            prop.setTimeInstant(timeInstant);
        } else {
            prop.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
        }
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = this.getClass().getResourceAsStream("SpaceWeatherAdvisoryCleanUp.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class ToDOM extends SpaceWeatherIWXXMSerializer<Document> {

        @Override
        protected Document render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException {
            return this.renderXMLDocument(swx, hints);

        }

        @Override
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return SpaceWeatherIWXXMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

    public static class ToString extends SpaceWeatherIWXXMSerializer<String> {
        @Override
        protected String render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException {
            final Document result = renderXMLDocument(swx, hints);
            return renderDOMToString(result, hints);
        }

        @Override
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return SpaceWeatherIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

}
