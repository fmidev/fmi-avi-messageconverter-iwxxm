package fi.fmi.avi.converter.iwxxm.v3_0.sigmet;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import aero.aixm511.AirspaceTimeSlicePropertyType;
import aero.aixm511.AirspaceTimeSliceType;
import aero.aixm511.AirspaceType;
import aero.aixm511.AirspaceVolumeType;
import aero.aixm511.CodeAirspaceDesignatorType;
import aero.aixm511.CodeAirspaceType;
import aero.aixm511.CodeOrganisationDesignatorType;
import aero.aixm511.CodeUnitType;
import aero.aixm511.CodeVerticalReferenceType;
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
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.PhenomenonGeometry;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.VolcanoDescription;
import fi.fmi.avi.model.AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import icao.iwxxm30.AbstractTimeObjectPropertyType;
import icao.iwxxm30.AeronauticalSignificantWeatherPhenomenonType;
import icao.iwxxm30.AirspacePropertyType;
import icao.iwxxm30.AirspaceVolumePropertyType;
import icao.iwxxm30.AngleWithNilReasonType;
import icao.iwxxm30.ExpectedIntensityChangeType;
import icao.iwxxm30.PermissibleUsageReasonType;
import icao.iwxxm30.PermissibleUsageType;
import icao.iwxxm30.ReportStatusType;
import icao.iwxxm30.SIGMETEvolvingConditionCollectionType;
import icao.iwxxm30.SIGMETEvolvingConditionPropertyType;
import icao.iwxxm30.SIGMETEvolvingConditionType;
import icao.iwxxm30.SIGMETPositionCollectionType;
import icao.iwxxm30.SIGMETPositionPropertyType;
import icao.iwxxm30.SIGMETPositionType;
import icao.iwxxm30.SIGMETType;
import icao.iwxxm30.StringWithNilReasonType;
import icao.iwxxm30.TimeIndicatorType;
import icao.iwxxm30.TropicalCycloneSIGMETType;
import icao.iwxxm30.UnitPropertyType;
import icao.iwxxm30.VolcanicAshSIGMETEvolvingConditionCollectionType;
import icao.iwxxm30.VolcanicAshSIGMETPositionCollectionType;
import icao.iwxxm30.VolcanicAshSIGMETType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.AssociationRoleType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.PointPropertyType;
import net.opengis.gml32.PointType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;
import net.opengis.om20.TimeObjectPropertyType;
import wmo.metce2013.VolcanoPropertyType;
import wmo.metce2013.VolcanoType;

public abstract class SIGMETIWXXMSerializer<T> extends AbstractIWXXM30Serializer<SIGMET, T> {
    private static final Logger LOG = LoggerFactory.getLogger(SIGMETIWXXMSerializer.class);

