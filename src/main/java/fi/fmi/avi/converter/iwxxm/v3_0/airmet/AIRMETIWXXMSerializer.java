package fi.fmi.avi.converter.iwxxm.v3_0.airmet;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationCodeListUser.WeatherCausingVisibilityReduction;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.PhenomenonGeometryWithHeight;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import icao.iwxxm30.AIRMETEvolvingConditionCollectionPropertyType;
import icao.iwxxm30.AIRMETEvolvingConditionCollectionType;
import icao.iwxxm30.AIRMETEvolvingConditionPropertyType;
import icao.iwxxm30.AIRMETEvolvingConditionType;
import icao.iwxxm30.AIRMETExpectedIntensityChangeType;
import icao.iwxxm30.AIRMETType;
import icao.iwxxm30.AbstractTimeObjectPropertyType;
import icao.iwxxm30.AeronauticalAreaWeatherPhenomenonType;
import icao.iwxxm30.AirspacePropertyType;
import icao.iwxxm30.AirspaceVolumePropertyType;
import icao.iwxxm30.AngleWithNilReasonType;
import icao.iwxxm30.PermissibleUsageReasonType;
import icao.iwxxm30.PermissibleUsageType;
import icao.iwxxm30.ReportStatusType;
import icao.iwxxm30.StringWithNilReasonType;
import icao.iwxxm30.TimeIndicatorType;
import icao.iwxxm30.UnitPropertyType;
import icao.iwxxm30.WeatherCausingVisibilityReductionType;
import net.opengis.gml32.AbstractTimeObjectType;
import net.opengis.gml32.AngleType;
import net.opengis.gml32.LengthType;
import net.opengis.gml32.SpeedType;
import net.opengis.gml32.TimeInstantPropertyType;
import net.opengis.gml32.TimeInstantType;
import net.opengis.gml32.TimePeriodPropertyType;
import net.opengis.gml32.TimePeriodType;
import net.opengis.gml32.TimePositionType;
import net.opengis.gml32.TimePrimitivePropertyType;

public abstract class AIRMETIWXXMSerializer<T> extends AbstractIWXXM30Serializer<AIRMET, T> {
    private static final Logger LOG = LoggerFactory.getLogger(AIRMETIWXXMSerializer.class);

    protected static TimePeriodPropertyType getTimePeriodPropertyType(final AIRMET input) {
        return getATimePeriodPropertyType(input.getValidityPeriod());
    }

    protected static Optional<TimePeriodPropertyType> getCancelledTimePeriodPropertyType(final AIRMET input) {
        return input.getCancelledReference().map(airmetReference -> getATimePeriodPropertyType(airmetReference.getValidityPeriod()));
    }

