package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import aero.aixm511.*;
import aero.aixm511.SurfacePropertyType;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511WxSerializer;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.amd79.*;
import icao.iwxxm30.*;
import icao.iwxxm30.AirspaceVolumePropertyType;
import icao.iwxxm30.UnitPropertyType;
import net.opengis.gml32.*;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public abstract class SpaceWeatherIWXXMSerializer<T> extends AbstractIWXXM30Serializer<SpaceWeatherAdvisoryAmd79, T> {
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;
    private static final net.opengis.gml32.ObjectFactory GML_OF = new net.opengis.gml32.ObjectFactory();

    protected abstract T render(SpaceWeatherAdvisoryType swx, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisoryAmd79 input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        final SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        swxType.setId(UUID_PREFIX + UUID.randomUUID());

        if (input.getIssueTime().isPresent()) {
            swxType.setIssueTime(create(TimeInstantPropertyType.class, prop -> getIssueTime(prop, input.getIssueTime().get())));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
            return result;
        }

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, prop -> getIssuingCenter(prop, input.getIssuingCenter())));

        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, prop -> prop.setValue(input.getAdvisoryNumber().asAdvisoryNumber())));

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

        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, prop -> getNextAdvisory(prop, input.getNextAdvisory())));

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

    protected void updateMessageMetadata(final SpaceWeatherAdvisoryAmd79 source, final ConversionResult<?> results, final SpaceWeatherAdvisoryType target)
            throws ConversionException {
        try {
            final DatatypeFactory f = DatatypeFactory.newInstance();

            switch (source.getReportStatus()) {
                case AMENDMENT:
                    target.setReportStatus(ReportStatusType.AMENDMENT);
                    break;
                case CORRECTION:
                    target.setReportStatus(ReportStatusType.CORRECTION);
                    break;
                case NORMAL:
                    target.setReportStatus(ReportStatusType.NORMAL);
                    break;
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
                if (results.getStatus() != ConversionResult.Status.SUCCESS) {
                    source.getTranslatedTAC().ifPresent(target::setTranslationFailedTAC);
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

        analysisType.setId(UUID_PREFIX + UUID.randomUUID());
        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, prop -> getAnalysisTime(prop, analysis.getTime())));

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
                    } else {
                        locationType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                    }
                    regionType.setLocationIndicator(locationType);

                    if (region.getAirSpaceVolume().isPresent()) {
                        regionType.setGeographicLocation(
                                create(AirspaceVolumePropertyType.class, prop -> getAirspaceVolumeProperty(prop, region.getAirSpaceVolume().get())));
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
        timeInstantType.setId(UUID_PREFIX + UUID.randomUUID());
        final TimePositionType timePositionType = create(TimePositionType.class);
        toIWXXMDateTime(time).ifPresent(t -> timePositionType.getValue().add(t));
        timeInstantType.setTimePosition(timePositionType);
        @SuppressWarnings({"unchecked", "rawtypes"}) final JAXBElement<AbstractTimeObjectType> jaxbTimeInstant = (JAXBElement) GML_OF.createTimeInstant(timeInstantType);
        prop.setAbstractTimeObject(jaxbTimeInstant);
    }

    private void getIssuingCenter(final UnitPropertyType prop, final IssuingCenter issuingCenter) {
        final UnitType unitType = create(UnitType.class);
        unitType.setId(UUID_PREFIX + UUID.randomUUID());
        final UnitTimeSlicePropertyType unitTimeSlicePropertyType = create(UnitTimeSlicePropertyType.class);
        final UnitTimeSliceType unitTimeSliceType = create(UnitTimeSliceType.class);
        unitTimeSliceType.setId(UUID_PREFIX + UUID.randomUUID());
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
        toIWXXMDateTime(time).ifPresent(t -> tp.getValue().add(t));
        ti.setTimePosition(tp);
        ti.setId(UUID_PREFIX + UUID.randomUUID());
        prop.setTimeInstant(ti);
    }

    private void getAirspaceVolumeProperty(final AirspaceVolumePropertyType prop, final AirspaceVolume volume) {
        final AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);
        if (volume.getHorizontalProjection().isPresent()) {
            airspaceVolumeType.setId(UUID_PREFIX + UUID.randomUUID());
            final SurfacePropertyType surfaceProperty = createSurface(volume.getHorizontalProjection().get(), UUID_PREFIX + UUID.randomUUID());
            airspaceVolumeType.setHorizontalProjection(surfaceProperty);
        }

        volume.getLowerLimit().ifPresent(limit -> {
            // TODO: Once xsi:nil can be set, setLowerLimit can be moved outside presence test
            airspaceVolumeType.setLowerLimit(toValDistanceVertical(limit).orElseGet(AbstractIWXXMAixm511WxSerializer::nilValDistanceVertical));
            airspaceVolumeType.setLowerLimitReference(create(CodeVerticalReferenceType.class,
                    codeVerticalReferenceType -> volume.getLowerLimitReference().ifPresent(codeVerticalReferenceType::setValue)));
        });
        volume.getUpperLimit().ifPresent(limit -> {
            // TODO: Once xsi:nil can be set, setLowerLimit can be moved outside presence test
            airspaceVolumeType.setUpperLimit(toValDistanceVertical(limit).orElseGet(AbstractIWXXMAixm511WxSerializer::nilValDistanceVertical));
            airspaceVolumeType.setUpperLimitReference(create(CodeVerticalReferenceType.class,
                    codeVerticalReferenceType -> volume.getUpperLimitReference().ifPresent(codeVerticalReferenceType::setValue)));
        });

        prop.setAirspaceVolume(airspaceVolumeType);
    }

    private void getNextAdvisory(final TimeInstantPropertyType prop, final NextAdvisory nextAdvisory) {
        if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_AT || nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
            final TimeInstantType timeInstant = create(TimeInstantType.class);
            final TimePositionType timePosition = create(TimePositionType.class);
            if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
                timePosition.setIndeterminatePosition(TimeIndeterminateValueType.BEFORE);
            }
            // TODO: 'after' not supported in model; temporarily omit
            nextAdvisory.getTime()//
                    .flatMap(AbstractIWXXMSerializer::toIWXXMDateTime)//
                    .ifPresent(t -> timePosition.getValue().add(t));
            timeInstant.setId(UUID_PREFIX + UUID.randomUUID());
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
