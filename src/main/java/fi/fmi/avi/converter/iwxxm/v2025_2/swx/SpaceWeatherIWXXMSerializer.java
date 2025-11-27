package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import aero.aixm511full.*;
import aero.aixm511full.SurfacePropertyType;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullSerializer;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v2025_2.AbstractIWXXM20252Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.amd82.*;
import icao.iwxxm2025_2.*;
import icao.iwxxm2025_2.AirspaceVolumePropertyType;
import icao.iwxxm2025_2.UnitPropertyType;
import net.opengis.gml32.*;
import net.opengis.gml32.ObjectFactory;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public abstract class SpaceWeatherIWXXMSerializer<T> extends AbstractIWXXM20252Serializer<SpaceWeatherAdvisoryAmd82, T> {
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;
    private static final ObjectFactory GML_OF = new ObjectFactory();

    protected abstract T render(SpaceWeatherAdvisoryType swx, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisoryAmd82 input, final ConversionHints hints) {
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

        input.getReplaceAdvisoryNumbers()
                .forEach(advisoryNumber -> swxType.getReplacedAdvisoryNumber().add(advisoryNumber.asAdvisoryNumber()));

        final StringWithNilReasonType remarkType = create(StringWithNilReasonType.class);
        if (input.getRemarks().isPresent() && !input.getRemarks().get().isEmpty()) {
            remarkType.setValue(String.join(" ", input.getRemarks().get()));
        } else {
            remarkType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
        }
        swxType.setRemarks(remarkType);

        final SpaceWeatherEffectType effectType = SpaceWeatherEffectType.fromValue(input.getEffect().getCode());
        swxType.getEffect().add(effectType);

        if (input.getAnalyses().size() == REQUIRED_NUMBER_OF_ANALYSES) {
            final SpaceWeatherRegionHandler regionIdList = new SpaceWeatherRegionHandler(input.getAnalyses());
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
            final T rendered = this.render(swxType, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
    }

    protected void updateMessageMetadata(final SpaceWeatherAdvisoryAmd82 source, final ConversionResult<?> results, final SpaceWeatherAdvisoryType target)
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

    private SpaceWeatherAnalysisPropertyType toSpaceWeatherAnalysisPropertyType(
            final SpaceWeatherAdvisoryAnalysis analysis,
            final List<SpaceWeatherRegionHandler.RegionId> regionList) {
        final SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
        final SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);

        if (analysis.getAnalysisType() == SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION) {
            analysisType.setTimeIndicator(TimeIndicatorType.OBSERVATION);
        } else {
            analysisType.setTimeIndicator(TimeIndicatorType.FORECAST);
        }

        analysisType.setId(UUID_PREFIX + UUID.randomUUID());
        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, prop -> getAnalysisTime(prop, analysis.getTime())));

        // Handle NIL reasons for the entire analysis
        if (analysis.getNilReason().isPresent()) {
            final SpaceWeatherIntensityAndRegionPropertyType nilProperty = create(SpaceWeatherIntensityAndRegionPropertyType.class);
            switch (analysis.getNilReason().get()) {
                case NO_INFORMATION_AVAILABLE:
                    nilProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING);
                    break;
                case NO_SWX_EXPECTED:
                    nilProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                    break;
            }
            analysisType.getIntensityAndRegion().add(nilProperty);
        } else {
            // Process analysis intensity and regions
            int regionCounter = 0;
            for (final SpaceWeatherIntensityAndRegion intensityAndRegion : analysis.getIntensityAndRegions()) {
                final SpaceWeatherIntensityAndRegionPropertyType iarProperty = create(SpaceWeatherIntensityAndRegionPropertyType.class);
                final SpaceWeatherIntensityAndRegionType intensityAndRegionType = create(SpaceWeatherIntensityAndRegionType.class);
                intensityAndRegionType.setId(UUID_PREFIX + UUID.randomUUID());

                final SpaceWeatherIntensityType intensityType = SpaceWeatherIntensityType.fromValue(intensityAndRegion.getIntensity().getCode());
                intensityAndRegionType.setIntensity(intensityType);

                for (int i = 0; i < intensityAndRegion.getRegions().size(); i++) {
                    final SpaceWeatherRegionHandler.RegionId regionId = regionList.get(regionCounter);
                    final SpaceWeatherRegion region = regionId.getRegion();
                    final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);

                    if (regionId.isDuplicate()) {
                        regionProperty.setHref("#" + regionId.getId());
                    } else {
                        final SpaceWeatherRegionType regionType = create(SpaceWeatherRegionType.class);
                        regionType.setId(regionId.getId());

                        if (region.getAirSpaceVolume().isPresent()) {
                            regionType.setLocation(
                                    create(AirspaceVolumePropertyType.class, prop -> getAirspaceVolumeProperty(prop, region.getAirSpaceVolume().get())));
                        } else {
                            final AirspaceVolumePropertyType airspaceVolumePropertyType = create(AirspaceVolumePropertyType.class);
                            airspaceVolumePropertyType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN);
                            regionType.setLocation(airspaceVolumePropertyType);
                        }

                        final SpaceWeatherLocationType locationType = create(SpaceWeatherLocationType.class);
                        if (region.getLocationIndicator().isPresent()) {
                            locationType.setHref(region.getLocationIndicator().get().asWMOCodeListValue());
                        } else {
                            locationType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
                        }
                        regionType.getLocationIndicator().add(locationType);
                        regionProperty.setSpaceWeatherRegion(regionType);
                    }

                    intensityAndRegionType.getRegion().add(regionProperty);
                    regionCounter++;
                }

                iarProperty.setSpaceWeatherIntensityAndRegion(intensityAndRegionType);
                analysisType.getIntensityAndRegion().add(iarProperty);
            }
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
            final SurfacePropertyType surfaceProperty = createAixm511fullSurface(volume.getHorizontalProjection().get(), UUID_PREFIX + UUID.randomUUID());
            airspaceVolumeType.setHorizontalProjection(surfaceProperty);
        }

        volume.getLowerLimit().ifPresent(limit -> {
            AbstractIWXXMAixm511FullSerializer.toValDistanceVertical(limit).ifPresent(airspaceVolumeType::setLowerLimit);
            airspaceVolumeType.setLowerLimitReference(create(CodeVerticalReferenceType.class,
                    codeVerticalReferenceType -> volume.getLowerLimitReference().ifPresent(codeVerticalReferenceType::setValue)));
        });

        volume.getUpperLimit().ifPresent(limit -> {
            AbstractIWXXMAixm511FullSerializer.toValDistanceVertical(limit).ifPresent(airspaceVolumeType::setUpperLimit);
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
