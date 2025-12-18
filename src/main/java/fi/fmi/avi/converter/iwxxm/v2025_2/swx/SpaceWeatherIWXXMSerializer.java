package fi.fmi.avi.converter.iwxxm.v2025_2.swx;

import aero.aixm511full.*;
import aero.aixm511full.SurfacePropertyType;
import fi.fmi.avi.converter.*;
import fi.fmi.avi.converter.iwxxm.AbstractIWXXMAixm511FullSerializer;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.converter.iwxxm.v2025_2.AbstractIWXXM20252Serializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.amd82.*;
import icao.iwxxm2025_2.*;
import icao.iwxxm2025_2.AirspaceVolumePropertyType;
import icao.iwxxm2025_2.UnitPropertyType;
import net.opengis.gml32.*;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public abstract class SpaceWeatherIWXXMSerializer<T> extends AbstractIWXXM20252Serializer<SpaceWeatherAdvisoryAmd82, T> {
    private static final int REQUIRED_NUMBER_OF_ANALYSES = 5;

    private static StringWithNilReasonType createRemarksType(final List<String> remarks) {
        final StringWithNilReasonType remarkType = create(StringWithNilReasonType.class);
        if (remarks.isEmpty()) {
            remarkType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
        } else {
            remarkType.setValue(String.join(" ", remarks));
        }
        return remarkType;
    }

    private static SpaceWeatherIntensityAndRegionPropertyType createNilReasonProperty(
            final SpaceWeatherAdvisoryAnalysis.NilReason nilReason) {
        final SpaceWeatherIntensityAndRegionPropertyType nilProperty = create(SpaceWeatherIntensityAndRegionPropertyType.class);
        switch (nilReason) {
            case NO_INFORMATION_AVAILABLE:
                nilProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_MISSING);
                break;
            case NO_SWX_EXPECTED:
                nilProperty.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_NOTHING_OF_OPERATIONAL_SIGNIFICANCE);
                break;
        }
        return nilProperty;
    }

    private static AirspaceVolumePropertyType createAirspaceVolumeProperty(final SpaceWeatherRegion region) {
        return create(AirspaceVolumePropertyType.class, prop -> {
            if (region.getAirSpaceVolume().isPresent()) {
                setAirspaceVolume(prop, region.getAirSpaceVolume().get());
            } else {
                prop.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_UNKNOWN);
            }
        });
    }

    private static SpaceWeatherLocationType createLocationType(final SpaceWeatherRegion region) {
        return create(SpaceWeatherLocationType.class, locationType -> {
            if (region.getLocationIndicator().isPresent()) {
                locationType.setHref(region.getLocationIndicator().get().asWMOCodeListValue());
            } else {
                locationType.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
            }
        });
    }

    private static SpaceWeatherRegionPropertyType createRegionProperty(
            final SpaceWeatherRegion region,
            final SpaceWeatherIntensityAndRegionHandler handler) {
        final SpaceWeatherRegionPropertyType regionProperty = create(SpaceWeatherRegionPropertyType.class);
        final SpaceWeatherIntensityAndRegionHandler.RegionIdAssignment regionIdAssignment = handler.registerRegion(region);

        if (regionIdAssignment.isNew()) {
            final SpaceWeatherRegionType regionType = create(SpaceWeatherRegionType.class, type -> {
                type.setId(regionIdAssignment.getId());
                type.setLocation(createAirspaceVolumeProperty(regionIdAssignment.getRegion()));
                type.getLocationIndicator().add(createLocationType(regionIdAssignment.getRegion()));
            });
            regionProperty.setSpaceWeatherRegion(regionType);
        } else {
            regionProperty.setHref("#" + regionIdAssignment.getId());
        }

        return regionProperty;
    }

    private static void processIntensityAndRegions(
            final List<SpaceWeatherIntensityAndRegion> intensityAndRegions,
            final SpaceWeatherAnalysisType analysisType,
            final SpaceWeatherIntensityAndRegionHandler handler) {
        for (final SpaceWeatherIntensityAndRegion iar : intensityAndRegions) {
            final SpaceWeatherIntensityAndRegionPropertyType iarProperty = create(SpaceWeatherIntensityAndRegionPropertyType.class);
            final SpaceWeatherIntensityAndRegionHandler.IdAssignment idAssignment = handler.register(iar.getIntensity(), iar.getRegions());

            if (idAssignment.isNew()) {
                final SpaceWeatherIntensityAndRegionType intensityAndRegionType = create(SpaceWeatherIntensityAndRegionType.class);
                intensityAndRegionType.setId(idAssignment.getId());

                final SpaceWeatherIntensityType intensityType = SpaceWeatherIntensityType.fromValue(iar.getIntensity().getCode());
                intensityAndRegionType.setIntensity(intensityType);

                for (final SpaceWeatherRegion region : iar.getRegions()) {
                    intensityAndRegionType.getRegion().add(createRegionProperty(region, handler));
                }

                iarProperty.setSpaceWeatherIntensityAndRegion(intensityAndRegionType);
            } else {
                iarProperty.setHref("#" + idAssignment.getId());
            }

            analysisType.getIntensityAndRegion().add(iarProperty);
        }
    }

    private static SpaceWeatherAnalysisPropertyType toSpaceWeatherAnalysisPropertyType(
            final SpaceWeatherAdvisoryAnalysis analysis,
            final SpaceWeatherIntensityAndRegionHandler handler) {
        final SpaceWeatherAnalysisPropertyType propertyType = create(SpaceWeatherAnalysisPropertyType.class);
        final SpaceWeatherAnalysisType analysisType = create(SpaceWeatherAnalysisType.class);

        if (analysis.getAnalysisType() == SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION) {
            analysisType.setTimeIndicator(TimeIndicatorType.OBSERVATION);
        } else {
            analysisType.setTimeIndicator(TimeIndicatorType.FORECAST);
        }

        analysisType.setId(getUUID());
        analysisType.setPhenomenonTime(create(AbstractTimeObjectPropertyType.class, prop ->
                setAnalysisTime(prop, analysis.getTime())));

        if (analysis.getNilReason().isPresent()) {
            analysisType.getIntensityAndRegion().add(createNilReasonProperty(analysis.getNilReason().get()));
        } else {
            processIntensityAndRegions(analysis.getIntensityAndRegions(), analysisType, handler);
        }

        propertyType.setSpaceWeatherAnalysis(analysisType);
        return propertyType;
    }

    @SuppressWarnings("unchecked")
    private static void setAnalysisTime(final AbstractTimeObjectPropertyType prop, final PartialOrCompleteTimeInstant time) {
        final TimeInstantType timeInstant = createTimeInstant(time);
        final JAXBElement<?> jaxbTimeInstant = wrap(timeInstant, TimeInstantType.class);
        prop.setAbstractTimeObject((JAXBElement<AbstractTimeObjectType>) jaxbTimeInstant);
    }

    private static void setAirspaceVolume(final AirspaceVolumePropertyType prop, final AirspaceVolume volume) {
        final AirspaceVolumeType airspaceVolumeType = create(AirspaceVolumeType.class);
        if (volume.getHorizontalProjection().isPresent()) {
            airspaceVolumeType.setId(getUUID());
            final SurfacePropertyType surfaceProperty =
                    createSurface(volume.getHorizontalProjection().get(), getUUID());
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

    private static void setIssuingCenter(final UnitPropertyType prop, final IssuingCenter issuingCenter) {
        final UnitType unitType = create(UnitType.class, unit -> {
            unit.setId(getUUID());
            unit.getTimeSlice().add(create(UnitTimeSlicePropertyType.class, sliceProp ->
                    sliceProp.setUnitTimeSlice(create(UnitTimeSliceType.class, slice -> {
                        slice.setId(getUUID());
                        slice.setInterpretation("SNAPSHOT");
                        slice.setValidTime(new TimePrimitivePropertyType());
                        issuingCenter.getName().ifPresent(name ->
                                slice.setUnitName(create(TextNameType.class, tnt -> tnt.setValue(name))));
                        issuingCenter.getType().ifPresent(type ->
                                slice.setType(create(CodeUnitType.class, cut -> cut.setValue(type))));
                        issuingCenter.getDesignator().ifPresent(designator ->
                                slice.setDesignator(create(CodeOrganisationDesignatorType.class, codt ->
                                        codt.setValue(designator))));
                    }))));
        });
        prop.setUnit(unitType);
    }

    private static void setNextAdvisory(final TimeInstantPropertyType prop, final NextAdvisory nextAdvisory) {
        if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_AT || nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
            nextAdvisory.getTime().ifPresent(time -> {
                final TimeInstantType timeInstant = createTimeInstant(time);
                if (nextAdvisory.getTimeSpecifier() == NextAdvisory.Type.NEXT_ADVISORY_BY) {
                    timeInstant.getTimePosition().setIndeterminatePosition(TimeIndeterminateValueType.BEFORE);
                }
                prop.setTimeInstant(timeInstant);
            });
        } else {
            prop.getNilReason().add(AviationCodeListUser.CODELIST_VALUE_NIL_REASON_INAPPLICABLE);
        }
    }

    protected abstract T render(SpaceWeatherAdvisoryType swx, ConversionHints hints) throws ConversionException;

    protected abstract IssueList validate(final T output, final XMLSchemaInfo schemaInfo, final ConversionHints hints)
            throws ConversionException;

    @Override
    public ConversionResult<T> convertMessage(final SpaceWeatherAdvisoryAmd82 input, final ConversionHints hints) {
        final ConversionResult<T> result = new ConversionResult<>();

        final SpaceWeatherAdvisoryType swxType = create(SpaceWeatherAdvisoryType.class);
        swxType.setId(getUUID());

        if (input.getIssueTime().isPresent()) {
            swxType.setIssueTime(create(TimeInstantPropertyType.class, prop ->
                    prop.setTimeInstant(createTimeInstant(input.getIssueTime().get()))));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Issue time is missing"));
            return result;
        }

        swxType.setIssuingSpaceWeatherCentre(create(UnitPropertyType.class, prop ->
                setIssuingCenter(prop, input.getIssuingCenter())));
        swxType.setAdvisoryNumber(create(StringWithNilReasonType.class, prop ->
                prop.setValue(input.getAdvisoryNumber().asAdvisoryNumber())));
        input.getReplaceAdvisoryNumbers().forEach(advisoryNumber ->
                swxType.getReplacedAdvisoryNumber().add(advisoryNumber.asAdvisoryNumber()));
        swxType.setRemarks(createRemarksType(input.getRemarks().orElse(Collections.emptyList())));

        final SpaceWeatherEffectType effectType = SpaceWeatherEffectType.fromValue(input.getEffect().getCode());
        swxType.getEffect().add(effectType);

        if (input.getAnalyses().size() == REQUIRED_NUMBER_OF_ANALYSES) {
            final SpaceWeatherIntensityAndRegionHandler handler = new SpaceWeatherIntensityAndRegionHandler();
            for (final SpaceWeatherAdvisoryAnalysis analysis : input.getAnalyses()) {
                swxType.getAnalysis().add(toSpaceWeatherAnalysisPropertyType(analysis, handler));
            }
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                    "Expected " + REQUIRED_NUMBER_OF_ANALYSES + " analysis objects, but found " + input.getAnalyses().size()));
            return result;
        }

        swxType.setNextAdvisoryTime(create(TimeInstantPropertyType.class, prop ->
                setNextAdvisory(prop, input.getNextAdvisory())));

        try {
            setReportCommonMetadata(input, result, swxType);
            final T rendered = render(swxType, hints);
            result.addIssue(validate(rendered, getSchemaInfo(), hints));
            result.setConvertedMessage(rendered);
        } catch (final ConversionException e) {
            result.setStatus(ConversionResult.Status.FAIL);
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, "Unable to render IWXXM message", e));
        }
        return result;
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
