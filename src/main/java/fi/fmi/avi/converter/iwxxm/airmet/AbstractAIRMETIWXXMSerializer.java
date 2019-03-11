package fi.fmi.avi.converter.iwxxm.airmet;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import net.opengis.gml32.AbstractRingPropertyType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.CircleByCenterPointType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveSegmentArrayPropertyType;
import net.opengis.gml32.CurveType;
import net.opengis.gml32.DirectPositionListType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.LinearRingType;
import net.opengis.gml32.ObjectFactory;
import net.opengis.gml32.PolygonPatchType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.RingType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.StringOrRefType;
import net.opengis.gml32.SurfacePatchArrayPropertyType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;
import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.OMProcessPropertyType;
import net.opengis.om20.TimeObjectPropertyType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import aero.aixm511.AirspaceTimeSlicePropertyType;
import aero.aixm511.AirspaceTimeSliceType;
import aero.aixm511.AirspaceType;
import aero.aixm511.AirspaceVolumePropertyType;
import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.CodeAirspaceDesignatorType;
import aero.aixm511.CodeAirspaceType;
import aero.aixm511.CodeOrganisationDesignatorType;
import aero.aixm511.CodeUnitType;
import aero.aixm511.CodeVerticalReferenceType;
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
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import icao.iwxxm21.AIRMETEvolvingConditionCollectionPropertyType;
import icao.iwxxm21.AIRMETEvolvingConditionCollectionType;
import icao.iwxxm21.AIRMETEvolvingConditionPropertyType;
import icao.iwxxm21.AIRMETEvolvingConditionType;
import icao.iwxxm21.AIRMETExpectedIntensityChangeType;
import icao.iwxxm21.AIRMETReportStatusType;
import icao.iwxxm21.AIRMETType;
import icao.iwxxm21.AeronauticalAreaWeatherPhenomenonType;
import icao.iwxxm21.AeronauticalSignificantWeatherPhenomenonType;
import icao.iwxxm21.AirspacePropertyType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.ExpectedIntensityChangeType;
import icao.iwxxm21.PermissibleUsageReasonType;
import icao.iwxxm21.PermissibleUsageType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.TimeIndicatorType;
import icao.iwxxm21.UnitPropertyType;
import wmo.metce2013.ProcessType;

public abstract class AbstractAIRMETIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<AIRMET, T> {
    protected abstract T render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException;

