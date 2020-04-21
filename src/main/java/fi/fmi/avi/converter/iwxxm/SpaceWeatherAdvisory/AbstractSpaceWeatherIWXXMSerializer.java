package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.gml32.AbstractRingPropertyType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.ArcByCenterPointType;
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
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AirspaceVolume;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
import icao.iwxxm30.PermissibleUsageReasonType;
import icao.iwxxm30.PermissibleUsageType;
import icao.iwxxm30.AbstractTimeObjectPropertyType;
import icao.iwxxm30.AirspaceVolumePropertyType;
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

public abstract class AbstractSpaceWeatherIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<SpaceWeatherAdvisory, T> {
    private aero.aixm511.ObjectFactory AIXM_OF = new aero.aixm511.ObjectFactory();
    private net.opengis.gml32.ObjectFactory GML_OF = new net.opengis.gml32.ObjectFactory();

    protected abstract T render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisory input, final ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            //TODO: Fix all time references and uncomment below return statement
            //return result;
        }

        SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        swxType.setId("uuid." + UUID.randomUUID().toString());

        if(input.getIssueTime().isPresent()) {
            swxType.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> getIssueTime(prop, input.getIssueTime().get())));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
            return result;
        }

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, (prop) -> getIssuingCenter(prop, input.getIssuingCenterName())));

        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, (prop) -> getAdvisoryNumber(prop, input.getAdvisoryNumber())));

        if (input.getReplaceAdvisoryNumber().isPresent()) {
            swxType.setReplacedAdvisoryNumber(getAdvisoryNumber(input.getReplaceAdvisoryNumber().get()));
        }

        if(input.getRemarks().isPresent() && input.getRemarks().get().size() > 0) {
            if(input.getRemarks().get().size() > 1) {
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

        if(input.getAnalyses().size() == 5) {
            SpaceWeatherRegionIdMapper.createIdMap(input.getAnalyses());
            Iterator<SpaceWeatherAdvisoryAnalysis> analysesIterator = input.getAnalyses().iterator();
            int iIndex = 0;
            while(analysesIterator.hasNext()) {
            //for (final SpaceWeatherAdvisoryAnalysis analysis : input.getAnalyses()) {
                final SpaceWeatherAdvisoryAnalysis analysis = analysesIterator.next();
                swxType.getAnalysis().add(getSpaceWeatherAnalyses(analysis, iIndex));
                iIndex++;
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Was expecting 5 analysis objects, but found " + input.getAnalyses().size()));
            return result;
        }

        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, (prop) -> getNextAdvisory(prop, input.getNextAdvisory())));

        try {
            result.setStatus(ConversionResult.Status.SUCCESS);
            this.updateMessageMetadata(input, result, swxType);
            final ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(result);
            //this.validateDocument(swxType, SpaceWeatherAdvisoryType.class, hints, eventHandler);
            if (eventHandler.errorsFound()) {
                result.setStatus(ConversionResult.Status.FAIL);
            } else {
                result.setConvertedMessage(this.render(swxType, hints));
            }
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }
//TODO: Check
    protected void updateMessageMetadata(final SpaceWeatherAdvisory source, final ConversionResult<?> results, final SpaceWeatherAdvisoryType target)
            throws ConversionException {
        try {
            DatatypeFactory f = DatatypeFactory.newInstance();

            //Default permissions
            target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
            target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
            source.getPermissibleUsage().ifPresent((us) -> {
                if (us== AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL) {
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

    private SpaceWeatherAnalysisPropertyType getSpaceWeatherAnalyses(SpaceWeatherAdvisoryAnalysis analysis, int analysisNumber) {
        SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
        SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);


        if(analysis.getAnalysisType().isPresent()) {
            if(analysis.getAnalysisType().get() == SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION) {
                analysisType.setTimeIndicator(TimeIndicatorType.OBSERVATION);
            } else {
                analysisType.setTimeIndicator(TimeIndicatorType.FORECAST);
            }

        }

        analysisType.setId("uuid." + UUID.randomUUID().toString());

        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, (prop) -> getAnalysisTime(prop, analysis.getTime())));

        //TODO: add check for optional
        Iterator<SpaceWeatherRegion> regionIterator = analysis.getRegion().get().iterator();
        int regionNumber = 0;
        //for (SpaceWeatherRegion region : analysis.getRegion().get()) {
        while(regionIterator.hasNext()) {
            SpaceWeatherRegionIdMapper.RegionId regionId = SpaceWeatherRegionIdMapper.getRegionId(analysisNumber, regionNumber);

            final SpaceWeatherRegion region = regionIterator.next();
            final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
            if(!regionId.isDuplicate()) {
                if(!region.getAirSpaceVolume().isPresent() && !region.getLocationIndicator().isPresent() && !region.getTac().isPresent()) {
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
            regionNumber ++;
        }

        propertyType.setSpaceWeatherAnalysis(analysisType);
        return propertyType;
    }

    private void getAnalysisTime(AbstractTimeObjectPropertyType prop, PartialOrCompleteTimeInstant time) {
        final TimeInstantType timeInstantType = create(TimeInstantType.class);
        timeInstantType.setId("uuid." + UUID.randomUUID().toString());
        final TimePositionType timePositionType = create(TimePositionType.class);
        timePositionType.getValue().add(time.getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        timeInstantType.setTimePosition(timePositionType);
        final JAXBElement<AbstractTimeObjectType> jaxbElement = GML_OF.createAbstractTimeObject(timeInstantType);
        prop.setAbstractTimeObject(jaxbElement);
    }


    private void getIssuingCenter(UnitPropertyType prop, String issuingCenter) {
        final UnitType unitType = create(UnitType.class);
        unitType.setId("uuid." + UUID.randomUUID().toString());
        final UnitTimeSlicePropertyType unitTimeSlicePropertyType = create(UnitTimeSlicePropertyType.class);
        final UnitTimeSliceType unitTimeSliceType = create(UnitTimeSliceType.class);
        unitTimeSliceType.setId(UUID.randomUUID().toString());
        final TextNameType textNameType = create(TextNameType.class);
        textNameType.setValue(issuingCenter);
        unitTimeSliceType.setUnitName(textNameType);
        final CodeUnitType codeUnitType = create(CodeUnitType.class);
        //TODO START
        codeUnitType.setValue("OTHER:SWXC");
        unitTimeSliceType.setType(codeUnitType);
        unitTimeSliceType.setInterpretation("SNAPSHOT");
        //TODO END
        unitTimeSlicePropertyType.setUnitTimeSlice(unitTimeSliceType);
        unitType.getTimeSlice().add(unitTimeSlicePropertyType);
        prop.setUnit(unitType);
    }


    private void getIssueTime(TimeInstantPropertyType prop, PartialOrCompleteTimeInstant time) {
        final TimeInstantType ti = create(TimeInstantType.class);
        final TimePositionType tp = create(TimePositionType.class);
        tp.getValue().add(time.getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ti.setTimePosition(tp);
        ti.setId("uuid." + UUID.randomUUID().toString());
        prop.setTimeInstant(ti);
    }

    private void getAirspaceVolumeProperty(AirspaceVolumePropertyType prop, AirspaceVolume volume) {
        //TODO: check region airspacevolume is present

        final AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);
        airspaceVolumeType.setId("uuid." + UUID.randomUUID().toString());
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
        surfaceType.setId("uuid." + UUID.randomUUID().toString());
        for (String label : volume.getAxisLabels().get()) {
            surfaceType.getAxisLabels().add(label);
        }

        SurfacePatchArrayPropertyType surfacePatchArrayPropertyType = new SurfacePatchArrayPropertyType();
        PolygonPatchType polygonPatchType = create(PolygonPatchType.class);
        AbstractRingPropertyType abstractRingPropertyType = create(AbstractRingPropertyType.class);

        if (volume.getGeometry().get() instanceof PointGeometry) {
            getPointGeometry(abstractRingPropertyType, (PointGeometry) volume.getGeometry().get());
        } else if (volume.getGeometry().get() instanceof CircleByCenterPoint) {
            getCircleByCenterPointGeometry(abstractRingPropertyType, (CircleByCenterPoint) volume.getGeometry().get());
        }
        polygonPatchType.setExterior(abstractRingPropertyType);
        surfacePatchArrayPropertyType.getAbstractSurfacePatch().add(GML_OF.createAbstractSurfacePatch(polygonPatchType));

        surfaceType.setPatches(GML_OF.createPatches(surfacePatchArrayPropertyType));

        surfacePropertyType.setSurface(AIXM_OF.createSurface(surfaceType));

    }

    private void getPointGeometry(AbstractRingPropertyType prop, PointGeometry geometry) {
        LinearRingType ring = create(LinearRingType.class);
        DirectPositionListType posList = create(DirectPositionListType.class);
        for (Double coordinate : geometry.getPoint()) {
            posList.getValue().add(coordinate);
        }
        ring.setPosList(posList);
        prop.setAbstractRing(GML_OF.createAbstractRing(ring));
    }

    private void getCircleByCenterPointGeometry(AbstractRingPropertyType prop, CircleByCenterPoint geometry) {
        final RingType ring = create(RingType.class);
        final CurvePropertyType curvePropertyType = create(CurvePropertyType.class);
        final CurveType curveType = create(CurveType.class);
        final CurveSegmentArrayPropertyType curveSegmentArrayPropertyType = create(CurveSegmentArrayPropertyType.class);
        final ArcByCenterPointType arcByCenterPointType = create(ArcByCenterPointType.class);
        final LengthType lengthType = create(LengthType.class);
        lengthType.setUom((geometry.getRadius().getUom()));
        lengthType.setValue(geometry.getRadius().getValue());
        arcByCenterPointType.setRadius(lengthType);

        final DirectPositionType directPosition = create(DirectPositionType.class);
        for (Double value : geometry.getCoordinates()) {
            directPosition.getValue().add(value);
        }
        arcByCenterPointType.setPos(directPosition);
        curveSegmentArrayPropertyType.getAbstractCurveSegment().add(GML_OF.createAbstractCurveSegment(arcByCenterPointType));
        curveType.setSegments(curveSegmentArrayPropertyType);
        curvePropertyType.setAbstractCurve(GML_OF.createCurve(curveType));
        ring.getCurveMember().add(curvePropertyType);
        prop.setAbstractRing(GML_OF.createAbstractRing(ring));
    }

    private void getNextAdvisory(final TimeInstantPropertyType prop, final NextAdvisory nextAdvisory) {
        if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_AT || nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
            final TimeInstantType timeInstant = create(TimeInstantType.class);
            final TimePositionType timePosition = create(TimePositionType.class);
            timePosition.getValue().add(nextAdvisory.getTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            timeInstant.setTimePosition(timePosition);
            prop.setTimeInstant(timeInstant);
            //TODO: Where to set BY or AT for next advisory

        } else {
            prop.getNilReason().add("http://codes.wmo.int/common/nil/inapplicable");
        }
    }

    private void getAdvisoryNumber(StringWithNilReasonType prop, AdvisoryNumber advisoryNumber) {
        prop.setValue(getAdvisoryNumber(advisoryNumber));
    }

    private String getAdvisoryNumber(AdvisoryNumber advisoryNumber) {
        final StringBuilder sb = new StringBuilder();
        sb.append(advisoryNumber.getYear());
        sb.append("/");
        sb.append(advisoryNumber.getSerialNumber());

        return sb.toString();
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        //TODO: fix resource loading
        String usrDir = System.getProperty("user.dir");
        String path = usrDir + "/" + "src/main/resources/fi/fmi/avi/converter/iwxxm/swx/SpaceWeatherAdvisoryCleanUp.xsl";
        InputStream retval = this.getClass().getResourceAsStream("swx/SpaceWeatherAdvisoryCleanUp.xsl");
        if (retval == null) {
            try {
                retval = new FileInputStream(path);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }
}
