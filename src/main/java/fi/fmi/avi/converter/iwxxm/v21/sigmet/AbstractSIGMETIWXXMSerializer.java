package fi.fmi.avi.converter.iwxxm.v21.sigmet;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

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
import net.opengis.gml32.PointPropertyType;
import net.opengis.gml32.PointType;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v21.AbstractIWXXM21Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.VolcanoDescription;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.PolygonsGeometryImpl;
import fi.fmi.avi.model.PhenomenonGeometry;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import icao.iwxxm21.AeronauticalSignificantWeatherPhenomenonType;
import icao.iwxxm21.AirspacePropertyType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.ExpectedIntensityChangeType;
import icao.iwxxm21.PermissibleUsageReasonType;
import icao.iwxxm21.PermissibleUsageType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.SIGMETEvolvingConditionCollectionPropertyType;
import icao.iwxxm21.SIGMETEvolvingConditionCollectionType;
import icao.iwxxm21.SIGMETEvolvingConditionPropertyType;
import icao.iwxxm21.SIGMETEvolvingConditionType;
import icao.iwxxm21.SIGMETPositionCollectionPropertyType;
import icao.iwxxm21.SIGMETPositionCollectionType;
import icao.iwxxm21.SIGMETPositionPropertyType;
import icao.iwxxm21.SIGMETPositionType;
import icao.iwxxm21.SIGMETReportStatusType;
import icao.iwxxm21.SIGMETType;
import icao.iwxxm21.TimeIndicatorType;
import icao.iwxxm21.TropicalCycloneSIGMETType;
import icao.iwxxm21.UnitPropertyType;
import icao.iwxxm21.VolcanicAshSIGMETType;
import wmo.metce2013.ProcessType;
import wmo.metce2013.VolcanoPropertyType;
import wmo.metce2013.VolcanoType;

