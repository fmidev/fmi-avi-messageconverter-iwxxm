package fi.fmi.avi.converter.iwxxm.v3_0.swx;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

        if (result.getConversionIssues().size() > 0) {
            return null;
        }

        final List<SpaceWeatherAnalysisProperties> analysisPropertiesList = properties.getList(SpaceWeatherAdvisoryProperties.Name.ANALYSES,
                SpaceWeatherAnalysisProperties.class);

        //ANALYSES
        final List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();
        for (final SpaceWeatherAnalysisProperties analysisProperties : analysisPropertiesList) {
            final SpaceWeatherAdvisoryAnalysisImpl.Builder spaceWeatherAnalysis = SpaceWeatherAdvisoryAnalysisImpl.builder();

            final Optional<PartialOrCompleteTimeInstant> analysisTime = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TIME,
                    PartialOrCompleteTimeInstant.class);
            analysisTime.ifPresent(spaceWeatherAnalysis::setTime);

            final List<SpaceWeatherRegionProperties> regionPropertiesList = analysisProperties.getList(SpaceWeatherAnalysisProperties.Name.REGION,
                    SpaceWeatherRegionProperties.class);

            //REGIONS
            final List<SpaceWeatherRegion> weatherRegions = new ArrayList<>();
            for (final SpaceWeatherRegionProperties regionProperties : regionPropertiesList) {
                final SpaceWeatherRegionImpl.Builder spaceWeatherRegion = SpaceWeatherRegionImpl.builder();

                final Optional<AirspaceVolume> airspaceVolume = regionProperties.get(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, AirspaceVolume.class);
                airspaceVolume.ifPresent(spaceWeatherRegion::setAirSpaceVolume);

                final Optional<SpaceWeatherRegion.SpaceWeatherLocation> locationIndicator = regionProperties.get(
                        SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR, SpaceWeatherRegion.SpaceWeatherLocation.class);
                locationIndicator.ifPresent(spaceWeatherRegion::setLocationIndicator);
                weatherRegions.add(spaceWeatherRegion.build());
            }

            //Set region
            spaceWeatherAnalysis.addAllRegions(weatherRegions);
            final Optional<SpaceWeatherAdvisoryAnalysis.Type> type = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE,
                    SpaceWeatherAdvisoryAnalysis.Type.class);
            type.ifPresent(spaceWeatherAnalysis::setAnalysisType);

            final Optional<Boolean> noInformation = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, Boolean.class);
            if (noInformation.isPresent()) {
                spaceWeatherAnalysis.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE);
            }

            final Optional<Boolean> notExpected = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, Boolean.class);
            if (notExpected.isPresent()) {
                spaceWeatherAnalysis.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED);
            }

            analyses.add(spaceWeatherAnalysis.build());
        }

        //ADVISORY
        final SpaceWeatherAdvisoryImpl.Builder spaceWeatherAdvisory = SpaceWeatherAdvisoryImpl.builder();

        spaceWeatherAdvisory.addAllAnalyses(analyses);

        final Optional<PartialOrCompleteTimeInstant> issueTime = properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUE_TIME,
                PartialOrCompleteTimeInstant.class);
        issueTime.ifPresent(spaceWeatherAdvisory::setIssueTime);
        final Optional<IssuingCenter> issuer = properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER, IssuingCenter.class);
        issuer.ifPresent(spaceWeatherAdvisory::setIssuingCenter);

        final Optional<AdvisoryNumber> advisoryNumber = properties.get(SpaceWeatherAdvisoryProperties.Name.ADVISORY_NUMBER, AdvisoryNumber.class);
        advisoryNumber.ifPresent(spaceWeatherAdvisory::setAdvisoryNumber);

        final Optional<AdvisoryNumber> replaceAdvisoryNumber = properties.get(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER,
                AdvisoryNumber.class);
        replaceAdvisoryNumber.ifPresent(spaceWeatherAdvisory::setReplaceAdvisoryNumber);
        final List<SpaceWeatherPhenomenon> phenomena = properties.getList(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, SpaceWeatherPhenomenon.class);
        spaceWeatherAdvisory.addAllPhenomena(phenomena);

        final Optional<List> remarks = properties.get(SpaceWeatherAdvisoryProperties.Name.REMARKS, List.class);
        remarks.ifPresent(s -> spaceWeatherAdvisory.setRemarks(s));

        final Optional<NextAdvisory> nextAdvisory = properties.get(SpaceWeatherAdvisoryProperties.Name.NEXT_ADVISORY, NextAdvisory.class);
        nextAdvisory.ifPresent(spaceWeatherAdvisory::setNextAdvisory);

        properties.get(SpaceWeatherAdvisoryProperties.Name.REPORT_METADATA, GenericReportProperties.class).ifPresent((metaProps) -> {
            metaProps.get(GenericReportProperties.Name.REPORT_STATUS, AviationWeatherMessage.ReportStatus.class)
                    .ifPresent(spaceWeatherAdvisory::setReportStatus);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE, AviationCodeListUser.PermissibleUsage.class)
                    .ifPresent(spaceWeatherAdvisory::setPermissibleUsage);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_REASON, AviationCodeListUser.PermissibleUsageReason.class)
                    .ifPresent(spaceWeatherAdvisory::setPermissibleUsageReason);
            metaProps.get(GenericReportProperties.Name.PERMISSIBLE_USAGE_SUPPLEMENTARY, String.class)
                    .ifPresent(spaceWeatherAdvisory::setPermissibleUsageSupplementary);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_ID, String.class).ifPresent(spaceWeatherAdvisory::setTranslatedBulletinID);
            metaProps.get(GenericReportProperties.Name.TRANSLATED_BULLETIN_RECEPTION_TIME, ZonedDateTime.class)
                    .ifPresent(spaceWeatherAdvisory::setTranslatedBulletinReceptionTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_DESIGNATOR, String.class)
                    .ifPresent(spaceWeatherAdvisory::setTranslationCentreDesignator);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_CENTRE_NAME, String.class).ifPresent(spaceWeatherAdvisory::setTranslationCentreName);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_TIME, ZonedDateTime.class).ifPresent(spaceWeatherAdvisory::setTranslationTime);
            metaProps.get(GenericReportProperties.Name.TRANSLATION_FAILED_TAC, String.class).ifPresent(spaceWeatherAdvisory::setTranslatedTAC); //!!!
        });
        return spaceWeatherAdvisory.build();
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
