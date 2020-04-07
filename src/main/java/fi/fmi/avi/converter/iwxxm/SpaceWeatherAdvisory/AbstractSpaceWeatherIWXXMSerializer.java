package fi.fmi.avi.converter.iwxxm.SpaceWeatherAdvisory;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.xml.bind.JAXBElement;

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
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AirspaceVolume;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
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
import icao.iwxxm30.UnitPropertyType;

public abstract class AbstractSpaceWeatherIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<SpaceWeatherAdvisory, T> {
    private aero.aixm511.ObjectFactory aixmObjectFactory = new aero.aixm511.ObjectFactory();
    private net.opengis.gml32.ObjectFactory gmlObjectFactory = new net.opengis.gml32.ObjectFactory();

    protected abstract T render(final SpaceWeatherAdvisoryType swx, final ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisory input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            //TODO: Fix all time references and uncomment below return statement
            //return result;
        }

        SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        //UUID.randomUUID().toString()
        swxType.setId("swx-" + UUID.randomUUID().toString());

        swxType.setIssueTime(create(TimeInstantPropertyType.class, (prop) -> {
            final TimeInstantType ti = create(TimeInstantType.class);
            final TimePositionType tp = create(TimePositionType.class);
            tp.getValue().add(input.getIssueTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            ti.setTimePosition(tp);
            ti.setId("time-" + UUID.randomUUID().toString());
            prop.setTimeInstant(ti);
        }));

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, (prop) -> {
            final UnitType unitType = create(UnitType.class);
            final UnitTimeSlicePropertyType unitTimeSlicePropertyType = create(UnitTimeSlicePropertyType.class);
            final UnitTimeSliceType unitTimeSliceType = create(UnitTimeSliceType.class);
            final TextNameType textNameType = create(TextNameType.class);
            textNameType.setValue(input.getIssuingCenterName());
            unitTimeSliceType.setUnitName(textNameType);
            unitTimeSlicePropertyType.setUnitTimeSlice(unitTimeSliceType);
            unitType.getTimeSlice().add(unitTimeSlicePropertyType);
            prop.setUnit(unitType);
        }));

        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, (prop) -> createAdvisoryNumber(prop, input.getAdvisoryNumber())));

        if (input.getReplaceAdvisoryNumber().isPresent()) {
            swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, (prop) -> createAdvisoryNumber(prop, input.getReplaceAdvisoryNumber().get())));
        }

        swxType.setRemarks(create(StringWithNilReasonType.class, (prop) -> {
            prop.setValue(input.getRemarks().get().get(0));
        }));

        for (String phenomenon : input.getPhenomena()) {
            SpaceWeatherPhenomenaType spaceWeatherPhenomenaType = create(SpaceWeatherPhenomenaType.class);
            spaceWeatherPhenomenaType.setHref(phenomenon);
            swxType.getPhenomenon().add(spaceWeatherPhenomenaType);
        }

        //TODO: Analysis
        //TODO: add ids and checks for identical objects
        for (final SpaceWeatherAdvisoryAnalysis analysis : input.getAnalyses()) {
            SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
            SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);

            analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, (prop) -> {
                final TimeInstantType timeInstantType = create(TimeInstantType.class);
                final TimePositionType timePositionType = create(TimePositionType.class);
                timePositionType.getValue().add(analysis.getTime().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                timeInstantType.setTimePosition(timePositionType);
                final JAXBElement<AbstractTimeObjectType> jaxbElement = gmlObjectFactory.createAbstractTimeObject(new TimeInstantType());
                prop.setAbstractTimeObject(jaxbElement);
            }));

            for (SpaceWeatherRegion region : analysis.getRegion().get()) {
                final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
                final SpaceWeatherRegionType regionType = create(SpaceWeatherRegionType.class);
                final SpaceWeatherLocationType locationType = create(SpaceWeatherLocationType.class);
                if(region.getLocationIndicator().isPresent()) {
                    locationType.setHref(region.getLocationIndicator().get());
                }
                regionType.setLocationIndicator(locationType);

                regionType.setGeographicLocation(create(AirspaceVolumePropertyType.class, (prop) -> {
                    AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);

                    //TODO: check region airspacevolume is present
                    if(region.getAirSpaceVolume().isPresent()) {
                        AirspaceVolume volume = region.getAirSpaceVolume().get();
                        final SurfacePropertyType surfaceProperty = create(SurfacePropertyType.class,
                                (surfacePropertyType) -> createSurfaceProperty(surfacePropertyType, region.getAirSpaceVolume().get()));

                        airspaceVolumeType.setHorizontalProjection(surfaceProperty);

                        if (volume.getUpperLimit().isPresent()) {
                            final CodeVerticalReferenceType codeVerticalReferenceType = create(CodeVerticalReferenceType.class);
                            codeVerticalReferenceType.setValue(volume.getUpperLimitReference().get());

                            final ValDistanceVerticalType valDistanceVerticalType = create(ValDistanceVerticalType.class);
                            valDistanceVerticalType.setUom(volume.getUpperLimit().get().getUom());
                            valDistanceVerticalType.setValue(volume.getUpperLimit().get().getValue().toString());
                            airspaceVolumeType.setUpperLimit(valDistanceVerticalType);
                        }
                    }

                    prop.setAirspaceVolume(airspaceVolumeType);

                }));
            }

            propertyType.setSpaceWeatherAnalysis(analysisType);
        }

        //TODO: NextAdvisory
        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, (prop) -> createNextAdvisory(prop, input.getNextAdvisory())));
        //TODO: Serialize swxType object and add to conversion result
        //result.setConvertedMessage();
        try {
            T val = this.render(swxType, hints);
            System.out.println("asdfghgds");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //result.setConvertedMessage("");
        return result;
    }

    private void createSurfaceProperty(final SurfacePropertyType surfacePropertyType, final AirspaceVolume volume) {
        final SurfaceType surfaceType = create(SurfaceType.class);
        surfaceType.setSrsDimension(volume.getSrsDimension().get());
        surfaceType.setSrsName(volume.getSrsName().get());
        for (String label : volume.getAxisLabels().get()) {
            surfaceType.getAxisLabels().add(label);
        }

        SurfacePatchArrayPropertyType surfacePatchArrayPropertyType = new SurfacePatchArrayPropertyType();
        PolygonPatchType polygonPatchType = create(PolygonPatchType.class);
        AbstractRingPropertyType abstractRingPropertyType = create(AbstractRingPropertyType.class);

        if (volume.getGeometry().get() instanceof PointGeometry) {
            LinearRingType ring = create(LinearRingType.class);
            DirectPositionListType posList = create(DirectPositionListType.class);
            for (Double coordinate : ((PointGeometry) volume.getGeometry().get()).getPoint()) {
                posList.getValue().add(coordinate);
            }
            ring.setPosList(posList);
        } else if (volume.getGeometry().get() instanceof CircleByCenterPoint) {
            final RingType ring = create(RingType.class);
            final CurvePropertyType curvePropertyType = create(CurvePropertyType.class);
            final CurveType curveType = create(CurveType.class);
            final CurveSegmentArrayPropertyType curveSegmentArrayPropertyType = create(CurveSegmentArrayPropertyType.class);
            final ArcByCenterPointType arcByCenterPointType = create(ArcByCenterPointType.class);
            final LengthType lengthType = create(LengthType.class);
            lengthType.setUom(((CircleByCenterPoint) volume.getGeometry().get()).getRadius().getUom());
            lengthType.setValue(((CircleByCenterPoint) volume.getGeometry().get()).getRadius().getValue());
            arcByCenterPointType.setRadius(lengthType);

            final DirectPositionType directPosition = create(DirectPositionType.class);
            for (Double value : ((CircleByCenterPoint) volume.getGeometry().get()).getCoordinates()) {
                directPosition.getValue().add(value);
            }
            arcByCenterPointType.setPos(directPosition);
            curveSegmentArrayPropertyType.getAbstractCurveSegment().add(gmlObjectFactory.createAbstractCurveSegment(arcByCenterPointType));
            curveType.setSegments(curveSegmentArrayPropertyType);
            curvePropertyType.setAbstractCurve(gmlObjectFactory.createCurve(curveType));
            ring.getCurveMember().add(curvePropertyType);
        }
        polygonPatchType.setExterior(abstractRingPropertyType);
        //TODO: conditional type based on volume
        //surfacePatchArrayPropertyType.
        surfacePatchArrayPropertyType.getAbstractSurfacePatch().add(gmlObjectFactory.createAbstractSurfacePatch(polygonPatchType));

        surfaceType.setPatches(gmlObjectFactory.createPatches(surfacePatchArrayPropertyType));

        surfacePropertyType.setSurface(aixmObjectFactory.createSurface(surfaceType));

    }

    private void createNextAdvisory(final TimeInstantPropertyType prop, final NextAdvisory nextAdvisory) {
        if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_AT || nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
            prop.setTimeInstant(create(TimeInstantType.class, (timeInstant) -> {
                final TimePositionType timePosition = create(TimePositionType.class);
                timePosition.getValue().add(nextAdvisory.getTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                timeInstant.setTimePosition(timePosition);
            }));
            //TODO: Where to set BY or AT for next advisory
        } else {
            //TODO: Find and add nilreason to constants;
            prop.getNilReason().add("");
        }
    }

    private void createAdvisoryNumber(StringWithNilReasonType prop, AdvisoryNumber advisoryNumber) {
        final StringBuilder sb = new StringBuilder();
        sb.append(advisoryNumber.getSerialNumber());
        sb.append("/");
        sb.append(advisoryNumber.getYear());
        prop.setValue(sb.toString());
    }

    @Override
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        /*
        final InputStream retval = this.getClass().getResourceAsStream("TAFCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;*/
        return null;
    }
}