public abstract class AbstractSIGMETIWXXMSerializer<T> extends AbstractIWXXM21Serializer<SIGMET, T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSIGMETIWXXMSerializer.class);

    @SuppressWarnings("unchecked")
    private static OMObservationPropertyType createForecastPositionAnalysis(SIGMET inputs, String designator, String issueTime, String sigmetUUID) {
        OMObservationPropertyType forecastPositionAnalysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("forecastPositionAnalysis-" + sigmetUUID);
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_POSITION_COLLECTION_ANALYSIS);
                }));
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                    JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, (period) -> {
                        period.setId("time-" + UUID.randomUUID().toString());
                        period.setTimePosition(create(TimePositionType.class, (tPos) -> {
                            Object o = tPos.getValue();
                            tPos.getValue()
                                    .add(inputs.getForecastGeometries()
                                            .get()
                                            .get(0)
                                            .getTime()
                                            .get()
                                            .getCompleteTime()
                                            .get()
                                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }));
                    });
                    toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                }));

                omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                    tip.setHref("#resltt-" + sigmetUUID);
                }));

                omObs.setValidTime(create(TimePeriodPropertyType.class, (tip) -> {
                    tip.setHref("#validt-" + sigmetUUID);
                }));

                omObs.setProcedure(create(OMProcessPropertyType.class, (procProp) -> {
                    procProp.setHref("#proc-" + sigmetUUID);
                    //
                }));
                omObs.setFeatureOfInterest(create(FeaturePropertyType.class, (fppt) -> {
                    fppt.setHref("#sampling-surface-" + sigmetUUID);
                }));

                omObs.setObservedProperty(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_POSITION_COLLECTION_ANALYSIS);
                }));

                int cnt = 0;
                for (PhenomenonGeometry geometry : inputs.getForecastGeometries().get()) {
                    LOG.debug("About to setResult for FPA");
                    omObs.setResult(createFPAResult(geometry, designator, cnt, sigmetUUID));
                    cnt++;
                }
            }));
        });
        return forecastPositionAnalysis;
    }

    private static SIGMETPositionCollectionPropertyType createFPAResult(PhenomenonGeometry geometry, String designator, int cnt, String sigmetUUID) {
        ObjectFactory of = new ObjectFactory();
        SIGMETPositionCollectionPropertyType _spcpt = create(SIGMETPositionCollectionPropertyType.class, (spcpt) -> {
            spcpt.setSIGMETPositionCollection(create(SIGMETPositionCollectionType.class, (spct) -> {
                spct.setId("spc-" + designator + "-" + cnt + "-" + sigmetUUID);
                spct.getMember().add(create(SIGMETPositionPropertyType.class, (sppt) -> {
                    sppt.setSIGMETPosition(create(SIGMETPositionType.class, (spot) -> {
                        spot.setId("fpa-pos-" + cnt + "-" + sigmetUUID);
                        spot.setApproximateLocation(geometry.getApproximateLocation().orElse(false));
                        spot.setGeometry(create(AirspaceVolumePropertyType.class, (avpt) -> {
                            avpt.setAirspaceVolume(create(AirspaceVolumeType.class, (avt) -> {
                                avt.setId("fpa-" + cnt + "-" + sigmetUUID);
                                avt.setHorizontalProjection(create(SurfacePropertyType.class, (spt) -> {
                                    spt.setSurface(createAndWrap(SurfaceType.class, (sft) -> {
                                        try {
                                            Geometry geom = geometry.getGeometry().get().getGeoGeometry().get();
                                            if (PointGeometryImpl.class.isAssignableFrom(geom.getClass())) {
                                                List<Double> pts = new ArrayList<>();
                                                for (Double coordElement : ((PointGeometry) geom).getPoint()) {
                                                    pts.add(coordElement);
                                                }

                                                JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, (poly) -> {
                                                    poly.setExterior(create(AbstractRingPropertyType.class, (arpt) -> {
                                                        arpt.setAbstractRing(createAndWrap(RingType.class, (rt) -> {
                                                            rt.getCurveMember().add(create(CurvePropertyType.class, (curvept) -> {
                                                                curvept.setAbstractCurve(createAndWrap(CurveType.class, (curvet) -> {
                                                                    curvet.setId("curve-forecast-" + cnt + "-" + sigmetUUID);
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
/*
                                                JAXBElement<SurfacePatchArrayPropertyType> spapt = createAndWrap(SurfacePatchArrayPropertyType.class,
                                                        (_spapt)-> {
                                                            _spapt.getAbstractSurfacePatch().add(ppt);
                                                        });

/*                                                SurfacePatchArrayPropertyType sp = of.createSurfacePatchArrayPropertyType();
                                                JAXBElement<SurfacePatchArrayPropertyType> spapt = of.createPolygonPatches(sp);
                                                spapt.getValue().getAbstractSurfacePatch().add(ppt);
                                                */
                                                sft.setPatches(spapt);
                                            } else {
                                                List<Double> pts = new ArrayList<Double>();
                                                for (Double coord : ((PolygonsGeometryImpl) geom).getPolygons().get(0)) {
                                                    pts.add(coord);
                                                }

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
                                        } catch (Exception e) {
                                            sft.setPatches(null);
                                            e.printStackTrace();
                                        }
                                        sft.setId("fpa-sfc-" + cnt + "-" + sigmetUUID);
                                        sft.setSrsDimension(new BigInteger("2"));
                                        sft.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                    }));
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        });
        return _spcpt;
    }

    private static String getTimePeriodId(SIGMET input) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "sigmet-" + valTime.getStartTime().get().getCompleteTime().get().format(dtf) + "-" + valTime.getEndTime()
                .get()
                .getCompleteTime()
                .get()
                .format(dtf);
    }

    protected static TimePeriodPropertyType getTimePeriodPropertyType(SIGMET input, String uuid) {
        return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
    }

    protected static TimePeriodPropertyType getCancelledTimePeriodPropertyType(SIGMET input, String uuid) {
        return getATimePeriodPropertyType(input.getCancelledReference().get().getValidityPeriod(), uuid);
    }

    protected static TimePeriodPropertyType getATimePeriodPropertyType(PartialOrCompleteTimePeriod valTime, String uuid) {
        return create(TimePeriodPropertyType.class, (prop) -> {
            TimePeriodType tp = create(TimePeriodType.class);
            tp.setId("validt-" + uuid);
            TimePositionType beginPos = create(TimePositionType.class);
            beginPos.getValue().add(valTime.getStartTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            tp.setBeginPosition(beginPos);
            TimePositionType endPos = create(TimePositionType.class);
            endPos.getValue().add(valTime.getEndTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            tp.setEndPosition(endPos);
            prop.setTimePeriod(tp);
        });
    }

    public static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(final TimeObjectPropertyType timeObjectPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject", AbstractTimeObjectType.class, refCtx);
        if (to.isPresent()) {
            if (TimePeriodType.class.isAssignableFrom(to.get().getClass())) {
                final TimePeriodType tp = (TimePeriodType) to.get();
                final PartialOrCompleteTimePeriod.Builder retval = PartialOrCompleteTimePeriod.builder();
                getStartTime(tp, refCtx).ifPresent((start) -> {
                    retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                            .setCompleteTime(start).build());
                });

                getEndTime(tp, refCtx).ifPresent((end) -> {
                    retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                            .setCompleteTime(end).build());
                });
                return Optional.of(retval.build());
            }
        }
        return Optional.empty();
    }

    public static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(final TimeObjectPropertyType timeObjectPropertyType,
            final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject", AbstractTimeObjectType.class, refCtx);
        if (to.isPresent()) {
            if (TimeInstantType.class.isAssignableFrom(to.get().getClass())) {
                final TimeInstantType ti = (TimeInstantType) to.get();
                final Optional<ZonedDateTime> time = getTime(ti.getTimePosition());
                if (time.isPresent()) {
                    return Optional.of(PartialOrCompleteTimeInstant.builder().setCompleteTime(time).build());
                }
            } else {
                throw new IllegalArgumentException("Time object is not a time instant");
            }
        }
        return Optional.empty();
    }

    protected abstract T render(final SIGMETType sigmet, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

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
    public ConversionResult<T> convertMessage(final SIGMET input, final ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();

        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String sigmetUuid = UUID.randomUUID().toString();
        final String validTimeId = "validt-" + sigmetUuid;
        final String phenomenonTimeId = "phent-" + sigmetUuid;
        final String resultTimeId = "resltt-" + sigmetUuid;
        final String procedureId = "proc-" + sigmetUuid;
        final String sfSpatialId = "sampling-surface-" + sigmetUuid;
        final String foiId = "foi-" + sigmetUuid;

        final SIGMETType sigmet;
        switch (input.getSigmetPhenomenon()) {
            case TC:
                sigmet = create(TropicalCycloneSIGMETType.class);
                sigmet.setId("wc-" + UUID.randomUUID().toString());
                break;
            case VA:
                sigmet = create(VolcanicAshSIGMETType.class);
                sigmet.setId("wv-" + UUID.randomUUID().toString());
                break;
            default:
                sigmet = create(SIGMETType.class);
                sigmet.setId("ws-" + UUID.randomUUID().toString());
        }

        //Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().get().getCompleteTime().orElse(ZonedDateTime.now()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        if (input.getCancelledReference().isPresent()) {
            sigmet.setStatus(SIGMETReportStatusType.CANCELLATION);
        } else {
            sigmet.setStatus(SIGMETReportStatusType.NORMAL);
        }
        sigmet.setIssuingAirTrafficServicesUnit(create(UnitPropertyType.class, (prop) -> {
            prop.setUnit(create(UnitType.class, (unit) -> {
                unit.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + sigmetUuid);
                unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> {
                    sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, (slice) -> {
                        slice.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-ts-" + sigmetUuid);
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

        sigmet.setOriginatingMeteorologicalWatchOffice(create(UnitPropertyType.class, (prop) -> {
            prop.setUnit(create(UnitType.class, (unit) -> {
                unit.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-" + sigmetUuid);
                unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, (sliceProp) -> {
                    sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, (slice) -> {
                        slice.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-ts-" + sigmetUuid);
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

        sigmet.setSequenceNumber(input.getSequenceNumber());

        sigmet.setValidPeriod(getTimePeriodPropertyType(input, sigmetUuid));

        if (sigmet.getStatus().equals(SIGMETReportStatusType.CANCELLATION)) {
            sigmet.setCancelledSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            sigmet.setCancelledValidPeriod(getCancelledTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input)));
            sigmet.setPhenomenon(create(AeronauticalSignificantWeatherPhenomenonType.class, (phen) -> {
                phen.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
            }));

            sigmet.setAnalysis(createCancelAnalysis(input, issueTime, sigmetUuid));
            if ((input.getSigmetPhenomenon()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
                if (input.getVAInfo().get().getVolcanicAshMovedToFIR().isPresent()) {
                    String designator = input.getVAInfo().get().getVolcanicAshMovedToFIR().get().getDesignator();
                    String airSpaceName = input.getVAInfo().get().getVolcanicAshMovedToFIR().get().getName();
                    sigmet.setVolcanicAshMovedToFIR(create(AirspacePropertyType.class, (apt) -> {
                        AirspaceType airspace = create(AirspaceType.class);
                        airspace.setValidTime(null);
                        airspace.setId("movedto-fir-" + designator + "-" + UUID.randomUUID());
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
                        apt.setAirspace(airspace);
                    }));
                }
            }
        } else {
            AeronauticalSignificantWeatherPhenomenonType phenType = create(AeronauticalSignificantWeatherPhenomenonType.class, (ref) -> {
                ref.setHref(AviationCodeListUser.CODELIST_SIGWX_PHENOMENA_ROOT + input.getSigmetPhenomenon());
                ref.setTitle("Sigmet PhenomenonType");

            });
            sigmet.setPhenomenon(phenType);

            sigmet.setAnalysis(
                    createAnalysis(input, input.getIssuingAirTrafficServicesUnit().getDesignator(), input.getIssuingAirTrafficServicesUnit().getName(),
                            issueTime, sigmetUuid));
            if ((input.getForecastGeometries().isPresent()) && (input.getForecastGeometries().get().size() > 0)) {
                sigmet.setForecastPositionAnalysis(
                        createForecastPositionAnalysis(input, input.getIssuingAirTrafficServicesUnit().getDesignator(), issueTime, sigmetUuid));
            }

        }
        if ((input.getSigmetPhenomenon()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
            VolcanoDescription volcano = input.getVAInfo().get().getVolcano();
            icao.iwxxm21.ObjectFactory of = new icao.iwxxm21.ObjectFactory();
            ((VolcanicAshSIGMETType) sigmet).getRest().add(of.createVolcanicAshSIGMETTypeEruptingVolcano(create(VolcanoPropertyType.class, (vpt) -> {

                vpt.setVolcano(createAndWrap(VolcanoType.class, (v) -> {
                    if (volcano.getVolcanoPosition().isPresent()) {
                        Double[] pts = volcano.getVolcanoPosition().get().getCoordinates().toArray(new Double[0]);
                        v.setPosition(create(PointPropertyType.class, (ppt) -> {
                            ppt.setPoint(create(PointType.class, (pt) -> {
                                pt.setPos(create(DirectPositionType.class, (dpt) -> {
                                    dpt.getValue().addAll(Arrays.asList(pts));
                                    dpt.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                    dpt.setSrsDimension(BigInteger.valueOf(2));
                                    dpt.getAxisLabels().add("Lat");
                                    dpt.getAxisLabels().add("Lon");
                                    dpt.getUomLabels().add("deg");
                                    dpt.getUomLabels().add("deg");
                                }));
                                pt.setId("wv-pt-" + sigmetUuid);
                            }));
                        }));
                    }
                    if (volcano.getVolcanoName().isPresent()) {
                        v.setVolcanoName(volcano.getVolcanoName().get());
                        v.setId("wv-" + volcano.getVolcanoName().get().replace(" ", "_") + "-" + sigmetUuid);
                    } else {
                        String generatedVolcanoName = "Unknown";
                        v.setVolcanoName(generatedVolcanoName);
                        v.setId("wv-" + generatedVolcanoName + "-" + sigmetUuid);
                    }
                }));
                //               } else {
                //                   vpt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
        })));
    }

    try {
        this.updateMessageMetadata(input, result, sigmet);

        //TODO: move into a an IWXXM 2.0 common abstract class when available
        final XMLSchemaInfo schemaInfo = new XMLSchemaInfo();
        schemaInfo.addSchemaSource(SIGMETType.class.getResourceAsStream("/int/icao/iwxxm/2.1.1/iwxxm.xsd"));
        schemaInfo.addSchemaLocation("http://icao.int/iwxxm/2.1", "https://schemas.wmo.int/iwxxm/2.1.1/iwxxm.xsd");
        schemaInfo.addSchemaLocation("http://def.wmo.int/metce/2013", "http://schemas.wmo.int/metce/1.2/metce.xsd");
        schemaInfo.addSchemaLocation("http://www.opengis.net/samplingSpatial/2.0",
                "http://schemas.opengis.net/samplingSpatial/2.0/spatialSamplingFeature.xsd");
        schemaInfo.setSchematronRules(SIGMETType.class.getResource("/schematron/xslt/int/icao/iwxxm/2.1.1/rule/iwxxm.xsl"));
        if (input.getSigmetPhenomenon().equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
            result.addIssue(validateDocument(((VolcanicAshSIGMETType) sigmet), VolcanicAshSIGMETType.class, schemaInfo, hints));
        } else {
            result.addIssue(validateDocument(sigmet, SIGMETType.class, schemaInfo, hints));
        }
        result.setConvertedMessage(this.render(sigmet, schemaInfo, hints));
    } catch (ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render SIGMET IWXXM message to String", e));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private OMObservationPropertyType createAnalysis(SIGMET input, String designator, String airspaceName, String issueTime, String sigmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("analysis-" + sigmetUUID);
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
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
                            period.setId("phent-" + sigmetUUID);
                            period.setTimePosition(create(TimePositionType.class, (tPos) -> {
                                tPos.getValue()
                                        .add(input.getAnalysisGeometries()
                                                .get()
                                                .get(0)
                                                .getTime()
                                                .get()
                                                .getCompleteTime()
                                                .get()
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                            }));
                        });
                        toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                    }));
                }
                omObs.setValidTime(create(TimePeriodPropertyType.class, (tppt) -> {
                    tppt.setHref("#validt-" + sigmetUUID);
                }));

                omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                    tip.setTimeInstant(create(TimeInstantType.class, (ti) -> {
                        ti.setTimePosition(create(TimePositionType.class, (tp) -> {
                            tp.getValue().add(issueTime);
                        }));
                        ti.setId("resltt-" + sigmetUUID);
                    }));
                }));

                omObs.setProcedure(create(OMProcessPropertyType.class, (omppt) -> {
                    omppt.setAny(createAndWrap(ProcessType.class, (process) -> {
                        process.setId("proc-" + sigmetUUID);
                        process.setDescription(create(StringOrRefType.class, (descr) -> {
                            descr.setValue(AviationCodeListUser.CODELIST_VALUE_SIGMET_PROCESS);
                        }));
                    }));
                }));

                omObs.setObservedProperty(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));

                omObs.setFeatureOfInterest(getFeatureOfInterest(input, designator, airspaceName, "sampling-surface-" + sigmetUUID));

                SIGMETEvolvingConditionCollectionPropertyType _seccpt = getResult(input, sigmetUUID);
                omObs.setResult(_seccpt);
            }));

        });
        return analysis;
    }

    private FeaturePropertyType getFeatureOfInterest(SIGMET input, String designator, String airSpaceName, String sfSpatialUUID) {
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

    private SIGMETEvolvingConditionCollectionPropertyType getResult(SIGMET input, String sigmetUUID) {
        ObjectFactory of = new ObjectFactory();
        SIGMETEvolvingConditionCollectionPropertyType _seccpt = create(SIGMETEvolvingConditionCollectionPropertyType.class, (seccpt) -> {
            seccpt.setSIGMETEvolvingConditionCollection(create(SIGMETEvolvingConditionCollectionType.class, (secct) -> {
                secct.setId("fcst-" + sigmetUUID);
                secct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                if (input.getAnalysisType() == SigmetAnalysisType.FORECAST) {
                    secct.setTimeIndicator(TimeIndicatorType.FORECAST);
                }
                int cnt = 0;
                for (PhenomenonGeometryWithHeight geometryWithHeight : input.getAnalysisGeometries().get()) {
                    secct.getMember().add(create(SIGMETEvolvingConditionPropertyType.class, (secpt) -> {
                        secpt.setSIGMETEvolvingCondition(create(SIGMETEvolvingConditionType.class, (sect) -> {
                            sect.setId("sec-" + cnt + "-" + sigmetUUID);

                            sect.setApproximateLocation(geometryWithHeight.getApproximateLocation().orElse(false));
                            input.getIntensityChange().ifPresent((intensityChange) -> {
                                switch (intensityChange) {
                                    case NO_CHANGE:
                                        sect.setIntensityChange(ExpectedIntensityChangeType.NO_CHANGE);
                                        break;
                                    case WEAKENING:
                                        sect.setIntensityChange(ExpectedIntensityChangeType.WEAKEN);
                                        break;
                                    case INTENSIFYING:
                                        sect.setIntensityChange(ExpectedIntensityChangeType.INTENSIFY);
                                        break;
                                    default:
                                        sect.setIntensityChange(ExpectedIntensityChangeType.NO_CHANGE);
                                        break;
                                }
                            });
                            if (input.getMovingDirection().isPresent()) {
                                icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                NumericMeasure md = input.getMovingDirection().get();
                                angl.setUom(md.getUom());
                                angl.setValue(md.getValue());

                                JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createSIGMETEvolvingConditionTypeDirectionOfMotion(angl);
                                sect.setDirectionOfMotion(directionOfMotion);

                                input.getMovingSpeed().ifPresent((ms) -> {
                                    sect.setSpeedOfMotion(create(SpeedType.class, (spd) -> {
                                        spd.setUom(ms.getUom());
                                        spd.setValue(ms.getValue());
                                    }));
                                });

                            } else { //Add zero speedOfMotion if STNR and nothing if there is an end position
                                if (!(input.getForecastGeometries().isPresent()&&(input.getForecastGeometries().get().size()!=0))) {
                                    sect.setSpeedOfMotion(create(SpeedType.class, (spd) -> {
                                        spd.setUom("[kn_i]");
                                        spd.setValue(0);
                                    }));
                                }

                            }

                            sect.setGeometry(create(AirspaceVolumePropertyType.class, (avpt) -> {
                                avpt.setAirspaceVolume(create(AirspaceVolumeType.class, (avt) -> {
                                    avt.setId("as-" + cnt + "-" + sigmetUUID);
                                    geometryWithHeight.getUpperLimit().ifPresent((l) -> {
                                        avt.setUpperLimit(create(ValDistanceVerticalType.class, (vdvt) -> {
                                            vdvt.setUom(l.getUom());
                                            vdvt.setValue(l.getValue().toString());
                                        }));
                                        avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, (cvrt) -> {
                                            if (l.getValue() == 0.) {
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
                                            if (l.getValue() == 0.) {
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
                                                    if (PointGeometryImpl.class.isAssignableFrom(geom.getClass())){
                                                        List<Double> pts = new ArrayList<Double>();
                                                        for (Double coordElement : ((PointGeometryImpl)geom).getPoint()) {
                                                            pts.add(coordElement);
                                                        }
                                                        JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, (poly) -> {
                                                            poly.setExterior(create(AbstractRingPropertyType.class, (arpt) -> {
                                                                arpt.setAbstractRing(createAndWrap(RingType.class, (rt) -> {
                                                                    rt.getCurveMember().add(create(CurvePropertyType.class, (curvept) -> {
                                                                        curvept.setAbstractCurve(createAndWrap(CurveType.class, (curvet) -> {
                                                                            curvet.setId("curve-analysis-" + cnt + "-" + sigmetUUID);
                                                                            curvet.setSegments(create(CurveSegmentArrayPropertyType.class, (curvesat) -> {
                                                                                curvesat.getAbstractCurveSegment()
                                                                                        .add(createAndWrap(CircleByCenterPointType.class, (cbcpt) -> {
                                                                                            cbcpt.setPos(create(DirectPositionType.class, (dpt) -> {
                                                                                                dpt.getValue()
                                                                                                        .addAll(Arrays.asList(pts.toArray(new Double[0])));
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
                                                        for (Double coord: ((PolygonsGeometryImpl)geom).getPolygons().get(0)) {
                                                            pts.add(coord);
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
                                            sft.setId("an-sfc-" + cnt + "-" + sigmetUUID);
                                            sft.setSrsDimension(new BigInteger("2"));
                                            sft.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                        }));
                                    }));
                                }));

                            }));
                            if (geometryWithHeight.getLowerLimitOperator().isPresent()) {
                                sect.setGeometryLowerLimitOperator(RelationalOperatorType.fromValue(geometryWithHeight.getLowerLimitOperator().get().name()));
                            }
                            if (geometryWithHeight.getUpperLimitOperator().isPresent()) {
                                sect.setGeometryUpperLimitOperator(RelationalOperatorType.fromValue(geometryWithHeight.getUpperLimitOperator().get().name()));
                            }
                        }));
                    }));
                }
            }));
        });

        return _seccpt;
    }

    private OMObservationPropertyType createCancelAnalysis(SIGMET input, String issueTime, String sigmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("cnl-analysis");
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                    toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                }));
                omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                    tip.setHref("#" + "resltt-" + sigmetUUID);
                }));

                omObs.setProcedure(create(OMProcessPropertyType.class, (omppt) -> {
                    omppt.setAny(createAndWrap(ProcessType.class, (process) -> {
                        process.setId("p-49-2-sigmet-" + sigmetUUID);
                        process.setDescription(create(StringOrRefType.class, (descr) -> {
                            descr.setValue(AviationCodeListUser.CODELIST_VALUE_SIGMET_PROCESS);
                        }));
                    }));
                }));

                omObs.setObservedProperty(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
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
                SIGMETEvolvingConditionCollectionPropertyType _seccpt = create(SIGMETEvolvingConditionCollectionPropertyType.class, (eccpt) -> {
                    eccpt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                });
                omObs.setResult(_seccpt);
            }));

        });
        return analysis;
        //return null;
    }

    protected void updateMessageMetadata(final SIGMET source, final ConversionResult<?> results, final SIGMETType target) throws ConversionException {
        try {
            DatatypeFactory f = DatatypeFactory.newInstance();

            //Default permissions
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
                if (results.getStatus() != Status.SUCCESS && source.getTranslatedTAC().isPresent()) {
                    target.setTranslationFailedTAC(source.getTranslatedTAC().get());
                }
            }
        } catch (DatatypeConfigurationException e) {
            throw new ConversionException("Exception in setting the translation time", e);
        }

    }
/*
    private static String getTimePeriodId(SIGMET input) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "sigmet-" + valTime.getStartTime().get().getCompleteTime().get().format(dtf) + "-" + valTime.getEndTime()
                .get()
                .getCompleteTime()
                .get()
                .format(dtf);
    }
    */

/*
    protected static TimePeriodPropertyType getTimePeriodPropertyType(SIGMET input, String uuid) {
        return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
    }

    protected static TimePeriodPropertyType getCancelledTimePeriodPropertyType(SIGMET input, String uuid) {
        return getATimePeriodPropertyType(input.getCancelledReference().get().getValidityPeriod(), uuid);
    }

 */
/*
    protected static TimePeriodPropertyType getATimePeriodPropertyType(PartialOrCompleteTimePeriod valTime, String uuid) {
        return create(TimePeriodPropertyType.class, (prop) -> {
            TimePeriodType tp = create(TimePeriodType.class);
            tp.setId("validt-" + uuid);
            TimePositionType beginPos = create(TimePositionType.class);
            beginPos.getValue().add(valTime.getStartTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            tp.setBeginPosition(beginPos);
            TimePositionType endPos = create(TimePositionType.class);
            endPos.getValue().add(valTime.getEndTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            tp.setEndPosition(endPos);
            prop.setTimePeriod(tp);
        });
    }
 */

    @Override
    protected InputStream getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        InputStream retval = this.getClass().getResourceAsStream("SIGMETCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }
}
