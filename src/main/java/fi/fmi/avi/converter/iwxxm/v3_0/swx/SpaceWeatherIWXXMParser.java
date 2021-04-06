package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.GenericReportProperties;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Parser;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.IssuingCenter;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryAnalysisImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherRegionImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public abstract class SpaceWeatherIWXXMParser<T> extends AbstractIWXXM30Parser<T, SpaceWeatherAdvisory> {
    @Override
    protected SpaceWeatherAdvisory createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<SpaceWeatherAdvisory> result, final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        final SpaceWeatherAdvisoryType input;

        if (SpaceWeatherAdvisoryType.class.isAssignableFrom(source.getClass())) {
            input = (SpaceWeatherAdvisoryType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            final JAXBElement<?> je = (JAXBElement<?>) source;
            if (SpaceWeatherAdvisoryType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SpaceWeatherAdvisoryType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SWX JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SWX JAXB element");
        }
        final SpaceWeatherAdvisoryProperties properties = new SpaceWeatherAdvisoryProperties();

        final List<ConversionIssue> issues = SpaceWeatherAdvisoryIWXXMScanner.collectSpaceWeatherAdvisoryProperties(input, refCtx, properties, hints);
        result.addIssue(issues);

        if (!result.getConversionIssues().isEmpty()) {
            return null;
        }

        final List<SpaceWeatherAnalysisProperties> analysisPropertiesList = properties.getList(SpaceWeatherAdvisoryProperties.Name.ANALYSES,
                SpaceWeatherAnalysisProperties.class);

        //ANALYSES
        final List<SpaceWeatherAdvisoryAnalysis> analyses = analysisPropertiesList.stream()//
                .map(this::toSpaceWeatherAdvisoryAnalysis)//
                .collect(toImmutableList());

        //ADVISORY
        final SpaceWeatherAdvisoryImpl.Builder builder = SpaceWeatherAdvisoryImpl.builder();

        builder.addAllAnalyses(analyses);

        properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class).ifPresent(builder::setIssueTime);
        properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER, IssuingCenter.class).ifPresent(builder::setIssuingCenter);

        properties.get(SpaceWeatherAdvisoryProperties.Name.ADVISORY_NUMBER, AdvisoryNumber.class).ifPresent(builder::setAdvisoryNumber);
        properties.get(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER, AdvisoryNumber.class).ifPresent(builder::setReplaceAdvisoryNumber);

        builder.addAllPhenomena(properties.getList(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, SpaceWeatherPhenomenon.class));

        if (properties.contains(SpaceWeatherAdvisoryProperties.Name.REMARKS)) {
            builder.setRemarks(properties.getList(SpaceWeatherAdvisoryProperties.Name.REMARKS, String.class));
        }

        properties.get(SpaceWeatherAdvisoryProperties.Name.NEXT_ADVISORY, NextAdvisory.class).ifPresent(builder::setNextAdvisory);

        properties.get(SpaceWeatherAdvisoryProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent(metaProps -> {
            metaProps.get(GenericReportProperties.Name.REPORT_STATUS, AviationWeatherMessage.ReportStatus.class).ifPresent(builder::setReportStatus);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class).ifPresent(builder::setPermissibleUsage);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                    .ifPresent(builder::setPermissibleUsageReason);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class).ifPresent(builder::setPermissibleUsageSupplementary);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).ifPresent(builder::setTranslatedBulletinID);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                    .ifPresent(builder::setTranslatedBulletinReceptionTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class).ifPresent(builder::setTranslationCentreDesignator);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).ifPresent(builder::setTranslationCentreName);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).ifPresent(builder::setTranslationTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).ifPresent(builder::setTranslatedTAC); //!!!
        });
        return builder.build();
    }

    private SpaceWeatherAdvisoryAnalysisImpl toSpaceWeatherAdvisoryAnalysis(final SpaceWeatherAnalysisProperties analysisProperties) {
        final SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();

        analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TIME, PartialOrCompleteTimeInstant.class).ifPresent(builder::setTime);

        builder.addAllRegions(analysisProperties.getList(SpaceWeatherAnalysisProperties.Name.REGION, SpaceWeatherRegionProperties.class).stream()//
                .map(regionProperties -> {
                    final SpaceWeatherRegionImpl.Builder regionBuilder = SpaceWeatherRegionImpl.builder();
                    regionProperties.get(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, AirspaceVolume.class).ifPresent(regionBuilder::setAirSpaceVolume);
                    regionProperties.get(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR, SpaceWeatherRegion.SpaceWeatherLocation.class)
                            .ifPresent(regionBuilder::setLocationIndicator);
                    return regionBuilder;
                })//
                .filter(regionBuilder -> regionBuilder.getAirSpaceVolume().isPresent() || regionBuilder.getLocationIndicator().isPresent())//
                .map(SpaceWeatherRegionImpl.Builder::build));
        analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, SpaceWeatherAdvisoryAnalysis.Type.class).ifPresent(builder::setAnalysisType);

        final Optional<Boolean> noInformation = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, Boolean.class);
        if (noInformation.isPresent()) {
            builder.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE);
        }

        final Optional<Boolean> notExpected = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, Boolean.class);
        if (notExpected.isPresent()) {
            builder.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED);
        }

        return builder.build();
    }

    public static class FromDOM extends SpaceWeatherIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) {
            return input;
        }
    }

    public static class FromString extends SpaceWeatherIWXXMParser<String> {
        @Override
        protected Document parseAsDom(final String input) throws ConversionException {
            return IWXXMConverterBase.parseStringToDOM(input);
        }
    }
}
