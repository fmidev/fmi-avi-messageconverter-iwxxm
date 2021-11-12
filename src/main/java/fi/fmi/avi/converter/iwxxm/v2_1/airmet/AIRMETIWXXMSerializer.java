package fi.fmi.avi.converter.iwxxm.v2_1.airmet;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.AngleType;
import net.opengis.gml32.FeaturePropertyType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.ReferenceType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.StringOrRefType;
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

import org.w3c.dom.Document;

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
import aero.aixm511.TextNameType;
import aero.aixm511.UnitTimeSlicePropertyType;
import aero.aixm511.UnitTimeSliceType;
import aero.aixm511.UnitType;
import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v2_1.AbstractIWXXM21Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.PhenomenonGeometry;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AirmetWind;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import icao.iwxxm21.AIRMETEvolvingConditionCollectionPropertyType;
import icao.iwxxm21.AIRMETEvolvingConditionCollectionType;
import icao.iwxxm21.AIRMETEvolvingConditionPropertyType;
import icao.iwxxm21.AIRMETEvolvingConditionType;
import icao.iwxxm21.AIRMETExpectedIntensityChangeType;
import icao.iwxxm21.AIRMETReportStatusType;
import icao.iwxxm21.AIRMETType;
import icao.iwxxm21.AeronauticalAreaWeatherPhenomenonType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.PermissibleUsageReasonType;
import icao.iwxxm21.PermissibleUsageType;
import icao.iwxxm21.RelationalOperatorType;
import icao.iwxxm21.TimeIndicatorType;
import icao.iwxxm21.UnitPropertyType;
import icao.iwxxm21.WeatherCausingVisibilityReductionType;
import wmo.metce2013.ProcessType;

public abstract class AIRMETIWXXMSerializer<T> extends AbstractIWXXM21Serializer<AIRMET, T> {
    protected static TimePeriodPropertyType getTimePeriodPropertyType(final AIRMET input, final String uuid) {
        return getATimePeriodPropertyType(input.getValidityPeriod(), uuid);
    }