    @SuppressWarnings("unchecked")
    private JAXBElement<SIGMETPositionCollectionType> createFPA(final List<PhenomenonGeometry> fcs,
            final String fcTime) {

        AbstractTimeObjectPropertyType phenTimeProp = create(AbstractTimeObjectPropertyType.class, toProp -> {
            final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                period.setId(getUUID());
                period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(fcTime)));
            });
            toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
        });

        JAXBElement<SIGMETPositionCollectionType> spc = createAndWrap(SIGMETPositionCollectionType.class, spct -> {
            spct.setPhenomenonTime(phenTimeProp);
            spct.setId(getUUID());

            for (PhenomenonGeometry fc : fcs) {
                LOG.info("adding forecast geometry " + fc.toString());
                spct.getMember().add(create(SIGMETPositionPropertyType.class, sppt -> {
                    sppt.setSIGMETPosition(create(SIGMETPositionType.class, spt -> {
                        spt.setApproximateLocation(false);
                        spt.setId(getUUID());
                        spt.setGeometry(null);
                        spt.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> {
                            avpt.setAirspaceVolume(createAirspaceVolume(fc));
                        }));
                    }));

                }));
            }

        });
        return spc;
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<VolcanicAshSIGMETPositionCollectionType> createVolcanicAshFPA(final List<PhenomenonGeometry> fcs,
            final String fcTime, final boolean noVaExp, String volcanoId) {

        AbstractTimeObjectPropertyType phenTimeProp = create(AbstractTimeObjectPropertyType.class, toProp -> {
            final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                period.setId(getUUID());
                period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(fcTime)));
            });
            toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
        });
        JAXBElement<VolcanicAshSIGMETPositionCollectionType> spc = createAndWrap(VolcanicAshSIGMETPositionCollectionType.class, spct -> {
            spct.setPhenomenonTime(phenTimeProp);
            spct.setId(getUUID());
            spct.setVolcanoId(volcanoId);

            if (noVaExp) {
                spct.getMember().add(create(SIGMETPositionPropertyType.class, sppt -> {
                    sppt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                }));
            } else {
                for (PhenomenonGeometry fc : fcs) {
                    LOG.info("adding forecast geometry " + fc.toString());
                    spct.getMember().add(create(SIGMETPositionPropertyType.class, sppt -> {
                        sppt.setSIGMETPosition(create(SIGMETPositionType.class, spt -> {
                            spt.setApproximateLocation(false);
                            spt.setId(getUUID());
                            spt.setGeometry(null);
                            spt.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> {
                                avpt.setAirspaceVolume(createAirspaceVolume(fc));
                            }));
                        }));

                    }));
                }
            }
        });
        return spc;
    }

    private static String getTimePeriodId(final SIGMET input) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        final PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "sigmet-"
                + valTime.getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                        .map(time -> time.format(dtf)).orElse("")//
                + "-" + valTime.getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                        .map(time -> time.format(dtf)).orElse("");
    }

    protected static TimePeriodPropertyType getTimePeriodPropertyType(final SIGMET input, final String uuid) {
        return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
    }

    protected static Optional<TimePeriodPropertyType> getCancelledTimePeriodPropertyType(final SIGMET input,
            final String uuid) {
        return input.getCancelledReference()
                .map(airmetReference -> getATimePeriodPropertyType(airmetReference.getValidityPeriod(), uuid));
    }

    protected static TimePeriodPropertyType getATimePeriodPropertyType(final PartialOrCompleteTimePeriod valTime,
            final String uuid) {
        return create(TimePeriodPropertyType.class, prop -> {
            final TimePeriodType tp = create(TimePeriodType.class);
            tp.setId(getUUID());
            final TimePositionType beginPos = create(TimePositionType.class);
            startToIWXXMDateTime(valTime).ifPresent(time -> beginPos.getValue().add(time));
            tp.setBeginPosition(beginPos);
            final TimePositionType endPos = create(TimePositionType.class);
            endToIWXXMDateTime(valTime).ifPresent(time -> endPos.getValue().add(time));
            tp.setEndPosition(endPos);
            prop.setTimePeriod(tp);
        });
    }

    public static Optional<PartialOrCompleteTimePeriod> getCompleteTimePeriod(
            final TimeObjectPropertyType timeObjectPropertyType, final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject",
                AbstractTimeObjectType.class, refCtx);
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

    public static Optional<PartialOrCompleteTimeInstant> getCompleteTimeInstant(
            final TimeObjectPropertyType timeObjectPropertyType, final ReferredObjectRetrievalContext refCtx) {
        final Optional<AbstractTimeObjectType> to = resolveProperty(timeObjectPropertyType, "abstractTimeObject",
                AbstractTimeObjectType.class, refCtx);
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

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
            throws ConversionException;

    /**
     * Converts a SIGMET object into another format.
     *
     * @param input input message
     * @param hints parsing hints
     *
     * @return the conversion result.
     */
    @Override
    public ConversionResult<T> convertMessage(final SIGMET input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        LOG.info("Starting conversion of SIGMET");
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String sigmetUuid = getUUID();

        final SIGMETType sigmet;
        switch (input.getSigmetPhenomenon().get()) {
            case TC:
                sigmet = create(TropicalCycloneSIGMETType.class);
                sigmet.setId(getUUID());
                break;
            case VA:
                sigmet = create(VolcanicAshSIGMETType.class);
                sigmet.setId(getUUID());
                break;
            default:
                sigmet = create(SIGMETType.class);
                sigmet.setId(getUUID());
        }

        if (input.getCancelledReference().isPresent()) {
            sigmet.setId(getUUID());
            sigmet.setReportStatus(ReportStatusType.NORMAL);
            sigmet.setIsCancelReport(true);
            input.getVAInfo().get();
            sigmet.setCancelledReportSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            getCancelledTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input))
                    .ifPresent(sigmet::setCancelledReportValidPeriod);
            // sigmet.setPhenomenon(create(AeronauticalSignificantWeatherPhenomenonType.class,
            //         phen -> phen.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)));
            if (input.getSigmetPhenomenon().get().equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
                input.getVAInfo().ifPresent(v -> {
                    v.getVolcanicAshMovedToFIR().ifPresent(movedToFir -> {
                        ((VolcanicAshSIGMETType) sigmet).setVolcanicAshMovedToFIR(
                            create(AirspacePropertyType.class, prop -> prop.setAirspace(create(AirspaceType.class, airspace -> {
                                airspace.setValidTime(null);
                                airspace.setId(getUUID());
                                airspace.getTimeSlice()
                                        .add(create(AirspaceTimeSlicePropertyType.class, timeSliceProp -> timeSliceProp
                                            .setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                                timeSlice.setInterpretation("SNAPSHOT");
                                                timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                                timeSlice.setAirspaceName(create(TextNameType.class, name -> name
                                                        .setValue(input.getIssuingAirTrafficServicesUnit().getName())));
                                                timeSlice.setId(getUUID());
                                                timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, desig -> desig
                                                        .setValue(input.getIssuingAirTrafficServicesUnit().getDesignator())));

                                            }))));

                        }))));
                    });
                });
            }
        } else {
            sigmet.setReportStatus(ReportStatusType.NORMAL);
            sigmet.setIsCancelReport(false);
            sigmet.setPhenomenon(create(AeronauticalSignificantWeatherPhenomenonType.class, phen -> phen.setHref(
                    AviationCodeListUser.CODELIST_SIGWX_PHENOMENA_ROOT + input.getSigmetPhenomenon().get().name())));
        }

        // Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().<String>flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .orElseGet(() -> toIWXXMDateTime(ZonedDateTime.now()));

        sigmet.setIssueTime(
                create(TimeInstantPropertyType.class, tip -> tip.setTimeInstant(create(TimeInstantType.class, ti -> {
                    ti.setTimePosition(create(TimePositionType.class, tp -> tp.getValue().add(issueTime)));
                    ti.setId(getUUID());
                }))));

        sigmet.setIssuingAirTrafficServicesUnit(
                create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
                    unit.setId(getUUID());
                    unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class,
                            sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                                slice.setId(getUUID());
                                slice.setValidTime(create(TimePrimitivePropertyType.class, tp -> {
                                }));
                                slice.setInterpretation("SNAPSHOT");
                                slice.setType(create(CodeUnitType.class, codeUnitType -> codeUnitType.setValue("FIC")));
                                slice.setUnitName(create(TextNameType.class, tnt -> {
                                    tnt.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator());
                                    slice.setDesignator(create(CodeOrganisationDesignatorType.class, desig -> desig
                                            .setValue(input.getIssuingAirTrafficServicesUnit().getDesignator())));
                                }));
                            }))));
                }))));

        sigmet.setOriginatingMeteorologicalWatchOffice(
                create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
                    unit.setId(getUUID());
                    unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class,
                            sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                                slice.setId(getUUID());
                                slice.setValidTime(create(TimePrimitivePropertyType.class, tp -> {
                                }));
                                slice.setInterpretation("SNAPSHOT");
                                slice.setType(create(CodeUnitType.class, codeUnitType -> codeUnitType.setValue("MWO")));
                                slice.setUnitName(create(TextNameType.class, tnt -> {
                                    tnt.setValue(input.getMeteorologicalWatchOffice().getDesignator() + " MWO");
                                    slice.setDesignator(create(CodeOrganisationDesignatorType.class, desig -> desig
                                            .setValue(input.getMeteorologicalWatchOffice().getDesignator())));
                                }));
                            }))));
                }))));

        sigmet.setIssuingAirTrafficServicesRegion(
                create(AirspacePropertyType.class, prop -> prop.setAirspace(create(AirspaceType.class, airspace -> {
                    airspace.setValidTime(null);
                    airspace.setId(getUUID());
                    airspace.getTimeSlice()
                            .add(create(AirspaceTimeSlicePropertyType.class, timeSliceProp -> timeSliceProp
                                    .setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                        timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                        timeSlice.setInterpretation("SNAPSHOT");
                                        timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                        timeSlice.setAirspaceName(create(TextNameType.class, name -> name
                                                .setValue(input.getIssuingAirTrafficServicesUnit().getName())));
                                        timeSlice.setId(getUUID());
                                        timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class, desig -> desig
                                                .setValue(input.getIssuingAirTrafficServicesUnit().getDesignator())));

                                    }))));

                }))));

        sigmet.setSequenceNumber(
                create(StringWithNilReasonType.class, prop -> prop.setValue(input.getSequenceNumber())));

        sigmet.setValidPeriod(getTimePeriodPropertyType(input, sigmetUuid));

        if (input.getCancelledReference().isEmpty()) {
            final String analysisTime = input.getAnalysisGeometries()//
                    .map(AbstractIWXXM30Serializer::getFirstOrNull)//
                    .flatMap(PhenomenonGeometry::getTime)//
                    .<String>flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .orElse(null);
            boolean noForecasts = true;
            if (input.getForecastGeometries().isPresent() && input.getForecastGeometries().get().size() > 0) {
                noForecasts = false;
            }

            String volcanoId = getUUID();
            List<PhenomenonGeometryWithHeight> ans = input.getAnalysisGeometries().get();
            if (input.getSigmetPhenomenon().get().equals(AeronauticalSignificantWeatherPhenomenon.VA)) {
                JAXBElement<VolcanicAshSIGMETEvolvingConditionCollectionType> secct =
                    createVolcanicAnalysis(ans, analysisTime,
                        input.getIssuingAirTrafficServicesUnit().getDesignator(),
                        input.getIssuingAirTrafficServicesUnit().getName(),
                        issueTime, sigmetUuid, noForecasts, volcanoId);
                sigmet.getAnalysis().add(create(AssociationRoleType.class, at -> at.setAny(secct)));
            } else {

                JAXBElement<SIGMETEvolvingConditionCollectionType> secct = createAnalysis(ans, analysisTime,
                        input.getIssuingAirTrafficServicesUnit().getDesignator(),
                        input.getIssuingAirTrafficServicesUnit().getName(), issueTime, sigmetUuid, noForecasts);
                sigmet.getAnalysis().add(create(AssociationRoleType.class, at -> at.setAny(secct)));
            }
            // ObjectMapper om = new
            // ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            // try {
            // LOG.info("secct:"+om.writeValueAsString(secct));
            // } catch (JsonProcessingException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }

            final String fcTime = input.getForecastGeometries()//
                    .map(AbstractIWXXM30Serializer::getFirstOrNull)//
                    .flatMap(PhenomenonGeometry::getTime)//
                    .<String>flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .orElse(null);

            if (input.getForecastGeometries().isPresent()) {
                if ((input.getSigmetPhenomenon().get()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
                    boolean noVaExp = false;
                    if (input.getVAInfo().isPresent())  {
                        noVaExp = input.getVAInfo().get().getNoVolcanicAshExpected().orElse(false);
                    };
                    JAXBElement<VolcanicAshSIGMETPositionCollectionType> fpa =
                        createVolcanicAshFPA(input.getForecastGeometries().get(), fcTime, noVaExp, volcanoId);
                    sigmet.getForecastPositionAnalysis().add(create(AssociationRoleType.class, at -> at.setAny(fpa)));
                } else {
                    JAXBElement<SIGMETPositionCollectionType> fpa = createFPA(input.getForecastGeometries().get(), fcTime);
                    sigmet.getForecastPositionAnalysis().add(create(AssociationRoleType.class, at -> at.setAny(fpa)));
                }
            }

            if ((input.getSigmetPhenomenon().get()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA) && input.getVAInfo().isPresent()) {
                final VolcanoDescription volcano = input.getVAInfo().get().getVolcano().get();

                ((VolcanicAshSIGMETType) sigmet).getEruptingVolcano().add(create(VolcanoPropertyType.class, vpt -> {

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
                                pt.setId(getUUID());
                            }))));
                        } else {
                            v.setPosition(create(PointPropertyType.class, p -> p.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING)));
                        }
                        if (volcano.getVolcanoName().isPresent()) {
                            v.setVolcanoName(volcano.getVolcanoName().get());
                            v.setId(getUUID());
                        } else {
                            final String generatedVolcanoName = "Unknown";
                            v.setVolcanoName(generatedVolcanoName);
                            v.setId(getUUID());
                        }
                    }));
                }));
            }
        }
        try {
            this.updateMessageMetadata(input, result, sigmet);
            final T rendered = this.render(sigmet, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            /*
             * if (input.getSigmetPhenomenon().equals(AviationCodeListUser.
             * AeronauticalSignificantWeatherPhenomenon.VA)) {
             * result.addIssue(validateDocument(((VolcanicAshSIGMETType) sigmet),
             * VolcanicAshSIGMETType.class, getSchemaInfo(), hints)); } else {
             * result.addIssue(validateDocument(sigmet, SIGMETType.class, getSchemaInfo(),
             * hints)); }
             */
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER,
                    "Unable to render SIGMET IWXXM message to String" + " " + e.getCause(), e));
        }
        return result;
    }
    @SuppressWarnings("unchecked")

    private JAXBElement<SIGMETEvolvingConditionCollectionType> createAnalysis(
            final List<PhenomenonGeometryWithHeight> ans, final String analysisTime, final String designator,
            final String airspaceName, final String issueTime, final String sigmetUUID, boolean noForecasts) {

        AbstractTimeObjectPropertyType phenTimeProp = create(AbstractTimeObjectPropertyType.class, toProp -> {
            final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                period.setId(getUUID());
                period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(analysisTime)));
            });
            toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
        });

        JAXBElement<SIGMETEvolvingConditionCollectionType> sigmetEvolvingConditionCollectionType = createAndWrap(
                SIGMETEvolvingConditionCollectionType.class, ecct -> {
                    ecct.setPhenomenonTime(phenTimeProp);
                    ecct.setId(getUUID());
                    for (PhenomenonGeometryWithHeight an : ans) {
                        if (an.getAnalysisType().equals(SigmetAnalysisType.FORECAST)) {
                            ecct.setTimeIndicator(TimeIndicatorType.FORECAST);
                        } else if (an.getAnalysisType().equals(SigmetAnalysisType.OBSERVATION)) {
                            ecct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                        } else {
                            ecct.setTimeIndicator(null);
                        }
                        ecct.getMember().add(create(SIGMETEvolvingConditionPropertyType.class, seccpt -> {
                            seccpt.setSIGMETEvolvingCondition(create(SIGMETEvolvingConditionType.class, sect -> {
                                sect.setIntensityChange(ExpectedIntensityChangeType.NO_CHANGE);
                                sect.setId(getUUID());
                                sect.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> {
                                    avpt.setAirspaceVolume(createAirspaceVolume(an));
                                }));
                                if (noForecasts) { // Only add Motion elements if there are no forecasts
                                    final icao.iwxxm30.ObjectFactory of_iwxxm30 = new icao.iwxxm30.ObjectFactory();
                                    if (an.getMovingDirection().isPresent()) {
                                        final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                        final NumericMeasure md = an.getMovingDirection().get();
                                        angl.setUom(md.getUom());
                                        angl.setValue(md.getValue());
                                        final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm30
                                                .createSIGMETEvolvingConditionTypeDirectionOfMotion(angl);
                                        sect.setDirectionOfMotion(directionOfMotion);

                                        an.getMovingSpeed()
                                                .ifPresent(ms -> sect.setSpeedOfMotion(create(SpeedType.class, spd -> {
                                                    if (ms.getUom().equals("KT")) {
                                                        spd.setUom("[kn_i]");
                                                    } else if (ms.getUom().equals("KMH")) {
                                                        spd.setUom("km/h");
                                                    } else {
                                                        spd.setUom(ms.getUom());
                                                    }
                                                    spd.setValue(ms.getValue());
                                                })));
                                    } else { // Add zero speedOfMotion if STN
                                        // final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                        // angl.getNilReason()
                                        //         .add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                                        // angl.setUom("deg");
                                        // final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm30
                                        //         .createSIGMETEvolvingConditionTypeDirectionOfMotion(angl);
                                        // sect.setDirectionOfMotion(directionOfMotion);
                                        // sect.setSpeedOfMotion(create(SpeedType.class, spd -> {
                                        //     spd.setUom("[kn_i]");
                                        //     spd.setValue(0);
                                        // }));
                                    }
                                }

                            }));
                        }));
                    }
                });

        return sigmetEvolvingConditionCollectionType;
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<VolcanicAshSIGMETEvolvingConditionCollectionType> createVolcanicAnalysis(
            final List<PhenomenonGeometryWithHeight> ans, final String analysisTime, final String designator,
            final String airspaceName, final String issueTime, final String sigmetUUID, boolean noForecasts,
            String volcanoId) {

        AbstractTimeObjectPropertyType phenTimeProp = create(AbstractTimeObjectPropertyType.class, toProp -> {
            final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                period.setId(getUUID());
                period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(analysisTime)));
            });
            toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
        });

        JAXBElement<VolcanicAshSIGMETEvolvingConditionCollectionType> sigmetEvolvingConditionCollectionType = createAndWrap(
            VolcanicAshSIGMETEvolvingConditionCollectionType.class, ecct -> {
                    ecct.setPhenomenonTime(phenTimeProp);
                    ecct.setId(getUUID());
                    for (PhenomenonGeometryWithHeight an : ans) {
                        if (an.getAnalysisType().equals(SigmetAnalysisType.FORECAST)) {
                            ecct.setTimeIndicator(TimeIndicatorType.FORECAST);
                        } else if (an.getAnalysisType().equals(SigmetAnalysisType.OBSERVATION)) {
                            ecct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                        } else {
                            ecct.setTimeIndicator(null);
                        }
                        ecct.setVolcanoId(volcanoId);

                        ecct.getMember().add(create(SIGMETEvolvingConditionPropertyType.class, seccpt -> {
                            seccpt.setSIGMETEvolvingCondition(create(SIGMETEvolvingConditionType.class, sect -> {
                                sect.setIntensityChange(ExpectedIntensityChangeType.NO_CHANGE);
                                sect.setId(getUUID());
                                sect.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> {
                                    avpt.setAirspaceVolume(createAirspaceVolume(an));
                                }));
                                if (noForecasts) { // Only add Motion elements if there are no forecasts
                                    final icao.iwxxm30.ObjectFactory of_iwxxm30 = new icao.iwxxm30.ObjectFactory();
                                    if (an.getMovingDirection().isPresent()) {
                                        final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                        final NumericMeasure md = an.getMovingDirection().get();
                                        angl.setUom(md.getUom());
                                        angl.setValue(md.getValue());
                                        final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm30
                                                .createSIGMETEvolvingConditionTypeDirectionOfMotion(angl);
                                        sect.setDirectionOfMotion(directionOfMotion);

                                        an.getMovingSpeed()
                                                .ifPresent(ms -> sect.setSpeedOfMotion(create(SpeedType.class, spd -> {
                                                    if (ms.getUom().equals("KT")) {
                                                        spd.setUom("[kn_i]");
                                                    } else if (ms.getUom().equals("KMH")) {
                                                        spd.setUom("km/h");
                                                    } else {
                                                        spd.setUom(ms.getUom());
                                                    }
                                                    spd.setValue(ms.getValue());
                                                })));
                                    }
                                }

                            }));
                        }));
                    }
                });

        return sigmetEvolvingConditionCollectionType;
    }

    private AirspaceVolumeType createAirspaceVolume(PhenomenonGeometryWithHeight an) {
        NumericMeasure lowerLevel = an.getLowerLimit().orElse(null);
        NumericMeasure upperLevel = an.getUpperLimit().orElse(null);
        AviationCodeListUser.RelationalOperator lowerLimitOperator = an.getLowerLimitOperator().orElse(null);
        AviationCodeListUser.RelationalOperator upperLimitOperator = an.getUpperLimitOperator().orElse(null);
        AirspaceVolumeType avt = create(AirspaceVolumeType.class);
        avt.setId(getUUID());
        if (lowerLevel!=null) {
            if (upperLevel!=null) {
                //BTW BTW_SFC
                avt.setLowerLimit(toValDistanceVertical(lowerLevel).get());
                if (lowerLevel.getValue()==0) {
                    avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                        cvrt.setValue("SFC");
                    }));
                } else {
                    avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                        cvrt.setValue("STD");
                    }));
                }
                avt.setUpperLimit(toValDistanceVertical(upperLevel).get());
                avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    cvrt.setValue("STD");
                }));
        } else if (lowerLimitOperator==null) {
                //AT
                avt.setLowerLimit(toValDistanceVertical(lowerLevel).get());
                avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    cvrt.setValue("STD");
                }));
                avt.setUpperLimit(toValDistanceVertical(lowerLevel).get());
                avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    cvrt.setValue("STD");
                }));
            } else {
                //ABV
                avt.setLowerLimit(toValDistanceVertical(lowerLevel).get());
                avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    cvrt.setValue("STD");
                }));            }
        } else {
            if (upperLevel==null) {
                // In this case no levels are specified
            } else {
                if (upperLimitOperator == null) {
                    //TOP
                    avt.setUpperLimit(toValDistanceVertical(upperLevel).get());
                    avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                        cvrt.setValue("STD");
                    }));
                } else if (AviationCodeListUser.RelationalOperator.ABOVE.equals(upperLimitOperator)) {
                    // TOP ABV
                    avt.setUpperLimit(toValDistanceVertical(upperLevel).get());
                    avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                        cvrt.setValue("STD");
                    }));
                    avt.setMaximumLimit(create(ValDistanceVerticalType.class, vdvt -> {
                        vdvt.setNilReason("unknown");
                    }));
                } else if (AviationCodeListUser.RelationalOperator.BELOW.equals(upperLimitOperator)) {
                    // TOP BLW
                    avt.setUpperLimit(create(ValDistanceVerticalType.class, vdvt -> {
                        vdvt.setNilReason("unknown");
                    }));
                    avt.setMaximumLimit(toValDistanceVertical(upperLevel).get());
                    avt.setMaximumLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                        cvrt.setValue("STD");
                    }));

                }
            }
        }
        an.getGeometry().flatMap(TacOrGeoGeometry::getGeoGeometry)
        .ifPresent(geom -> avt.setHorizontalProjection(createSurface(geom, getUUID())));

        return avt;
    }


    private AirspaceVolumeType createAirspaceVolumeOLD(PhenomenonGeometryWithHeight an) {
        AirspaceVolumeType airspace = create(AirspaceVolumeType.class, avt -> {
            avt.setId(getUUID());
            an.getUpperLimit().ifPresent(l -> {
                toValDistanceVertical(l).ifPresent(avt::setUpperLimit);
                avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    if (l.getValue() == 0.) {
                        cvrt.setValue("SFC");
                    } else {
                        cvrt.setValue("STD");
                    }
                }));
            });
            an.getLowerLimit().ifPresent(l -> {
                toValDistanceVertical(l).ifPresent(avt::setLowerLimit);
                avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, cvrt -> {
                    if (l.getValue() == 0.) {
                        cvrt.setValue("SFC");
                    } else {
                        cvrt.setValue("STD");
                    }
                }));
            });
            an.getGeometry().flatMap(TacOrGeoGeometry::getGeoGeometry)
                    .ifPresent(geom -> avt.setHorizontalProjection(createSurface(geom, getUUID())));

        });
        return airspace;
    }

    private AirspaceVolumeType createAirspaceVolume(PhenomenonGeometry fc) {
        AirspaceVolumeType airspace = create(AirspaceVolumeType.class, avt -> {
            avt.setId(getUUID());
            fc.getGeometry().flatMap(TacOrGeoGeometry::getGeoGeometry)
                    .ifPresent(geom -> avt.setHorizontalProjection(createSurface(geom, getUUID())));

        });
        return airspace;
    }

    protected void updateMessageMetadata(final SIGMET source, final ConversionResult<?> results,
            final SIGMETType target) throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();

            // Default permissions
            target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
            target.setPermissibleUsageReason(PermissibleUsageReasonType.TEST);
            source.getPermissibleUsage().ifPresent(us -> {
                if (us == AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL) {
                    target.setPermissibleUsage(PermissibleUsageType.NON_OPERATIONAL);
                    if (source.getPermissibleUsageReason().isPresent()) {
                        target.setPermissibleUsageReason(
                                PermissibleUsageReasonType.valueOf(source.getPermissibleUsageReason().get().name()));
                    }
                    if ((source.getPermissibleUsageSupplementary() != null)
                            && (source.getPermissibleUsageSupplementary().isPresent())) {
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
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
                throws ConversionException {
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
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
                throws ConversionException {
            return SIGMETIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }
}