    /**
     * Converts a TAF object into another format.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the conversion result.
     */
    @Override
    public ConversionResult<T> convertMessage(final AIRMET input, final ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();
        System.err.println("AIRMET convertMessage(" + input.getAirmetPhenomenon() + ")");

        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String airmetUuid = UUID.randomUUID().toString();
        final String validTimeId = "validt-" + airmetUuid;
        final String phenomenonTimeId = "phent-" + airmetUuid;
        final String resultTimeId = "resltt-" + airmetUuid;
        final String procedureId = "proc-" + airmetUuid;
        final String sfSpatialId = "sampling-surface-" + airmetUuid;
        final String foiId = "foi-" + airmetUuid;

        final AIRMETType airmet;

        airmet = create(AIRMETType.class);
        airmet.setId("as-" + UUID.randomUUID().toString());

        
        
        //Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().getCompleteTime().orElse(ZonedDateTime.now()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        if (input.getCancelledReference().isPresent()) {
            airmet.setStatus(AIRMETReportStatusType.CANCELLATION);
        } else {
            airmet.setStatus(AIRMETReportStatusType.NORMAL);
        }
        airmet.setIssuingAirTrafficServicesUnit(create(UnitPropertyType.class, (prop) -> {
            prop.setUnit(create(UnitType.class, (unit) -> {
                unit.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + airmetUuid);
                unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> {
                    sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, (slice) -> {
                        slice.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-ts-" + airmetUuid);
                        slice.setValidTime(create(TimePrimitivePropertyType.class, (tp) -> {
                        }));
                        slice.setInterpretation("SNAPSHOT");
                        slice.setType(create(CodeUnitType.class, (codeUnitType) -> {
                            codeUnitType.setValue("FIC");
                        }));
                        slice.setUnitName(create(TextNameType.class, (tnt) -> {
                            tnt.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator());
                            slice.setDesignator(create(CodeOrganisationDesignatorType.class, (desig) -> {
                                desig.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator());
                            }));
                        }));
                    }));
                }));
            }));
        }));

        airmet.setOriginatingMeteorologicalWatchOffice(create(UnitPropertyType.class, (prop) -> {
            prop.setUnit(create(UnitType.class, (unit) -> {
                unit.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-" + airmetUuid);
                unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, (sliceProp) -> {
                    sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, (slice) -> {
                        slice.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-ts-" + airmetUuid);
                        slice.setValidTime(create(TimePrimitivePropertyType.class, (tp) -> {
                        }));
                        slice.setInterpretation("SNAPSHOT");
                        slice.setType(create(CodeUnitType.class, (codeUnitType) -> {
                            codeUnitType.setValue("MWO");
                        }));
                        slice.setUnitName(create(TextNameType.class, (tnt) -> {
                            tnt.setValue(input.getMeteorologicalWatchOffice().getDesignator() + " MWO");
                            slice.setDesignator(create(CodeOrganisationDesignatorType.class, (desig) -> {
                                desig.setValue(input.getMeteorologicalWatchOffice().getDesignator());
                            }));
                        }));
                    }));
                }));
            }));
        }));

        if (airmet.getStatus().equals(AIRMETReportStatusType.CANCELLATION)) {
            airmet.setCancelledSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            airmet.setCancelledValidPeriod(getCancelledTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input)));
            airmet.setPhenomenon(create(AeronauticalAreaWeatherPhenomenonType.class, (phen) -> {
                phen.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
            }));

            airmet.setAnalysis(createCancelAnalysis(input, issueTime, airmetUuid));

        } else {
            AeronauticalAreaWeatherPhenomenonType phenType = create(AeronauticalAreaWeatherPhenomenonType.class, (ref) -> {
                ref.setHref(AviationCodeListUser.CODELIST_SIGWX_PHENOMENA_ROOT + input.getAirmetPhenomenon());
                ref.setTitle("Airmet PhenomenonType");

            });
            airmet.setPhenomenon(phenType);

            System.err.println("Creating analysis");
            airmet.setAnalysis(createAnalysis(input, input.getIssuingAirTrafficServicesUnit().getDesignator(),
                        input.getIssuingAirTrafficServicesUnit().getName(), issueTime, airmetUuid));
        }
        
        System.err.println("seq:" + input.getSequenceNumber() + " " + airmet.getStatus());
        airmet.setSequenceNumber(input.getSequenceNumber());

        airmet.setValidPeriod(getTimePeriodPropertyType(input, airmetUuid));

        System.err.println("AM: " + airmet.toString());
        try {
            result.setStatus(Status.SUCCESS);
            this.updateMessageMetadata(input, result, airmet);
            ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(result);
            this.validateDocument(airmet, AIRMETType.class, hints, eventHandler); //TODO true is for debugging: shows results even in case of failure

            if (eventHandler.errorsFound()) {
                result.setStatus(Status.FAIL);
                System.err.println("FAILED in eventhandler");
                for (ConversionIssue iss: eventHandler.getResult().getConversionIssues()) {
                    System.err.println("ISS: "+iss.getMessage());
                }
            } else {
                result.setConvertedMessage(this.render(airmet, hints));
                System.err.println("result set");
            }
        } catch (ConversionException e) {
            System.err.println("Error in conversion ");
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message to String", e));
        }

        return result;
    }


        protected static TimePeriodPropertyType getTimePeriodPropertyType(AIRMET input, String uuid) {
            return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
        }

        protected static TimePeriodPropertyType getCancelledTimePeriodPropertyType(AIRMET input, String uuid) {
            return getATimePeriodPropertyType(input.getCancelledReference().get().getValidityPeriod(), uuid);
        }

        protected static TimePeriodPropertyType getATimePeriodPropertyType(PartialOrCompleteTimePeriod valTime, String uuid) {
            return create(TimePeriodPropertyType.class, (prop) -> {
                TimePeriodType tp = create(TimePeriodType.class);
                tp.setId("validt-"+uuid);
                TimePositionType beginPos = create(TimePositionType.class);
                beginPos.getValue().add(valTime.getStartTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                tp.setBeginPosition(beginPos);
                TimePositionType endPos = create(TimePositionType.class);
                endPos.getValue().add(valTime.getEndTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                tp.setEndPosition(endPos);
                prop.setTimePeriod(tp);
            });
        }


        /**************************************************************************/


    @SuppressWarnings("unchecked")
    private OMObservationPropertyType createAnalysis(AIRMET input, String designator, String airspaceName, String issueTime,
            String airmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("analysis-" + airmetUUID);
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));

                        //TODO Only if FCST or OBS time are given
                        if ((input.getAnalysisType() == SigmetAnalysisType.UNKNOWN) || !input.getAnalysisGeometries().get().get(0).getTime().isPresent()) {
                            //set Phen time to nil with nilReason of "missing"
                            omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                                toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING);
                            }));
                        } else {
                            omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                                JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, (period) -> {
                                    period.setId("phent-" + airmetUUID);
                                    period.setTimePosition(create(TimePositionType.class, (tPos) -> {
                                        tPos.getValue().add(input.getAnalysisGeometries().get().get(0).getTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                                    }));
                                });
                                toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                            }));
                        }
                        omObs.setValidTime(create(TimePeriodPropertyType.class, (tppt) -> {
                            tppt.setHref("#validt-" + airmetUUID);
                        }));

                        omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                            tip.setTimeInstant(create(TimeInstantType.class, (ti) -> {
                                ti.setTimePosition(create(TimePositionType.class, (tp) -> {
                                    tp.getValue().add(issueTime);
                                }));
                                ti.setId("resltt-" + airmetUUID);
                            }));
                        }));

                        omObs.setProcedure(create(OMProcessPropertyType.class, (omppt) -> {
                            omppt.setAny(createAndWrap(ProcessType.class, (process) -> {
                                process.setId("proc-" + airmetUUID);
                                process.setDescription(create(StringOrRefType.class, (descr) -> {
                                    descr.setValue(AviationCodeListUser.CODELIST_VALUE_AIRMET_PROCESS);
                                }));
                            }));
                        }));

                        omObs.setObservedProperty(create(ReferenceType.class, (ref) -> {
                            ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                        }));

                        omObs.setFeatureOfInterest(getFeatureOfInterest(input, designator, airspaceName, "sampling-surface-" + airmetUUID));

                        AIRMETEvolvingConditionCollectionPropertyType _seccpt = getResult(input, airmetUUID);
                        omObs.setResult(_seccpt);
            }));

        });
        return analysis;
    }

    private FeaturePropertyType getFeatureOfInterest(AIRMET input, String designator, String airSpaceName, String sfSpatialUUID) {
        FeaturePropertyType ftp = create(FeaturePropertyType.class, (prop) -> {
            prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, (samsFeature) -> {
                samsFeature.setId(sfSpatialUUID);
                samsFeature.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_OM_SAMPLING + "SF_SamplingSurface");
                    ref.setTitle("Sampling surface");
                }));

                samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, (samProp) -> {
                    AirspaceType airspace = create(AirspaceType.class);
                    airspace.setValidTime(null);
                    airspace.setId("fir-" + designator + "-" + UUID.randomUUID());
                    airspace.getTimeSlice().add(create(AirspaceTimeSlicePropertyType.class, (timeSliceProp) -> {
                        timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, (timeSlice) -> {
                            timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                            timeSlice.setInterpretation("SNAPSHOT");
                            timeSlice.setType(create(CodeAirspaceType.class, (type) -> {
                                type.setValue("FIR");
                            }));
                            timeSlice.setAirspaceName(create(TextNameType.class, (name) -> {
                                name.setValue(airSpaceName);
                            }));
                            timeSlice.setId("fir-" + designator + "-" + UUID.randomUUID() + "-ts");
                            timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, (desig) -> {
                                desig.setValue(designator);
                            }));

                        }));
                    }));
                    samProp.setAbstractFeature(wrap(airspace, AirspaceType.class));
                }));
                samsFeature.setShape(create(ShapeType.class, (shp) -> {
                    shp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_WITHHELD);
                }));
            }));
        });
        return ftp;
        //return null;
    }

    private AIRMETEvolvingConditionCollectionPropertyType getResult(AIRMET input, String airmetUUID) {
        ObjectFactory of = new ObjectFactory();
        AIRMETEvolvingConditionCollectionPropertyType _seccpt = create(AIRMETEvolvingConditionCollectionPropertyType.class, (seccpt) -> {
            seccpt.setAIRMETEvolvingConditionCollection(create(AIRMETEvolvingConditionCollectionType.class, (secct) -> {
                secct.setId("fcst-" + airmetUUID);
                secct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                if (input.getAnalysisType() == SigmetAnalysisType.FORECAST) {
                    secct.setTimeIndicator(TimeIndicatorType.FORECAST);
                }
                int cnt=0;
                for (PhenomenonGeometryWithHeight geometryWithHeight: input.getAnalysisGeometries().get()) {
                    secct.getMember().add(create(AIRMETEvolvingConditionPropertyType.class, (secpt) -> {
                        secpt.setAIRMETEvolvingCondition(create(AIRMETEvolvingConditionType.class, (sect) -> {
                            sect.setId("sec-" + cnt + "-" + airmetUUID);

                            sect.setApproximateLocation(geometryWithHeight.getApproximateLocation().orElse(false));
                            input.getIntensityChange().ifPresent((intensityChange) -> {
                                switch (intensityChange) {
                                    case NO_CHANGE:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.NO_CHANGE);
                                        break;
                                    case WEAKENING:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.WEAKEN);
                                        break;
                                    case INTENSIFYING:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.INTENSIFY);
                                        break;
                                    default:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.NO_CHANGE);
                                        break;
                                }
                            });
                            if (input.getMovingDirection().isPresent()) {
                                icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                NumericMeasure md=input.getMovingDirection().get();
                                angl.setUom(md.getUom());
                                angl.setValue(md.getValue());

                                JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createAIRMETEvolvingConditionTypeDirectionOfMotion(angl);
                                sect.setDirectionOfMotion(directionOfMotion);

                                input.getMovingSpeed().ifPresent((ms) -> {
                                    sect.setSpeedOfMotion(create(SpeedType.class, (spd) -> {
                                        spd.setUom(ms.getUom());
                                        spd.setValue(ms.getValue());
                                    }));
                                });
/*
                            } else { //Add nil directionOfMotion if there is no forecast
                                    icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                    AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                    angl.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING);
                                    angl.setUom("N/A");
                                    JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createAIRMETEvolvingConditionTypeDirectionOfMotion(angl);
                                    directionOfMotion.setNil(true);
*/
                                    sect.setDirectionOfMotion(directionOfMotion);
                            }

                            sect.setGeometry(create(AirspaceVolumePropertyType.class, (avpt) -> {
                                avpt.setAirspaceVolume(create(AirspaceVolumeType.class, (avt) -> {
                                    avt.setId("as-" + cnt + "-" + airmetUUID);
                                    geometryWithHeight.getUpperLimit().ifPresent((l) -> {
                                        avt.setUpperLimit(create(ValDistanceVerticalType.class, (vdvt) -> {
                                            vdvt.setUom(l.getUom());
                                            vdvt.setValue(l.getValue().toString());
                                        }));
                                        avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, (cvrt) -> {
                                            if (l.getValue()==0.){
                                                cvrt.setValue("SFC");
                                            } else {
                                                cvrt.setValue("STD");
                                            }
                                        }));
                                    });
                                    geometryWithHeight.getLowerLimit().ifPresent((l) -> {
                                        avt.setLowerLimit(create(ValDistanceVerticalType.class, (vdvt) -> {
                                            vdvt.setUom(l.getUom());
                                            vdvt.setValue(l.getValue().toString());
                                        }));
                                        avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, (cvrt) -> {
                                             if (l.getValue()==0.){
                                                cvrt.setValue("SFC");
                                            } else {
                                                cvrt.setValue("STD");
                                            }
                                        }));
                                    });
                                    avt.setHorizontalProjection(create(SurfacePropertyType.class, (spt) -> {
                                        spt.setSurface(createAndWrap(SurfaceType.class, (sft) -> {
                                            try {
                                                if (geometryWithHeight.getGeometry().isPresent()) {
                                                    Geometry geom = geometryWithHeight.getGeometry().get().getGeoGeometry().get();
                                                    System.err.println("GEOM: " + geom.getGeometryType());
                                                    if ("Point".equals(geom.getGeometryType())) {
                                                        List<Double> pts = new ArrayList<Double>();
                                                        for (Coordinate coord : geom.getCoordinates()) {
                                                            pts.add(coord.y);
                                                            pts.add(coord.x);
                                                        }
                                                        JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, (poly) -> {
                                                            poly.setExterior(create(AbstractRingPropertyType.class, (arpt) -> {
                                                                arpt.setAbstractRing(createAndWrap(RingType.class, (rt) -> {
                                                                    rt.getCurveMember().add(create(CurvePropertyType.class, (curvept) -> {
                                                                        curvept.setAbstractCurve(createAndWrap(CurveType.class, (curvet) -> {
                                                                            curvet.setId("curve-analysis-" + cnt + "-" + airmetUUID);
                                                                            curvet.setSegments(create(CurveSegmentArrayPropertyType.class, (curvesat) -> {
                                                                                curvesat.getAbstractCurveSegment()
                                                                                        .add(createAndWrap(CircleByCenterPointType.class, (cbcpt) -> {
                                                                                            cbcpt.setPos(create(DirectPositionType.class, (dpt) -> {
                                                                                                dpt.getValue().addAll(Arrays.asList(pts.toArray(new Double[0])));
                                                                                            }));
                                                                                            cbcpt.setNumArc(BigInteger.valueOf(1));
                                                                                            cbcpt.setRadius(create(LengthType.class, (lt) -> {
                                                                                                lt.setValue(0.0);
                                                                                                lt.setUom("[nmi_i]");
                                                                                            }));
                                                                                        }));
                                                                            }));
                                                                        }));
                                                                    }));
                                                                }));
                                                            }));
                                                        });
                                                        SurfacePatchArrayPropertyType sp = of.createSurfacePatchArrayPropertyType();
                                                        JAXBElement<SurfacePatchArrayPropertyType> spapt = of.createPolygonPatches(sp);
                                                        spapt.getValue().getAbstractSurfacePatch().add(ppt);
/*                                                    JAXBElement<SurfacePatchArrayPropertyType> spapt = createAndWrap(SurfacePatchArrayPropertyType.class,
                                                            (_spapt)-> {
                                                                _spapt.getAbstractSurfacePatch().add(ppt);
                                                            });*/
                                                        sft.setPatches(spapt);
                                                    } else {
                                                        List<Double> pts = new ArrayList<Double>();
                                                        for (Coordinate coord : geom.getCoordinates()) {
                                                            pts.add(coord.y);
                                                            pts.add(coord.x);
                                                        }
                                                        ;
                                                        JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, (poly) -> {
                                                            poly.setExterior(create(AbstractRingPropertyType.class, (arpt) -> {
                                                                arpt.setAbstractRing(createAndWrap(LinearRingType.class, (lrt) -> {
                                                                    DirectPositionListType dplt = create(DirectPositionListType.class, (dpl) -> {
                                                                        dpl.getValue().addAll(pts);
                                                                    });
                                                                    lrt.setPosList(dplt);
                                                                }));
                                                            }));
                                                        });

                                                        SurfacePatchArrayPropertyType sp = of.createSurfacePatchArrayPropertyType();
                                                        JAXBElement<SurfacePatchArrayPropertyType> spapt = of.createPolygonPatches(sp);
                                                        spapt.getValue().getAbstractSurfacePatch().add(ppt);

                                                        sft.setPatches(spapt);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                sft.setPatches(null);
                                                e.printStackTrace();
                                            }
                                            sft.setId("an-sfc-" + cnt + "-" + airmetUUID);
                                            sft.setSrsDimension(new BigInteger("2"));
                                            sft.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                        }));
                                    }));
                                }));

                            }));
                            if (geometryWithHeight.getLowerLimitOperator().isPresent()){
                                sect.setGeometryLowerLimitOperator(RelationalOperatorType.fromValue(geometryWithHeight.getLowerLimitOperator().get().name()));
                            }
                            if (geometryWithHeight.getUpperLimitOperator().isPresent()){
                                sect.setGeometryUpperLimitOperator(RelationalOperatorType.fromValue(geometryWithHeight.getUpperLimitOperator().get().name()));
                            }
                        }));
                    }));
                }
            }));
        });

        return _seccpt;
    }





    private OMObservationPropertyType createCancelAnalysis(AIRMET input, String issueTime, String airmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("cnl-analysis");
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                    toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                }));
                omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                    tip.setHref("#" + "resltt-"+airmetUUID);
                }));

                omObs.setProcedure(create(OMProcessPropertyType.class, (omppt) -> {
                    omppt.setAny(createAndWrap(ProcessType.class, (process) -> {
                        process.setId("p-49-2-airmet-" + airmetUUID);
                        process.setDescription(create(StringOrRefType.class, (descr) -> {
                            descr.setValue(AviationCodeListUser.CODELIST_VALUE_AIRMET_PROCESS);
                        }));
                    }));
                }));

                omObs.setObservedProperty(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));

                FeaturePropertyType ftp = create(FeaturePropertyType.class, (prop) -> {
                    prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, (samsFeature) -> {
                        samsFeature.setId("sampling-surface-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID());
                        samsFeature.setType(create(ReferenceType.class, (ref) -> {
                            ref.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_OM_SAMPLING + "SF_SamplingSurface");
                            ref.setTitle("Sampling surface");
                        }));

                        samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, (samProp) -> {
                            AirspaceType airspace = create(AirspaceType.class);
                            airspace.setValidTime(null);
                            airspace.setId("fir-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID());
                            airspace.getTimeSlice().add(create(AirspaceTimeSlicePropertyType.class, (timeSliceProp) -> {
                                timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, (timeSlice) -> {
                                    timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                    timeSlice.setInterpretation("SNAPSHOT");
                                    timeSlice.setType(create(CodeAirspaceType.class, (type) -> {
                                        type.setValue("FIR");
                                    }));
                                    timeSlice.setAirspaceName(create(TextNameType.class, (name) -> {
                                        name.setValue(input.getIssuingAirTrafficServicesUnit().getName());
                                    }));
                                    timeSlice.setId("fir-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID() + "-ts");
                                    timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, (desig) -> {
                                        desig.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator());
                                    }));

                                }));
                            }));
                            samProp.setAbstractFeature(wrap(airspace, AirspaceType.class));
                        }));
                        samsFeature.setShape(create(ShapeType.class, (shp) -> {
                            shp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_WITHHELD);
                        }));
                    }));
                });
                omObs.setFeatureOfInterest(ftp);
                AIRMETEvolvingConditionCollectionPropertyType _seccpt = create(AIRMETEvolvingConditionCollectionPropertyType.class, (eccpt) -> {
                    eccpt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                });
                omObs.setResult(_seccpt);
            }));

        });
        return analysis;
        //return null;
    }

    protected void updateMessageMetadata(final AIRMET source, final ConversionResult<?> results, final AIRMETType target) throws ConversionException {
        try {
            DatatypeFactory f = DatatypeFactory.newInstance();

            //Default permissions
            target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
            source.getPermissibleUsage().ifPresent((us) -> {
                target.setPermissibleUsage(PermissibleUsageType.valueOf(us.name()));
                if (source.getPermissibleUsageReason().isPresent()) {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.valueOf(source.getPermissibleUsageReason().get().name()));
                }
                if (target.getPermissibleUsage().equals(PermissibleUsageType.NON_OPERATIONAL)) {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.EXERCISE);
                }
                if ((source.getPermissibleUsageSupplementary() != null)&&(source.getPermissibleUsageSupplementary().isPresent())) {
                    target.setPermissibleUsageSupplementary(source.getPermissibleUsageSupplementary().get());
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
                if (results.getStatus() != Status.SUCCESS && source.getTranslatedTAC().isPresent()) {
                    target.setTranslationFailedTAC(source.getTranslatedTAC().get());
                }
            }
        } catch (DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }

    }

    private static String getTimePeriodId(AIRMET input) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "airmet-" + valTime.getStartTime().get().getCompleteTime().get().format(dtf) + "-" + valTime.getEndTime()
                .get()
                .getCompleteTime()
                .get()
                .format(dtf);
    }


    @Override
    protected Source getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(this.getClass().getResourceAsStream("AIRMETCleanup.xsl"));
            return new DOMSource(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException("Unexpected problem in reading the cleanup XSL sheet", e);
        }

    }

}