    protected static Optional<TimePeriodPropertyType> getCancelledTimePeriodPropertyType(final AIRMET input, final String uuid) {
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

    private static String getTimePeriodId(final AIRMET input) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm'Z'");
        final PartialOrCompleteTimePeriod valTime = input.getValidityPeriod();
        return "airmet-" + valTime.getStartTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(time -> time.format(dtf)).orElse("")//
                + "-" + valTime.getEndTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime).map(time -> time.format(dtf)).orElse("");
    }

    protected abstract T render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException;

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
    public ConversionResult<T> convertMessage(final AIRMET input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String airmetUuid = UUID.randomUUID().toString();

        final AIRMETType airmet;

        airmet = create(AIRMETType.class);
        airmet.setId("as-" + UUID.randomUUID().toString());

        //Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .orElseGet(() -> toIWXXMDateTime(ZonedDateTime.now()));

        if (input.getCancelledReference().isPresent()) {
            airmet.setStatus(AIRMETReportStatusType.CANCELLATION);
        } else {
            airmet.setStatus(AIRMETReportStatusType.NORMAL);
        }
        airmet.setIssuingAirTrafficServicesUnit(create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
            unit.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + airmetUuid);
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                slice.setId("fic-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-ts-" + airmetUuid);
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

        airmet.setOriginatingMeteorologicalWatchOffice(create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
            unit.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-" + airmetUuid);
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                slice.setId("mwo-" + input.getMeteorologicalWatchOffice().getDesignator() + "-ts-" + airmetUuid);
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

        if (airmet.getStatus().equals(AIRMETReportStatusType.CANCELLATION)) {
            airmet.setCancelledSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            getCancelledTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input)).ifPresent(airmet::setCancelledValidPeriod);
            airmet.setPhenomenon(create(AeronauticalAreaWeatherPhenomenonType.class,
                    phen -> phen.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)));

            airmet.setAnalysis(createCancelAnalysis(input, issueTime, airmetUuid));

        } else {
            final AeronauticalAreaWeatherPhenomenonType phenType = create(AeronauticalAreaWeatherPhenomenonType.class, ref -> {
                ref.setHref(AviationCodeListUser.CODELIST_AIRMET_PHENOMENA_ROOT + input.getAirmetPhenomenon().get());
                ref.setTitle("Airmet PhenomenonType");

            });
            airmet.setPhenomenon(phenType);

            airmet.setAnalysis(
                    createAnalysis(input, input.getIssuingAirTrafficServicesUnit().getDesignator(), input.getIssuingAirTrafficServicesUnit().getName(),
                            issueTime, airmetUuid));
        }

        airmet.setSequenceNumber(input.getSequenceNumber());

        airmet.setValidPeriod(getTimePeriodPropertyType(input, airmetUuid));

        try {
            this.updateMessageMetadata(input, result, airmet);
            final T rendered = this.render(airmet, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }

        return result;
    }

    /**************************************************************************/

    @SuppressWarnings("unchecked")
    private OMObservationPropertyType createAnalysis(final AIRMET input, final String designator, final String airspaceName, final String issueTime,
            final String airmetUUID) {
        return create(OMObservationPropertyType.class, omObsType -> omObsType.setOMObservation(create(OMObservationType.class, omObs -> {
            omObs.setId("analysis-" + airmetUUID);
            omObs.setType(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            //TODO Only if FCST or OBS time are given

            final String analysisTime = input.getAnalysisGeometries()//
                    .map(AbstractIWXXM21Serializer::getFirstOrNull)//
                    .flatMap(PhenomenonGeometry::getTime)//
                    .<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .orElse(null);
            if (input.getAnalysisGeometries().get().get(0).getAnalysisType() == SigmetAnalysisType.UNKNOWN || analysisTime == null) {
                //set Phen time to nil with nilReason of "missing"
                omObs.setPhenomenonTime(
                        create(TimeObjectPropertyType.class, toProp -> toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING)));
            } else {
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, toProp -> {
                    final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, period -> {
                        period.setId("phent-" + airmetUUID);
                        period.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(analysisTime)));
                    });
                    toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
                }));
            }
            omObs.setValidTime(create(TimePeriodPropertyType.class, tppt -> tppt.setHref("#validt-" + airmetUUID)));

            omObs.setResultTime(create(TimeInstantPropertyType.class, tip -> tip.setTimeInstant(create(TimeInstantType.class, ti -> {
                ti.setTimePosition(create(TimePositionType.class, tp -> tp.getValue().add(issueTime)));
                ti.setId("resltt-" + airmetUUID);
            }))));

            omObs.setProcedure(create(OMProcessPropertyType.class, omppt -> omppt.setAny(createAndWrap(ProcessType.class, process -> {
                process.setId("proc-" + airmetUUID);
                process.setDescription(create(StringOrRefType.class, descr -> descr.setValue(AviationCodeListUser.CODELIST_VALUE_AIRMET_PROCESS)));
            }))));

            omObs.setObservedProperty(
                    create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            omObs.setFeatureOfInterest(getFeatureOfInterest(input, designator, airspaceName, "sampling-surface-" + airmetUUID));

            final AIRMETEvolvingConditionCollectionPropertyType _seccpt = getResult(input, airmetUUID);
            omObs.setResult(_seccpt);
        })));
    }

    private FeaturePropertyType getFeatureOfInterest(final AIRMET input, final String designator, final String airSpaceName, final String sfSpatialUUID) {
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

    private AIRMETEvolvingConditionCollectionPropertyType getResult(final AIRMET input, final String airmetUUID) {
        return create(AIRMETEvolvingConditionCollectionPropertyType.class,
                seccpt -> seccpt.setAIRMETEvolvingConditionCollection(create(AIRMETEvolvingConditionCollectionType.class, secct -> {
                    secct.setId("fcst-" + airmetUUID);
                    secct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                    if (input.getAnalysisGeometries().get().get(0).getAnalysisType() == SigmetAnalysisType.FORECAST) {
                        secct.setTimeIndicator(TimeIndicatorType.FORECAST);
                    }
                    final int cnt = 0;
                    for (final PhenomenonGeometryWithHeight geometryWithHeight : input.getAnalysisGeometries().orElse(Collections.emptyList())) {
                        secct.getMember()
                                .add(create(AIRMETEvolvingConditionPropertyType.class,
                                        secpt -> secpt.setAIRMETEvolvingCondition(create(AIRMETEvolvingConditionType.class, sect -> {
                                            sect.setId("sec-" + cnt + "-" + airmetUUID);

                                            sect.setApproximateLocation(geometryWithHeight.getApproximateLocation().orElse(false));
                                            input.getAnalysisGeometries().get().get(0).getIntensityChange().ifPresent(intensityChange -> {
                                                switch (intensityChange) {
                                                    case WEAKENING:
                                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.WEAKEN);
                                                        break;
                                                    case INTENSIFYING:
                                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.INTENSIFY);
                                                        break;
                                                    case NO_CHANGE:
                                                    default:
                                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.NO_CHANGE);
                                                        break;
                                                }
                                            });

                                            if (input.getAnalysisGeometries().get().get(0).getMovingDirection().isPresent()) {
                                                final icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                                final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                                final NumericMeasure md = input.getAnalysisGeometries().get().get(0).getMovingDirection().get();
                                                angl.setUom(md.getUom());
                                                angl.setValue(md.getValue());

                                                final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createAIRMETEvolvingConditionTypeDirectionOfMotion(
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

                                            } else { //Add zero speedOfMotion for STNR
                                                sect.setSpeedOfMotion(create(SpeedType.class, spd -> {
                                                    spd.setUom("[kn_i]");
                                                    spd.setValue(0);
                                                }));
                                            }

                                            sect.setGeometry(create(AirspaceVolumePropertyType.class,
                                                    avpt -> avpt.setAirspaceVolume(create(AirspaceVolumeType.class, avt -> {
                                                        avt.setId("as-" + cnt + "-" + airmetUUID);
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
                                                                        createSurface(geom, "an-sfc-" + cnt + "-" + airmetUUID)));
                                                    }))));
                                            if (geometryWithHeight.getLowerLimitOperator().isPresent()) {
                                                sect.setGeometryLowerLimitOperator(
                                                        RelationalOperatorType.fromValue(geometryWithHeight.getLowerLimitOperator().get().name()));
                                            }
                                            if (geometryWithHeight.getUpperLimitOperator().isPresent()) {
                                                sect.setGeometryUpperLimitOperator(
                                                        RelationalOperatorType.fromValue(geometryWithHeight.getUpperLimitOperator().get().name()));
                                            }

                                            if (input.getCloudLevels().isPresent()) {
                                                final NumericMeasure base = input.getCloudLevels().get().getCloudBase();
                                                final NumericMeasure top = input.getCloudLevels().get().getCloudTop();
                                                if (base != null) {
                                                    sect.setCloudBase(create(LengthType.class, lt -> {
                                                        lt.setValue(base.getValue());
                                                        if (base.getUom().equalsIgnoreCase("ft")) {
                                                            lt.setUom("[ft_i]");
                                                        } else {
                                                            lt.setUom(base.getUom().toLowerCase(Locale.US));
                                                        }
                                                    }));
                                                }
                                                if (top != null) {
                                                    sect.setCloudTop(create(LengthType.class, lt -> {
                                                        lt.setValue(top.getValue());
                                                        if (top.getUom().equalsIgnoreCase("ft")) {
                                                            lt.setUom("[ft_i]");
                                                        } else {
                                                            lt.setUom(top.getUom().toLowerCase(Locale.US));
                                                        }
                                                    }));
                                                }
                                                if (input.getCloudLevels().get().getTopAbove().isPresent() && (input.getCloudLevels()
                                                        .get()
                                                        .getTopAbove()
                                                        .get())) {
                                                    sect.setGeometryUpperLimitOperator(RelationalOperatorType.ABOVE);
                                                }
                                            }

                                            if (input.getVisibility().isPresent()) {
                                                sect.setSurfaceVisibility(create(LengthType.class, lt -> {
                                                    lt.setValue(input.getVisibility().get().getValue().intValue());
                                                    lt.setUom(input.getVisibility().get().getUom().toLowerCase(Locale.US));
                                                }));
                                                for (final AviationCodeListUser.WeatherCausingVisibilityReduction w : input.getObscuration()
                                                        .orElse(Collections.emptyList())) {
                                                    final WeatherCausingVisibilityReductionType wt = new WeatherCausingVisibilityReductionType();
                                                    wt.setHref(AviationCodeListUser.CODELIST_VALUE_WEATHERCAUSINGVISIBILITYREDUCTION + "/" + w.getText());
                                                    sect.getSurfaceVisibilityCause().add(wt);
                                                }
                                            }

                                            if (input.getWind().isPresent()) {
                                                final AirmetWind w = input.getWind().get();
                                                sect.setSurfaceWindSpeed(create(SpeedType.class, st -> {
                                                    st.setValue(w.getSpeed().getValue());

                                                    if (w.getSpeed().getUom().equals("KT")) {
                                                        st.setUom("[kn_i]");
                                                    } else if (w.getSpeed().getUom().equals("KMH")) {
                                                        st.setUom("km/h");
                                                    } else {
                                                        st.setUom(w.getSpeed().getUom());
                                                    }
                                                }));
                                                sect.setSurfaceWindDirection(create(AngleType.class, at -> {
                                                    at.setValue(w.getDirection().getValue());
                                                    at.setUom(w.getDirection().getUom());
                                                }));
                                            }
                                        }))));
                    }
                })));
    }

    private OMObservationPropertyType createCancelAnalysis(final AIRMET input, final String issueTime, final String airmetUUID) {
        return create(OMObservationPropertyType.class, omObsType -> omObsType.setOMObservation(create(OMObservationType.class, omObs -> {
            omObs.setId("cnl-analysis");
            omObs.setType(create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));
            omObs.setPhenomenonTime(
                    create(TimeObjectPropertyType.class, toProp -> toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE)));
            omObs.setResultTime(create(TimeInstantPropertyType.class, tip -> tip.setHref("#" + "resltt-" + airmetUUID)));

            omObs.setProcedure(create(OMProcessPropertyType.class, omppt -> omppt.setAny(createAndWrap(ProcessType.class, process -> {
                process.setId("p-49-2-airmet-" + airmetUUID);
                process.setDescription(create(StringOrRefType.class, descr -> descr.setValue(AviationCodeListUser.CODELIST_VALUE_AIRMET_PROCESS)));
            }))));

            omObs.setObservedProperty(
                    create(ReferenceType.class, ref -> ref.setHref(AviationCodeListUser.CODELIST_AIRMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS)));

            final FeaturePropertyType ftp = create(FeaturePropertyType.class,
                    prop -> prop.setAbstractFeature(createAndWrap(SFSpatialSamplingFeatureType.class, samsFeature -> {
                        samsFeature.setId("sampling-surface-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID());
                        samsFeature.setType(create(ReferenceType.class, ref -> {
                            ref.setHref(AviationCodeListUser.CODELIST_VALUE_PREFIX_OM_SAMPLING + "SF_SamplingSurface");
                            ref.setTitle("Sampling surface");
                        }));

                        samsFeature.getSampledFeature().add(create(FeaturePropertyType.class, samProp -> {
                            final AirspaceType airspace = create(AirspaceType.class);
                            airspace.setValidTime(null);
                            airspace.setId("fir-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID());
                            airspace.getTimeSlice()
                                    .add(create(AirspaceTimeSlicePropertyType.class,
                                            timeSliceProp -> timeSliceProp.setAirspaceTimeSlice(create(AirspaceTimeSliceType.class, timeSlice -> {
                                                timeSlice.setValidTime(create(TimePrimitivePropertyType.class));
                                                timeSlice.setInterpretation("SNAPSHOT");
                                                timeSlice.setType(create(CodeAirspaceType.class, type -> type.setValue("FIR")));
                                                timeSlice.setAirspaceName(
                                                        create(TextNameType.class, name -> name.setValue(input.getIssuingAirTrafficServicesUnit().getName())));
                                                timeSlice.setId(
                                                        "fir-" + input.getIssuingAirTrafficServicesUnit().getDesignator() + "-" + UUID.randomUUID() + "-ts");
                                                timeSlice.setDesignator(create(CodeAirspaceDesignatorType.class,
                                                        desig -> desig.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator())));

                                            }))));
                            samProp.setAbstractFeature(wrap(airspace, AirspaceType.class));
                        }));
                        samsFeature.setShape(create(ShapeType.class, shp -> shp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_WITHHELD)));
                    })));
            omObs.setFeatureOfInterest(ftp);
            final AIRMETEvolvingConditionCollectionPropertyType _seccpt = create(AIRMETEvolvingConditionCollectionPropertyType.class,
                    eccpt -> eccpt.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE));
            omObs.setResult(_seccpt);
        })));
    }

    protected void updateMessageMetadata(final AIRMET source, final ConversionResult<?> results, final AIRMETType target) throws ConversionException {
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
                if (source.getTranslatedBulletinID().isPresent()) {
                    target.setTranslatedBulletinID(source.getTranslatedBulletinID().get());
                }
                source.getTranslatedBulletinReceptionTime()
                        .map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time)))
                        .ifPresent(target::setTranslatedBulletinReceptionTime);
                source.getTranslationCentreDesignator().ifPresent(target::setTranslationCentreDesignator);
                source.getTranslationCentreName().ifPresent(target::setTranslationCentreName);
                source.getTranslationTime().map(time -> f.newXMLGregorianCalendar(toIWXXMDateTime(time))).ifPresent(target::setTranslationTime);
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
        final InputStream retval = this.getClass().getResourceAsStream("AIRMETCleanup.xsl");
        if (retval == null) {
            throw new ConversionException("Error accessing cleanup XSLT sheet file");
        }
        return retval;
    }

    public static class ToDOM extends AIRMETIWXXMSerializer<Document> {
        @Override
        protected Document render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException {
            return this.renderXMLDocument(airmet, hints);
        }

        @Override
        protected IssueList validate(final Document output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return AIRMETIWXXMSerializer.validateDOMAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }

    public static class ToString extends AIRMETIWXXMSerializer<String> {

        @Override
        protected String render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException {
            final Document result = renderXMLDocument(airmet, hints);
            return renderDOMToString(result, hints);
        }

        @Override
        protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
            return AIRMETIWXXMSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
        }
    }
}
