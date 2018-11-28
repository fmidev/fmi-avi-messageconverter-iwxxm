package fi.fmi.avi.converter.iwxxm.sigmet;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import net.opengis.gml32.CoordinatesType;
import net.opengis.gml32.CurvePropertyType;
import net.opengis.gml32.CurveSegmentArrayPropertyType;
import net.opengis.gml32.CurveType;
import net.opengis.gml32.DirectPositionListType;
import net.opengis.gml32.DirectPositionType;
import net.opengis.gml32.DirectionPropertyType;
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.Debug;
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
import aero.aixm511.ValDistanceType;
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
import fi.fmi.avi.model.VolcanoDescription;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysis;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.VASIGMET;
import icao.iwxxm21.AeronauticalSignificantWeatherPhenomenonType;
import icao.iwxxm21.AirspacePropertyType;
import icao.iwxxm21.AngleWithNilReasonType;
import icao.iwxxm21.ExpectedIntensityChangeType;
import icao.iwxxm21.PermissibleUsageReasonType;
import icao.iwxxm21.PermissibleUsageType;
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

public abstract class AbstractSIGMETIWXXMSerializer<T> extends AbstractIWXXMSerializer implements AviMessageSpecificConverter<SIGMET, T> {
    protected abstract T render(final SIGMETType sigmet, final ConversionHints hints) throws ConversionException;

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
        System.err.println("SIGMET convertMessage(" + input.getSigmetPhenomenon() + ")");

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
        final String issueTime=input.getIssueTime().getCompleteTime().orElse(ZonedDateTime.now()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

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
                            tnt.setValue(input.getIssuingAirTrafficServicesUnit().getDesignator() + " FIC");
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

        System.err.println("seq:" + input.getSequenceNumber() + " " + sigmet.getStatus());
        sigmet.setSequenceNumber(input.getSequenceNumber());

        sigmet.setValidPeriod(getTimePeriodPropertyType(input, sigmetUuid));

        if (sigmet.getStatus().equals(SIGMETReportStatusType.CANCELLATION)) {
            sigmet.setCancelledSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            sigmet.setCancelledValidPeriod(getTimePeriodPropertyType(input, "cnl-tp-" + getTimePeriodId(input)));
            sigmet.setPhenomenon(create(AeronauticalSignificantWeatherPhenomenonType.class, (phen) -> {
                phen.getNilReason().add("inapplicable");
            }));

            sigmet.setAnalysis(createCancelAnalysis(input, issueTime, sigmetUuid));
            if ((input.getSigmetPhenomenon()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
                if (((VASIGMET)input).getVolcanicAshMovedToFIR().isPresent()) {
                    String designator = input.getIssuingAirTrafficServicesUnit().getDesignator();
                    String airSpaceName = input.getIssuingAirTrafficServicesUnit().getName();
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

            System.err.println("Creating analysis");
            input.getAnalysis().ifPresent((l) -> {
                sigmet.setAnalysis(createAnalysis(l, input.getIssuingAirTrafficServicesUnit().getDesignator(),
                        input.getIssuingAirTrafficServicesUnit().getName(), issueTime, sigmetUuid));
                if (l.get(0).getForecastTime().isPresent()) {
                    Debug.println("Creating FPA");
                    sigmet.setForecastPositionAnalysis(
                            createForecastPositionAnalysis(l, input.getIssuingAirTrafficServicesUnit().getDesignator(), issueTime,
                                    sigmetUuid));
                }
            });

        }
        if ((input.getSigmetPhenomenon()).equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
            VolcanoDescription volcano=((VASIGMET)input).getVolcano();
            icao.iwxxm21.ObjectFactory of=new icao.iwxxm21.ObjectFactory();
            ((VolcanicAshSIGMETType)sigmet).getRest().add(
                    of.createVolcanicAshSIGMETTypeEruptingVolcano(create(VolcanoPropertyType.class, (vpt)->{
                        vpt.setVolcano(createAndWrap(VolcanoType.class, (v)-> {
                            ((VASIGMET) input).getVolcano().getVolcanoName().ifPresent((vn)->{
                                v.setVolcanoName(vn);
                            });
                            //                   v.setVolcanoName(((VASIGMET) input).getVolcano().getVolcanoName().get());
                            Double[] pts=((VASIGMET) input).getVolcano().getVolcanoPosition().getCoordinates();
                            v.setPosition(create(PointPropertyType.class, (ppt) -> {
                                ppt.setPoint(create(PointType.class, (pt)->{
                                    pt.setPos(create(DirectPositionType.class, (dpt) -> {
                                        dpt.getValue().addAll(Arrays.asList(pts));
                                        dpt.setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                                        dpt.setSrsDimension(BigInteger.valueOf(2));
                                        dpt.getAxisLabels().add("Lat Lon");
                                        dpt.getUomLabels().add("deg deg");
                                    }));
                                    pt.setId("wv-pt-"+sigmetUuid);
                                }));
                            }));
                            v.setId("wv-"+((VASIGMET) input).getVolcano().getVolcanoName().get()+"-"+sigmetUuid);
                        }));
                    }))
            );
        }


        try {
            result.setStatus(Status.SUCCESS);
            this.updateMessageMetadata(input, result, sigmet);
            ConverterValidationEventHandler eventHandler = new ConverterValidationEventHandler(result);
            if (input.getSigmetPhenomenon().equals(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA)) {
                this.validateDocument(((VolcanicAshSIGMETType)sigmet), VolcanicAshSIGMETType.class, hints, eventHandler); //TODO true is for debugging: shows
                // results even in
                // case of failure
            } else {
                this.validateDocument(sigmet, SIGMETType.class, hints, eventHandler); //TODO true is for debugging: shows results even in case of failure
            }

            if (eventHandler.errorsFound()) {
                result.setStatus(Status.FAIL);
                System.err.println("FAILED in eventhandler");
                for (ConversionIssue iss: eventHandler.getResult().getConversionIssues()) {
                    System.err.println("ISS: "+iss.getMessage());

                }
            } else {
                result.setConvertedMessage(this.render(sigmet, hints));
                System.err.println("result set");
            }
        } catch (ConversionException e) {
            System.err.println("Error in conversion ");
            result.setStatus(Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message to String", e));
        }
        System.err.println("SM: " + sigmet.toString());

        return result;
    }

    @SuppressWarnings("unchecked")
    private OMObservationPropertyType createAnalysis(List<SigmetAnalysis> inputs, String designator, String airspaceName, String issueTime,
            String sigmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("analysis-" + sigmetUUID);
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));
                SigmetAnalysis firstInput=inputs.get(0);

                        //TODO Only if FCST or OBS time are given
                        if ((firstInput.getAnalysisType() == SigmetAnalysisType.UNKNOWN) || !firstInput.getAnalysisTime().isPresent()) {
                            //set Phen time to nil with nilReason of "missing"
                            omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                                toProp.getNilReason().add("missing");
                            }));
                        } else {
                            omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                                JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, (period) -> {
                                    period.setId("phent-" + sigmetUUID);
                                    period.setTimePosition(create(TimePositionType.class, (tPos) -> {
                                        tPos.getValue().add(firstInput.getAnalysisTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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

                        omObs.setFeatureOfInterest(getFeatureOfInterest(firstInput, designator, airspaceName, "sampling-surface-" + sigmetUUID));

                        SIGMETEvolvingConditionCollectionPropertyType _seccpt = getResult(inputs, sigmetUUID);
                        omObs.setResult(_seccpt);
            }));

        });
        return analysis;
    }

