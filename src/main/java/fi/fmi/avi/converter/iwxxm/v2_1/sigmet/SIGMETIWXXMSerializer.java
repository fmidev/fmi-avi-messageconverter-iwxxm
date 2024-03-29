package fi.fmi.avi.converter.iwxxm.v2_1.sigmet;

import aero.aixm511.*;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Serializer;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.VAInfo;
import icao.iwxxm21.AirspacePropertyType;
import icao.iwxxm21.UnitPropertyType;
import icao.iwxxm21.*;
import net.opengis.gml32.PointPropertyType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.*;
import net.opengis.om20.OMObservationPropertyType;
import net.opengis.om20.OMObservationType;
import net.opengis.om20.OMProcessPropertyType;
import net.opengis.om20.TimeObjectPropertyType;
import net.opengis.sampling.spatial.SFSpatialSamplingFeatureType;
import net.opengis.sampling.spatial.ShapeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import wmo.metce2013.ProcessType;
import wmo.metce2013.VolcanoPropertyType;
import wmo.metce2013.VolcanoType;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public abstract class SIGMETIWXXMSerializer<T> extends AbstractIWXXM21Serializer<SIGMET, T> {
    private static final Logger LOG = LoggerFactory.getLogger(SIGMETIWXXMSerializer.class);

    @SuppressWarnings("unchecked")
    private static OMObservationPropertyType createForecastPositionAnalysis(final SIGMET inputs, final String designator, final String issueTime,
                                                                            final String sigmetUUID) {
        return create(OMObservationPropertyType.class, omObsType -> omObsType.setOMObservation(create(OMObservationType.class, omObs -> {
            omObs.setId("forecastPositionAnalysis-" + sigmetUUID);
            omObs.setType(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_POSITION_COLLECTION_ANALYSIS)));
            omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, toProp -> {
                final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                    period.setId("time-" + UUID.randomUUID().toString());
                    period.setTimePosition(create(TimePositionType.class, tPos -> inputs.getForecastGeometries()//
                            .map(AbstractIWXXM21Serializer::getFirstOrNull)//
                            .flatMap(PhenomenonGeometry::getTime)//
                            .<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                            .ifPresent(time -> tPos.getValue().add(time))));
                });
                toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
            }));

            omObs.setResultTime(create(TimeInstantPropertyType.class, tip -> tip.setHref("#resltt-" + sigmetUUID)));

            omObs.setValidTime(create(TimePeriodPropertyType.class, tip -> tip.setHref("#validt-" + sigmetUUID)));

            omObs.setProcedure(create(OMProcessPropertyType.class, procProp -> {
                procProp.setHref("#proc-" + sigmetUUID);
                //
            }));
            omObs.setFeatureOfInterest(create(FeaturePropertyType.class, fppt -> fppt.setHref("#sampling-surface-" + sigmetUUID)));

            omObs.setObservedProperty(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_POSITION_COLLECTION_ANALYSIS)));

            int cnt = 0;
            for (final PhenomenonGeometry geometry : inputs.getForecastGeometries().orElse(Collections.emptyList())) {
                LOG.debug("About to setResult for FPA");
                omObs.setResult(createFPAResult(geometry, designator, cnt, sigmetUUID));
                cnt++;
            }
        })));
    }

    private static SIGMETPositionCollectionPropertyType createFPAResult(final PhenomenonGeometry geometry, final String designator, final int cnt,
            final String sigmetUUID) {
        return create(SIGMETPositionCollectionPropertyType.class,
                spcpt -> spcpt.setSIGMETPositionCollection(create(SIGMETPositionCollectionType.class, spct -> {
                    spct.setId("spc-" + designator + "-" + cnt + "-" + sigmetUUID);
                    spct.getMember().add(create(SIGMETPositionPropertyType.class, sppt -> sppt.setSIGMETPosition(create(SIGMETPositionType.class, spot -> {
                        spot.setId("fpa-pos-" + cnt + "-" + sigmetUUID);
                        spot.setApproximateLocation(geometry.getApproximateLocation().orElse(false));
                        spot.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> avpt.setAirspaceVolume(create(AirspaceVolumeType.class, avt -> {
                            avt.setId("fpa-" + cnt + "-" + sigmetUUID);
                            geometry.getGeometry()
                                    .flatMap(TacOrGeoGeometry::getGeoGeometry)
                                    .ifPresent(geom -> avt.setHorizontalProjection(createSurface(geom, "fpa-sfc-" + cnt + "-" + sigmetUUID)));

                        /*
                        avt.setHorizontalProjection(create(SurfacePropertyType.class, spt -> {
                            spt.setSurface(createAndWrap(SurfaceType.class, sft -> {
                                try {
                                    Geometry geom = geometry.getGeometry().get().getGeoGeometry().get();
                                    if (PointGeometryImpl.class.isAssignableFrom(geom.getClass())) {
                                        List<Double> pts = new ArrayList<>();
                                        for (Double coordElement : ((PointGeometry) geom).getPoint()) {
                                            pts.add(coordElement);
                                        }

                                        JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> {
                                            poly.setExterior(create(AbstractRingPropertyType.class, arpt -> {
                                                arpt.setAbstractRing(createAndWrap(RingType.class, rt -> {
                                                    rt.getCurveMember().add(create(CurvePropertyType.class, curvept -> {
                                                        curvept.setAbstractCurve(createAndWrap(CurveType.class, curvet -> {
                                                            curvet.setId("curve-forecast-" + cnt + "-" + sigmetUUID);
                                                            curvet.setSegments(create(CurveSegmentArrayPropertyType.class, curvesat -> {
                                                                curvesat.getAbstractCurveSegment()
                                                                        .add(createAndWrap(CircleByCenterPointType.class, cbcpt -> {
                                                                            cbcpt.setPos(create(DirectPositionType.class, dpt -> {
                                                                                dpt.getValue().addAll(Arrays.asList(pts.toArray(new Double[0])));
                                                                            }));
                                                                            cbcpt.setNumArc(BigInteger.valueOf(1));
                                                                            cbcpt.setRadius(create(LengthType.class, lt -> {
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
                                                _spapt -> {
                                                    _spapt.getAbstractSurfacePatch().add(ppt);
                                                });

/*                                                SurfacePatchArrayPropertyType sp = of.createSurfacePatchArrayPropertyType();
                                        JAXBElement<SurfacePatchArrayPropertyType> spapt = of.createPolygonPatches(sp);
                                        spapt.getValue().getAbstractSurfacePatch().add(ppt);
                                        */
                        /*
                                        sft.setPatches(spapt);
                                    } else {
                                        List<Double> pts = new ArrayList<Double>();
                                        for (Double coord : ((PolygonGeometryImpl) geom).getExteriorPoints().get(0)) {
                                            pts.add(coord);
                                        }

                                        JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> {
                                            poly.setExterior(create(AbstractRingPropertyType.class, arpt -> {
                                                arpt.setAbstractRing(createAndWrap(LinearRingType.class, lrt -> {
                                                    DirectPositionListType dplt = create(DirectPositionListType.class, dpl -> {
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
                */
                        }))));
                    }))));
                })));
    }

    private static String getTimePeriodId(final SIGMET input) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        final PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "sigmet-" + valTime.getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(time -> time.format(dtf)).orElse("")//
                + "-" + valTime.getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(time -> time.format(dtf)).orElse("");
    }

    protected static TimePeriodPropertyType getTimePeriodPropertyType(final SIGMET input, final String uuid) {
        return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
    }

    protected static Optional<TimePeriodPropertyType> getCancelledTimePeriodPropertyType(final SIGMET input, final String uuid) {
        return input.getCancelledReference().map(airmetReference -> getATimePeriodPropertyType(airmetReference.getValidityPeriod(), uuid));
    }

    protected static TimePeriodPropertyType getATimePeriodPropertyType(final PartialOrCompleteTimePeriod valTime, final String uuid) {
        return create(TimePeriodPropertyType.class, prop -> {
            final TimePeriodType tp = create(TimePeriodType.class);
            tp.setId("validt-" + uuid);
            final TimePositionType beginPos = create(TimePositionType.class);
            startToIWXXMDateTime(valTime).ifPresent(time -> beginPos.getValue().add(time));
            tp.setBeginPosition(beginPos);
            final TimePositionType endPos = create(TimePositionType.class);
            endToIWXXMDateTime(valTime).ifPresent(time -> endPos.getValue().add(time));
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
                getStartTime(tp, refCtx).ifPresent(start -> retval.setStartTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(start).build()));

                getEndTime(tp, refCtx).ifPresent(end -> retval.setEndTime(PartialOrCompleteTimeInstant.builder()//
                        .setCompleteTime(end).build()));
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

    protected abstract T render(final SIGMETType sigmet, final ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

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
        final ConversionResult<T> result = new ConversionResult<>();

        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String sigmetUuid = UUID.randomUUID().toString();
        //        final String validTimeId = "validt-" + sigmetUuid;
        //        final String phenomenonTimeId = "phent-" + sigmetUuid;
        //        final String resultTimeId = "resltt-" + sigmetUuid;
        //        final String procedureId = "proc-" + sigmetUuid;
        //        final String sfSpatialId = "sampling-surface-" + sigmetUuid;
        //        final String foiId = "foi-" + sigmetUuid;

        final SIGMETType sigmet;
        if (input.getCancelledReference().isPresent()) {
            sigmet = create(SIGMETType.class);
            sigmet.setId("ws-" + UUID.randomUUID().toString());
            sigmet.setStatus(SIGMETReportStatusType.CANCELLATION);
        } else {
            switch (input.getPhenomenon().get()) {
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
            sigmet.setStatus(SIGMETReportStatusType.NORMAL);
        }

        //Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .orElseGet(() -> toIWXXMDateTime(ZonedDateTime.now()));

        sigmet.setIssuingAirTrafficServicesUnit(create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
            unit.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + sigmetUuid);
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                slice.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-ts-" + sigmetUuid);
                slice.setValidTime(create(TimePrimitivePropertyType.class, tp -> {
                }));
                slice.setInterpretation("SNAPSHOT");
                slice.setType(create(CodeUnitType.class, codeUnitType -> codeUnitType.setValue("FIC")));
                slice.setUnitName(create(TextNameType.class, tnt -> {
                    tnt.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator());
                    slice.setDesignator(
                            create(CodeOrganisationDesignatorType.class, desig -> desig.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator())));
                }));
            }))));
        }))));

        sigmet.setOriginatingMeteorologicalWatchOffice(create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
            unit.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-" + sigmetUuid);
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                slice.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-ts-" + sigmetUuid);
                slice.setValidTime(create(TimePrimitivePropertyType.class, tp -> {
                }));
                slice.setInterpretation("SNAPSHOT");
                slice.setType(create(CodeUnitType.class, codeUnitType -> codeUnitType.setValue("MWO")));
                slice.setUnitName(create(TextNameType.class, tnt -> {
                    tnt.setValue(input.getMeteorologicalWatchOffice().getName());
                }));
                slice.setDesignator(create(CodeOrganisationDesignatorType.class, desig ->
                    desig.setValue(input.getMeteorologicalWatchOffice().getDesignator())));
            }))));
        }))));

        sigmet.setSequenceNumber(input.getSequenceNumber());

        sigmet.setValidPeriod(getTimePeriodPropertyType(input, sigmetUuid));

        if (sigmet.getStatus().equals(SIGMETReportStatusType.CANCELLATION)) {
            sigmet.setCancelledSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            getCancelledTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input)).ifPresent(sigmet::setCancelledValidPeriod);
            sigmet.setPhenomenon(create(AeronauticalSignificantWeatherPhenomenonType.class,
                    phen -> phen.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)));

            sigmet.setAnalysis(createCancelAnalysis(input, issueTime, sigmetUuid));
            input.getVAInfo()//
                    .flatMap(VAInfo::getVolcanicAshMovedToFIR)//
                    .ifPresent(volcanicAshMovedToFIR -> {
                        final String designator = volcanicAshMovedToFIR.getDesignator();
                        final String airSpaceName = volcanicAshMovedToFIR.getName();
                        sigmet.setVolcanicAshMovedToFIR(create(AirspacePropertyType.class, apt -> {
                            final AirspaceType airspace = create(AirspaceType.class);
                            airspace.setValidTime(null);
                            airspace.setId("movedto-fir-" + designator + "-" + UUID.randomUUID());
                            airspace.getTimeSlice()
                                    .add(create(AirspaceTimeSlicePropertyType.class,
                                            timeSliceProp -> timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                                timeSlice.setInterpretation("SNAPSHOT");
                                                timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                                timeSlice.setAirspaceName(create(TextNameType.class, name -> name.setValue(airSpaceName)));
                                                timeSlice.setId("fir-" + designator + "-" + UUID.randomUUID() + "-ts");
                                                timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, desig -> desig.setValue(designator)));

                                            }))));
                            apt.setAirspace(airspace);
                        }));
                    });
        } else {
            final AeronauticalSignificantWeatherPhenomenonType phenType = create(AeronauticalSignificantWeatherPhenomenonType.class, ref -> {
                ref.setHref(AviationCodeListUser.CODELIST_SIGWX_PHENOMENA_ROOT + input.getPhenomenon().get());
                ref.setTitle("Sigmet PhenomenonType");

            });
            sigmet.setPhenomenon(phenType);

            sigmet.setAnalysis(
                    createAnalysis(input, input.getAirspace().getDesignator(), input.getAirspace().getName(),
                            issueTime, sigmetUuid));
            if ((input.getForecastGeometries().isPresent()) && (input.getForecastGeometries().get().size() > 0)) {
                sigmet.setForecastPositionAnalysis(
                        createForecastPositionAnalysis(input, input.getAirspace().getDesignator(), issueTime, sigmetUuid));
            }

        if ((input.getPhenomenon().get()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA) && input.getVAInfo().isPresent()) {
            final VolcanoDescription volcano = input.getVAInfo().get().getVolcano().get();
            final icao.iwxxm21.ObjectFactory of = new icao.iwxxm21.ObjectFactory();
            ((VolcanicAshSIGMETType) sigmet).getRest().add(of.createVolcanicAshSIGMETTypeEruptingVolcano(create(VolcanoPropertyType.class, vpt -> {

                vpt.setVolcano(createAndWrap(VolcanoType.class, v -> {
                    if (volcano.getVolcanoPosition().isPresent()) {
                        final Double[] pts = volcano.getVolcanoPosition().get().getCoordinates().toArray(new Double[0]);
                        v.setPosition(create(PointPropertyType.class, ppt -> ppt.setPoint(create(PointType.class, pt -> {
                            pt.setPos(create(DirectPositionType.class, dpt -> {
                                dpt.getValue().addAll(Arrays.asList(pts));
                                dpt.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                dpt.setSrsDimension(BigInteger.valueOf(2));
                                dpt.getAxisLabels().add("Lat");
                                dpt.getAxisLabels().add("Lon");
                                dpt.getUomLabels().add("deg");
                                dpt.getUomLabels().add("deg");
                            }));
                            pt.setId("wv-pt-" + sigmetUuid);
                        }))));
                    } else {
                        v.setPosition(create(PointPropertyType.class, p -> p.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING)));
                    }
                    if (volcano.getVolcanoName().isPresent()) {
                        v.setVolcanoName(volcano.getVolcanoName().get());
                        v.setId("wv-" + volcano.getVolcanoName().get().replace(" ", "_") + "-" + sigmetUuid);
                    } else {
                        final String generatedVolcanoName = "Unknown";
                        v.setVolcanoName(generatedVolcanoName);
                        v.setId("wv-" + generatedVolcanoName + "-" + sigmetUuid);
                    }
                }));
            })));
        }
    }

        try {
            this.updateMessageMetadata(input, result, sigmet);
            final T rendered = this.render(sigmet, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
        /*
        if (input.getSigmetPhenomenon().equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
            result.addIssue(validateDocument(((VolcanicAshSIGMETType) sigmet), VolcanicAshSIGMETType.class, getSchemaInfo(), hints));
        } else {
            result.addIssue(validateDocument(sigmet, SIGMETType.class, getSchemaInfo(), hints));
        }
        */
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render SIGMET IWXXM message to String", e));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private OMObservationPropertyType createAnalysis(final SIGMET input, final String designator, final String airspaceName, final String issueTime,
            final String sigmetUUID) {
        return create(OMObservationPropertyType.class, omObsType -> omObsType.setOMObservation(create(OMObservationType.class, omObs -> {
            omObs.setId("analysis-" + sigmetUUID);
            omObs.setType(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            //TODO Only if FCST or OBS time are given
            final String analysisTime = input.getAnalysisGeometries()//
                    .map(AbstractIWXXM21Serializer::getFirstOrNull)//
                    .flatMap(PhenomenonGeometry::getTime)//
                    .<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .orElse(null);
            if (!input.getAnalysisGeometries().get().get(0).getAnalysisType().isPresent() || analysisTime == null) {
                //set Phen time to nil with nilReason of "missing"
                omObs.setPhenomenonTime(
                        create(TimeObjectPropertyType.class, toProp -> toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING)));
            } else {
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, toProp -> {
                    final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                        period.setId("phent-" + sigmetUUID);
                        period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(analysisTime)));
                    });
                    toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                }));
            }
            omObs.setValidTime(create(TimePeriodPropertyType.class, tppt -> tppt.setHref("#validt-" + sigmetUUID)));

            omObs.setResultTime(create(TimeInstantPropertyType.class, tip -> tip.setTimeInstant(create(TimeInstantType.class, ti -> {
                ti.setTimePosition(create(TimePositionType.class, tp -> tp.getValue().add(issueTime)));
                ti.setId("resltt-" + sigmetUUID);
            }))));

            omObs.setProcedure(create(OMProcessPropertyType.class, omppt -> omppt.setAny(createAndWrap(ProcessType.class, process -> {
                process.setId("proc-" + sigmetUUID);
                process.setDescription(create(StringOrRefType.class, descr -> descr.setValue(AviationCodeListUser.CODELIST_VALUE_SIGMET_PROCESS)));
            }))));

            omObs.setObservedProperty(
                    create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            omObs.setFeatureOfInterest(getFeatureOfInterest(input, designator, airspaceName, "sampling-surface-" + sigmetUUID));

            final SIGMETEvolvingConditionCollectionPropertyType _seccpt = getResult(input, sigmetUUID);
            omObs.setResult(_seccpt);
        })));
    }

    private FeaturePropertyType getFeatureOfInterest(final SIGMET input, final String designator, final String airSpaceName, final String sfSpatialUUID) {
        return create(FeaturePropertyType.class, prop -> prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, samsFeature -> {
            samsFeature.setId(sfSpatialUUID);
            samsFeature.setType(create(ReferenceType.class, ref -> {
                ref.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_OM_SAMPLING + "SF_SamplingSurface");
                ref.setTitle("Sampling surface");
            }));

            samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, samProp -> {
                final AirspaceType airspace = create(AirspaceType.class);
                airspace.setValidTime(null);
                airspace.setId("fir-" + designator + "-" + UUID.randomUUID());
                airspace.getTimeSlice()
                        .add(create(AirspaceTimeSlicePropertyType.class,
                                timeSliceProp -> timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                    timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                    timeSlice.setInterpretation("SNAPSHOT");
                                    timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                    timeSlice.setAirspaceName(create(TextNameType.class, name -> name.setValue(airSpaceName)));
                                    timeSlice.setId("fir-" + designator + "-" + UUID.randomUUID() + "-ts");
                                    timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, desig -> desig.setValue(designator)));

                                }))));
                samProp.setAbstractFeature(wrap(airspace, AirspaceType.class));
            }));
            samsFeature.setShape(create(ShapeType.class, shp -> shp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_WITHHELD)));
        })));
    }

    private SIGMETEvolvingConditionCollectionPropertyType getResult(final SIGMET input, final String sigmetUUID) {
        return create(SIGMETEvolvingConditionCollectionPropertyType.class,
                seccpt -> seccpt.setSIGMETEvolvingConditionCollection(create(SIGMETEvolvingConditionCollectionType.class, secct -> {
                    secct.setId("fcst-" + sigmetUUID);
                    secct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                    if (input.getAnalysisGeometries().get().get(0).getAnalysisType().orElse(null) == SigmetAnalysisType.FORECAST) {
                        secct.setTimeIndicator(TimeIndicatorType.FORECAST);
                    }
                    final int cnt = 0;
                    for (final PhenomenonGeometryWithHeight geometryWithHeight : input.getAnalysisGeometries().orElse(Collections.emptyList())) {
                        secct.getMember()
                                .add(create(SIGMETEvolvingConditionPropertyType.class,
                                        secpt -> secpt.setSIGMETEvolvingCondition(create(SIGMETEvolvingConditionType.class, sect -> {
                                            sect.setId("sec-" + cnt + "-" + sigmetUUID);

                                            sect.setApproximateLocation(geometryWithHeight.getApproximateLocation().orElse(false));
                                            input.getAnalysisGeometries().get().get(0).getIntensityChange().ifPresent(intensityChange -> {
                                                switch (intensityChange) {
                                                    case WEAKENING:
                                                        sect.setIntensityChange(ExpectedIntensityChangeType.WEAKEN);
                                                        break;
                                                    case INTENSIFYING:
                                                        sect.setIntensityChange(ExpectedIntensityChangeType.INTENSIFY);
                                                        break;
                                                    case NO_CHANGE:
                                                    default:
                                                        sect.setIntensityChange(ExpectedIntensityChangeType.NO_CHANGE);
                                                        break;
                                                }
                                            });
                                            if (input.getAnalysisGeometries().get().get(0).getMovingDirection().isPresent()) {
                                                final icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                                final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                                final NumericMeasure md = input.getAnalysisGeometries().get().get(0).getMovingDirection().get();
                                                angl.setUom(md.getUom());
                                                angl.setValue(md.getValue());

                                                final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createSIGMETEvolvingConditionTypeDirectionOfMotion(
                                                        angl);
                                                sect.setDirectionOfMotion(directionOfMotion);

                                                input.getAnalysisGeometries().get().get(0).getMovingSpeed().ifPresent(ms -> sect.setSpeedOfMotion(create(SpeedType.class, spd -> {

                                                    if (ms.getUom().equals("KT")) {
                                                        spd.setUom("[kn_i]");
                                                    } else if (ms.getUom().equals("KMH")) {
                                                        spd.setUom("km/h");
                                                    } else {
                                                        spd.setUom(ms.getUom());
                                                    }
                                                    spd.setValue(ms.getValue());
                                                })));

                                            } else { //Add zero speedOfMotion if STNR and nothing if there is an end position
                                                if (!(input.getForecastGeometries().isPresent() && (input.getForecastGeometries().get().size() != 0))) {
                                                    sect.setSpeedOfMotion(create(SpeedType.class, spd -> {
                                                        spd.setUom("[kn_i]");
                                                        spd.setValue(0);
                                                    }));
                                                }

                                            }

                                            sect.setGeometry(create(AirspaceVolumePropertyType.class,
                                                    avpt -> avpt.setAirspaceVolume(create(AirspaceVolumeType.class, avt -> {
                                                        avt.setId("as-" + cnt + "-" + sigmetUUID);
                                                        geometryWithHeight.getUpperLimit().ifPresent(l -> {
                                                            toValDistanceVertical(l).ifPresent(avt::setUpperLimit);
                                                            avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                                                                if (l.getValue() == 0.) {
                                                                    cvrt.setValue("SFC");
                                                                } else {
                                                                    cvrt.setValue("STD");
                                                                }
                                                            }));
                                                        });
                                                        geometryWithHeight.getLowerLimit().ifPresent(l -> {
                                                            toValDistanceVertical(l).ifPresent(avt::setLowerLimit);
                                                            avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                                                                if (l.getValue() == 0.) {
                                                                    cvrt.setValue("SFC");
                                                                } else {
                                                                    cvrt.setValue("STD");
                                                                }
                                                            }));
                                                        });
                                                        geometryWithHeight.getGeometry()
                                                                .flatMap(TacOrGeoGeometry::getGeoGeometry)
                                                                .ifPresent(geom -> avt.setHorizontalProjection(
                                                                        createSurface(geom, "an-sfc-" + cnt + "-" + sigmetUUID)));
                            /*
                            avt.setHorizontalProjection(create(SurfacePropertyType.class, spt -> {
                                spt.setSurface(createAndWrap(SurfaceType.class, sft -> {
                                    try {
                                        if (geometryWithHeight.getGeometry().isPresent()) {
                                            Geometry geom = geometryWithHeight.getGeometry().get().getGeoGeometry().get();
                                            if (PointGeometryImpl.class.isAssignableFrom(geom.getClass())) {
                                                List<Double> pts = new ArrayList<Double>();
                                                for (Double coordElement : ((PointGeometryImpl) geom).getPoint()) {
                                                    pts.add(coordElement);
                                                }
                                                JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> {
                                                    poly.setExterior(create(AbstractRingPropertyType.class, arpt -> {
                                                        arpt.setAbstractRing(createAndWrap(RingType.class, rt -> {
                                                            rt.getCurveMember().add(create(CurvePropertyType.class, curvept -> {
                                                                curvept.setAbstractCurve(createAndWrap(CurveType.class, curvet -> {
                                                                    curvet.setId("curve-analysis-" + cnt + "-" + sigmetUUID);
                                                                    curvet.setSegments(create(CurveSegmentArrayPropertyType.class, curvesat -> {
                                                                        curvesat.getAbstractCurveSegment()
                                                                                .add(createAndWrap(CircleByCenterPointType.class, cbcpt -> {
                                                                                    cbcpt.setPos(create(DirectPositionType.class, dpt -> {
                                                                                        dpt.getValue()
                                                                                                .addAll(Arrays.asList(pts.toArray(new Double[0])));
                                                                                    }));
                                                                                    cbcpt.setNumArc(BigInteger.valueOf(1));
                                                                                    cbcpt.setRadius(create(LengthType.class, lt -> {
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
                                                    _spapt -> {
                                                        _spapt.getAbstractSurfacePatch().add(ppt);
                                                    });*/
                            /*
                                                sft.setPatches(spapt);
                                            } else {
                                                List<Double> pts = new ArrayList<Double>();
                                                for (Double coord : ((PolygonGeometryImpl) geom).getExteriorPoints().get(0)) {
                                                    pts.add(coord);
                                                }
                                                ;
                                                JAXBElement<PolygonPatchType> ppt = createAndWrap(PolygonPatchType.class, poly -> {
                                                    poly.setExterior(create(AbstractRingPropertyType.class, arpt -> {
                                                        arpt.setAbstractRing(createAndWrap(LinearRingType.class, lrt -> {
                                                            DirectPositionListType dplt = create(DirectPositionListType.class, dpl -> {
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
                            */
                                                    }))));
                                            if (geometryWithHeight.getLowerLimitOperator().isPresent()) {
                                                sect.setGeometryLowerLimitOperator(
                                                        RelationalOperatorType.fromValue(geometryWithHeight.getLowerLimitOperator().get().name()));
                                            }
                                            if (geometryWithHeight.getUpperLimitOperator().isPresent()) {
                                                sect.setGeometryUpperLimitOperator(
                                                        RelationalOperatorType.fromValue(geometryWithHeight.getUpperLimitOperator().get().name()));
                                            }
                                        }))));
                    }
                })));
    }

    private OMObservationPropertyType createCancelAnalysis(final SIGMET input, final String issueTime, final String sigmetUUID) {
        return create(OMObservationPropertyType.class, omObsType -> omObsType.setOMObservation(create(OMObservationType.class, omObs -> {
            omObs.setId("cnl-analysis");
            omObs.setType(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));
            omObs.setPhenomenonTime(
                    create(TimeObjectPropertyType.class, toProp -> toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)));
            omObs.setResultTime(create(TimeInstantPropertyType.class, tip -> tip.setHref("#" + "resltt-" + sigmetUUID)));

            omObs.setProcedure(create(OMProcessPropertyType.class, omppt -> omppt.setAny(createAndWrap(ProcessType.class, process -> {
                process.setId("p-49-2-sigmet-" + sigmetUUID);
                process.setDescription(create(StringOrRefType.class, descr -> descr.setValue(AviationCodeListUser.CODELIST_VALUE_SIGMET_PROCESS)));
            }))));

            omObs.setObservedProperty(
                    create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            final FeaturePropertyType ftp = create(FeaturePropertyType.class,
                    prop -> prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, samsFeature -> {
                        samsFeature.setId("sampling-surface-" + input.getAirspace().getDesignator() + "-" + UUID.randomUUID());
                        samsFeature.setType(create(ReferenceType.class, ref -> {
                            ref.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_OM_SAMPLING + "SF_SamplingSurface");
                            ref.setTitle("Sampling surface");
                        }));

                        samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, samProp -> {
                            final AirspaceType airspace = create(AirspaceType.class);
                            airspace.setValidTime(null);
                            airspace.setId("fir-" + input.getAirspace().getDesignator() + "-" + UUID.randomUUID());
                            airspace.getTimeSlice()
                                    .add(create(AirspaceTimeSlicePropertyType.class,
                                            timeSliceProp -> timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                                timeSlice.setInterpretation("SNAPSHOT");
                                                timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                                timeSlice.setAirspaceName(
                                                        create(TextNameType.class, name -> name.setValue(input.getAirspace().getName())));
                                                timeSlice.setId(
                                                        "fir-" + input.getAirspace().getDesignator() + "-" + UUID.randomUUID() + "-ts");
                                                timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class,
                                                        desig -> desig.setValue(input.getAirspace().getDesignator())));

                                            }))));
                            samProp.setAbstractFeature(wrap(airspace, AirspaceType.class));
                        }));
                        samsFeature.setShape(create(ShapeType.class, shp -> shp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_WITHHELD)));
                    })));
            omObs.setFeatureOfInterest(ftp);
            final SIGMETEvolvingConditionCollectionPropertyType _seccpt = create(SIGMETEvolvingConditionCollectionPropertyType.class,
                    eccpt -> eccpt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE));
            omObs.setResult(_seccpt);
        })));
    }

    protected void updateMessageMetadata(final SIGMET source, final ConversionResult<?> results, final SIGMETType target) throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();

            //Default permissions
            target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
            target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
            source.getPermissibleUsage().ifPresent(us -> {
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
                source.getTranslatedBulletinID().ifPresent(target::setTranslatedBulletinID);
                source.getTranslatedBulletinReceptionTime()//
                        .map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time)))//
                        .ifPresent(target::setTranslatedBulletinReceptionTime);
                source.getTranslationCentreDesignator().ifPresent(target::setTranslationCentreDesignator);
                source.getTranslationCentreName().ifPresent(target::setTranslationCentreName);
                source.getTranslationTime()//
                        .map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time)))//
                        .ifPresent(target::setTranslationTime);
                if (results.getStatus() != Status.SUCCESS) {
                    source.getTranslatedTAC().ifPresent(target::setTranslationFailedTAC);
                }
            }
        } catch (final DatatypeConfigurationException e) {
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
        return create(TimePeriodPropertyType.class, prop -> {
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
    protected InputStream getCleanupTransformationStylesheet(final ConversionHints hints) throws ConversionException {
        final InputStream retval = this.getClass().getResourceAsStream("SIGMETCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class ToDOM extends SIGMETIWXXMSerializer<Document> {
        @Override
        protected Document render(final SIGMETType sigmet, final ConversionHints hints) throws ConversionException {
            return renderXMLDocument(sigmet, hints);
        }

        @Override
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return SIGMETIWXXMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

    public static class ToString extends SIGMETIWXXMSerializer<String> {
        @Override
        protected String render(final SIGMETType sigmet, final ConversionHints hints) throws ConversionException {
            final Document result = renderXMLDocument(sigmet, hints);
            return renderDOMToString(result, hints);
        }

        @Override
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return SIGMETIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }
}