    protected static TimePeriodPropertyType getATimePeriodPropertyType(final PartialOrCompleteTimePeriod valTime) {
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

    protected abstract T render(final AIRMETType airmet, final ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

    /**
     * Converts an AIRMET object into another format.
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

        LOG.info("Starting conversion of AIRMET");
        if (!input.areAllTimeReferencesComplete()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "All time references must be completed before converting to IWXXM"));
            return result;
        }

        final String airmetUuid = getUUID();

        final AIRMETType airmet;

        airmet = create(AIRMETType.class);
        airmet.setId(getUUID());
        if (input.getCancelledReference().isPresent()) {
            airmet.setReportStatus(ReportStatusType.NORMAL);
            airmet.setIsCancelReport(true);
            airmet.setCancelledReportSequenceNumber(input.getCancelledReference().get().getSequenceNumber());
            getCancelledTimePeriodPropertyType(input)
                    .ifPresent(airmet::setCancelledReportValidPeriod);
        } else {
            airmet.setReportStatus(ReportStatusType.NORMAL);
            airmet.setIsCancelReport(false);
            airmet.setPhenomenon(create(AeronauticalAreaWeatherPhenomenonType.class, phen -> phen.setHref(
                    AviationCodeListUser.CODELIST_AIRMET_PHENOMENA_ROOT + input.getAirmetPhenomenon().get().name())));
        }
        //Use current time as issueTime if missing
        final String issueTime = input.getIssueTime().<String> flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                .orElseGet(() -> toIWXXMDateTime(ZonedDateTime.now()));

        airmet.setIssueTime(
            create(TimeInstantPropertyType.class, tip -> tip.setTimeInstant(create(TimeInstantType.class, ti -> {
                ti.setTimePosition(create(TimePositionType.class, tp -> tp.getValue().add(issueTime)));
                ti.setId(getUUID());
            }))));

        airmet.setIssuingAirTrafficServicesUnit(create(UnitPropertyType.class, prop -> prop.setUnit(create(UnitType.class, unit -> {
            unit.setId(getUUID());
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp -> sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                slice.setId(getUUID());
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

        airmet.setOriginatingMeteorologicalWatchOffice(
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
                                    tnt.setValue(input.getMeteorologicalWatchOffice().getName());
                                }));

                                slice.setDesignator(create(CodeOrganisationDesignatorType.class, desig ->
                                    desig.setValue(input.getMeteorologicalWatchOffice().getDesignator())));
                            }))));
                }))));

airmet.setIssuingAirTrafficServicesRegion(
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

        airmet.setSequenceNumber(
                    create(StringWithNilReasonType.class, prop -> prop.setValue(input.getSequenceNumber())));

        airmet.setValidPeriod(getTimePeriodPropertyType(input));

        if (!input.getCancelledReference().isPresent()) {
            final String analysisTime = input.getAnalysisGeometries()//
                    .map(AbstractIWXXM30Serializer::getFirstOrNull)//
                    .flatMap(PhenomenonGeometryWithHeight::getTime)//
                    .<String>flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .orElse(null);

            List<PhenomenonGeometryWithHeight> ans = input.getAnalysisGeometries().get();

            AIRMETEvolvingConditionCollectionPropertyType aeccpt = createAnalysis(input, ans, analysisTime,
                        input.getIssuingAirTrafficServicesUnit().getDesignator(),
                        input.getIssuingAirTrafficServicesUnit().getName(), issueTime, airmetUuid);
            airmet.setAnalysis(aeccpt);

        }

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
    private AIRMETEvolvingConditionCollectionPropertyType createAnalysis(AIRMET input,
            final List<PhenomenonGeometryWithHeight> ans, final String analysisTime, final String designator,
            final String airspaceName, final String issueTime, final String airmetUUID) {

        AbstractTimeObjectPropertyType phenTimeProp;
        if (analysisTime!=null) {
            phenTimeProp= create(AbstractTimeObjectPropertyType.class, toProp -> {
            final JAXBElement<?> wrapped = createAndWrap(TimeInstantType.class, inst -> {
                inst.setId(getUUID());
                ZonedDateTime validityStart  = input.getValidityPeriod().getStartTime().get().getCompleteTime().get();
                ZonedDateTime phenTime = ZonedDateTime.parse(analysisTime, DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC));
                if (phenTime.isEqual(validityStart)) {
                    inst.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(analysisTime)));
                } else {
                    String phenomenonTimeString = validityStart.format(DateTimeFormatter.ISO_INSTANT);
                    inst.setTimePosition(create(TimePositionType.class, tPos -> tPos.getValue().add(phenomenonTimeString)));
                }
            });
            toProp.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) wrapped);
        });
        } else {
            phenTimeProp= create(AbstractTimeObjectPropertyType.class, toProp -> toProp.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING));
        }
        AIRMETEvolvingConditionCollectionType aecct = create(
            AIRMETEvolvingConditionCollectionType.class, ecct -> {
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
                    ecct.getMember().add(create(AIRMETEvolvingConditionPropertyType.class, seccpt -> {
                        seccpt.setAIRMETEvolvingCondition(create(AIRMETEvolvingConditionType.class, sect -> {
                            if (an.getIntensityChange().isPresent()) {
                                switch (an.getIntensityChange().get()) {
                                    case INTENSIFYING:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.INTENSIFY);
                                        break;
                                    case WEAKENING:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.WEAKEN);
                                        break;
                                    case NO_CHANGE:
                                        sect.setIntensityChange(AIRMETExpectedIntensityChangeType.NO_CHANGE);
                                        break;
                                }
                            } else {
                                sect.setIntensityChange(AIRMETExpectedIntensityChangeType.NO_CHANGE);
                            }
                            sect.setId(getUUID());
                            sect.setGeometry(create(AirspaceVolumePropertyType.class, avpt -> {
                                avpt.setAirspaceVolume(createAirspaceVolume(an));
                            }));
                            if (input.getAirmetPhenomenon().get().name().equals("SFC_VIS")) {
                                sect.setSurfaceVisibility(create(LengthType.class, l -> {
                                    l.setUom(toIwxxmLevelUom(input.getVisibility().get()));
                                    l.setValue(input.getVisibility().get().getValue());
                                }));
                                for (WeatherCausingVisibilityReduction obsc: input.getObscuration().get()) {
                                    sect.getSurfaceVisibilityCause().add(create(WeatherCausingVisibilityReductionType.class, w -> {
                                        w.setHref(AviationCodeListUser.CODELIST_VALUE_WEATHERCAUSINGVISIBILITYREDUCTION+"/"+obsc.name());
                                    }));
                                }
                            }
                            if (input.getAirmetPhenomenon().get().name().equals("BKN_CLD")||
                                input.getAirmetPhenomenon().get().name().equals("OVC_CLD")) {
                                    sect.setCloudBase(create(LengthType.class, b -> {
                                        b.setUom(toIwxxmLevelUom(input.getCloudLevels().get().getCloudBase()));
                                        b.setValue(input.getCloudLevels().get().getCloudBase().getValue());
                                    }));
                                    if (input.getCloudLevels().get().getCloudBase().getValue()==0) {
                                        sect.setCloudBaseReference("SFC");
                                    } else {
                                        sect.setCloudBaseReference("STD");
                                    }
                                    sect.setCloudTop(create(LengthType.class, b -> {
                                        b.setUom(toIwxxmLevelUom(input.getCloudLevels().get().getCloudTop()));
                                        b.setValue(input.getCloudLevels().get().getCloudTop().getValue());
                                    }));
                                    sect.setCloudTopReference("STD");
                            }
                            if (input.getAirmetPhenomenon().get().name().equals("SFC_WIND")) {
                                sect.setSurfaceWindDirection(create(AngleType.class, a ->{
                                    a.setUom(toIwxxmLevelUom(input.getWind().get().getDirection()));
                                    a.setValue(input.getWind().get().getDirection().getValue());
                                }));
                                sect.setSurfaceWindSpeed(create(SpeedType.class, a ->{
                                    a.setUom(toIwxxmLevelUom(input.getWind().get().getSpeed()));
                                    a.setValue(input.getWind().get().getSpeed().getValue());
                                }));

                            }

                            final icao.iwxxm30.ObjectFactory of_iwxxm30 = new icao.iwxxm30.ObjectFactory();
                            if (an.getMovingDirection().isPresent()) {
                                final AngleWithNilReasonType angl = new AngleWithNilReasonType();
                                final NumericMeasure md = an.getMovingDirection().get();
                                angl.setUom(md.getUom());
                                angl.setValue(md.getValue());
                                final JAXBElement<AngleWithNilReasonType> directionOfMotion = of_iwxxm30
                                        .createAIRMETEvolvingConditionTypeDirectionOfMotion(angl);
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
                        }));
                    }));
                }
            });

        return create(AIRMETEvolvingConditionCollectionPropertyType.class, aeccpt -> aeccpt.setAIRMETEvolvingConditionCollection(aecct));
    }

    private String toIwxxmLevelUom(NumericMeasure level) {
        String uom = level.getUom();
        String iwxxmUom = uom; //Fallback to whatever is already there
        switch (uom) {
            case "FT":
              iwxxmUom = "[ft_i]";
              break;
            case "M":
              iwxxmUom = "m";
              break;
            case "KT":
              iwxxmUom = "[kn_i]";
              break;
            case "deg":
              iwxxmUom = "deg";
              break;
        }
        return iwxxmUom;
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