    private FeaturePropertyType getFeatureOfInterest(SigmetAnalysis input, String designator, String airSpaceName, String sfSpatialUUID) {
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
                    shp.getNilReason().add("withheld");
                }));
            }));
        });
        return ftp;
        //return null;
    }

    private SIGMETEvolvingConditionCollectionPropertyType getResult(List<SigmetAnalysis> inputs, String sigmetUUID) {
        ObjectFactory of = new ObjectFactory();
        SIGMETEvolvingConditionCollectionPropertyType _seccpt = create(SIGMETEvolvingConditionCollectionPropertyType.class, (seccpt) -> {
            seccpt.setSIGMETEvolvingConditionCollection(create(SIGMETEvolvingConditionCollectionType.class, (secct) -> {
                secct.setId("fcst-" + sigmetUUID);
                SigmetAnalysis firstInput=inputs.get(0);
                secct.setTimeIndicator(TimeIndicatorType.OBSERVATION);
                if (firstInput.getAnalysisType() == SigmetAnalysisType.FORECAST) {
                    secct.setTimeIndicator(TimeIndicatorType.FORECAST);
                }
                int cnt=0;
                for (SigmetAnalysis input: inputs) {
                    secct.getMember().add(create(SIGMETEvolvingConditionPropertyType.class, (secpt) -> {
                        secpt.setSIGMETEvolvingCondition(create(SIGMETEvolvingConditionType.class, (sect) -> {
                            sect.setId("sec-" + cnt + "-" + sigmetUUID);

                            sect.setApproximateLocation(input.getAnalysisApproximateLocation().orElse(false));
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
                            input.getMovingDirection().ifPresent((md) -> {
                                icao.iwxxm21.ObjectFactory of_iwxxm21 = new icao.iwxxm21.ObjectFactory();
                                AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                angl.setUom(md.getUom());
                                angl.setValue(md.getValue());

                                JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm21.createSIGMETEvolvingConditionTypeDirectionOfMotion(angl);
                                sect.setDirectionOfMotion(directionOfMotion);
                            });
                            input.getMovingSpeed().ifPresent((ms) -> {
                                sect.setSpeedOfMotion(create(SpeedType.class, (spd) -> {
                                    spd.setUom(ms.getUom());
                                    spd.setValue(ms.getValue());
                                }));
                            });

                            sect.setGeometry(create(AirspaceVolumePropertyType.class, (avpt) -> {
                                avpt.setAirspaceVolume(create(AirspaceVolumeType.class, (avt) -> {
                                    avt.setId("as-" + cnt + "-" + sigmetUUID);
                                    input.getUpperLimit().ifPresent((l) -> {
                                        avt.setUpperLimit(create(ValDistanceVerticalType.class, (vdvt) -> {
                                            vdvt.setUom(l.getUom());
                                            vdvt.setValue(l.getValue().toString());
                                        }));
                                    });
                                    avt.setUpperLimitReference(create(CodeVerticalReferenceType.class, (cvrt) -> {
                                        cvrt.setValue("STD");//TODO extract correct value from input
                                    }));
                                    input.getLowerLimit().ifPresent((l) -> {
                                        avt.setLowerLimit(create(ValDistanceVerticalType.class, (vdvt) -> {
                                            vdvt.setUom(l.getUom());
                                            vdvt.setValue(l.getValue().toString());
                                        }));
                                        avt.setLowerLimitReference(create(CodeVerticalReferenceType.class, (cvrt) -> {
                                            cvrt.setValue("STD");//TODO extract correct value from input
                                        }));
                                    });
                                    avt.setHorizontalProjection(create(SurfacePropertyType.class, (spt) -> {
                                        spt.setSurface(createAndWrap(SurfaceType.class, (sft) -> {
                                            try {
                                                Geometry geom = input.getAnalysisGeometry();
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
                                                                            curvet.setId("curve-analysis-" + cnt + "-" + sigmetUUID);
                                                                            curvet.setSegments(create(CurveSegmentArrayPropertyType.class, (curvesat) -> {
                                                                                curvesat.getAbstractCurveSegment()
                                                                                        .add(createAndWrap(CircleByCenterPointType.class, (cbcpt) -> {
/*
                                                                                        cbcpt.setCoordinates(create(CoordinatesType.class, (coordt) -> {
                                                                                            coordt.setValue("5 52.0");
                                                                                        }));
*/
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
                        }));
                    }));
                }
            }));
        });

        return _seccpt;
    }

    @SuppressWarnings("unchecked")
    private static OMObservationPropertyType createForecastPositionAnalysis(List<SigmetAnalysis> inputs, String designator, String issueTime,
            String sigmetUUID) {
        OMObservationPropertyType forecastPositionAnalysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("forecastPositionAnalysis-" + sigmetUUID);
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_POSITION_COLLECTION_ANALYSIS);
                }));
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                    JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, (period) -> {
                        SigmetAnalysis firstInput=inputs.get(0);
                        period.setId("time-"  + UUID.randomUUID().toString());
                        period.setTimePosition(create(TimePositionType.class, (tPos) -> {
                            Object o=tPos.getValue();
                            tPos.getValue().add(firstInput.getForecastTime().get().getCompleteTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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

                int cnt=0;
                for (SigmetAnalysis input: inputs) {
                    Debug.println("About to setResult for FPA");
                    omObs.setResult(createFPAResult(input, designator, cnt, sigmetUUID));
                    cnt++;
                }
            }));
        });
        return forecastPositionAnalysis;
    }

    private static SIGMETPositionCollectionPropertyType createFPAResult(SigmetAnalysis input, String designator, int cnt, String sigmetUUID) {
        ObjectFactory of = new ObjectFactory();
        SIGMETPositionCollectionPropertyType _spcpt = create(SIGMETPositionCollectionPropertyType.class, (spcpt) -> {
            spcpt.setSIGMETPositionCollection(create(SIGMETPositionCollectionType.class, (spct) -> {
                spct.setId("spc-" + designator + "-" + cnt + "-" + sigmetUUID);
                spct.getMember().add(create(SIGMETPositionPropertyType.class, (sppt) -> {
                    sppt.setSIGMETPosition(create(SIGMETPositionType.class, (spot) -> {
                        spot.setId("fpa-pos-" + cnt + "-" + sigmetUUID);
                        spot.setApproximateLocation(input.getForecastApproximateLocation().orElse(false));
                        spot.setGeometry(create(AirspaceVolumePropertyType.class, (avpt) -> {
                            avpt.setAirspaceVolume(create(AirspaceVolumeType.class, (avt) -> {
                                avt.setId("fpa-" + cnt + "-" + sigmetUUID);
                                avt.setHorizontalProjection(create(SurfacePropertyType.class, (spt) -> {
                                    spt.setSurface(createAndWrap(SurfaceType.class, (sft) -> {
                                        try {
                                            Geometry geom = input.getForecastGeometry().get();
                                            System.err.println("GEOM: " + geom.getGeometryType());
                                            if ("Point".equals(geom.getGeometryType())) {
                                                List<Double> pts = new ArrayList<>();
                                                for (Coordinate coord : geom.getCoordinates()) {
                                                    pts.add(coord.y);
                                                    pts.add(coord.x);
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
                                                for (Coordinate coord : geom.getCoordinates()) {
                                                    pts.add(coord.y);
                                                    pts.add(coord.x);
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

    private OMObservationPropertyType createCancelAnalysis(SIGMET input, String issueTime, String sigmetUUID) {
        OMObservationPropertyType analysis = create(OMObservationPropertyType.class, (omObsType) -> {
            omObsType.setOMObservation(create(OMObservationType.class, (omObs) -> {
                omObs.setId("cnl-analysis");
                omObs.setType(create(ReferenceType.class, (ref) -> {
                    ref.setHref(AviationCodeListUser.CODELIST_SIGMET_EVOLVING_CONDITION_COLLECTION_ANALYSIS);
                }));
                omObs.setPhenomenonTime(create(TimeObjectPropertyType.class, (toProp) -> {
                    toProp.getNilReason().add("inapplicable");
                }));
                omObs.setResultTime(create(TimeInstantPropertyType.class, (tip) -> {
                    tip.setHref("#" + "resltt-"+sigmetUUID);
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
                            shp.getNilReason().add("withheld");
                        }));
                    }));
                });
                omObs.setFeatureOfInterest(ftp);
                SIGMETEvolvingConditionCollectionPropertyType _seccpt = create(SIGMETEvolvingConditionCollectionPropertyType.class, (eccpt) -> {
                    eccpt.getNilReason().add("inapplicable");
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
            target.setPermissibleUsageReason(PermissibleUsageReasonType.EXERCISE);
            source.getPermissibleUsage().ifPresent((us) -> {
                target.setPermissibleUsage(PermissibleUsageType.valueOf(us.name()));
                source.getPermissibleUsageReason().ifPresent((usr) -> {
                    target.setPermissibleUsageReason(PermissibleUsageReasonType.valueOf(usr.name()));
                });
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

    @Override
    protected Source getCleanupTransformationStylesheet(ConversionHints hints) throws ConversionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(this.getClass().getResourceAsStream("SIGMETCleanup.xsl"));
            return new DOMSource(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException("Unexpected problem in reading the cleanup XSL sheet", e);
        }

    }

}
