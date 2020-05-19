package fi.fmi.avi.converter.iwxxm.v3_0.SpaceWeatherAdvisory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMConverterBase;
import fi.fmi.avi.converter.iwxxm.ReferredObjectRetrievalContext;
import fi.fmi.avi.converter.iwxxm.v3_0.AbstractIWXXM30Parser;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AdvisoryNumber;
import fi.fmi.avi.model.SpaceWeatherAdvisory.AirspaceVolume;
import fi.fmi.avi.model.SpaceWeatherAdvisory.NextAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisory;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.SpaceWeatherAdvisory.SpaceWeatherRegion;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.IssuingCenterImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherAdvisoryAnalysisImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.SpaceWeatherAdvisory.immutable.SpaceWeatherRegionImpl;
import icao.iwxxm30.SpaceWeatherAdvisoryType;

public abstract class SpaceWeatherIWXXMParser<T> extends AbstractIWXXM30Parser<T, SpaceWeatherAdvisory> {
    @Override
    protected SpaceWeatherAdvisory createPOJO(final Object source, final ReferredObjectRetrievalContext refCtx,
            final ConversionResult<SpaceWeatherAdvisory> result, final ConversionHints hints) {
        Objects.requireNonNull(source, "source cannot be null");
        SpaceWeatherAdvisoryType input;

        if (SpaceWeatherAdvisoryType.class.isAssignableFrom(source.getClass())) {
            input = (SpaceWeatherAdvisoryType) source;
        } else if (JAXBElement.class.isAssignableFrom(source.getClass())) {
            JAXBElement<?> je = (JAXBElement<?>) source;
            if (SpaceWeatherAdvisoryType.class.isAssignableFrom(je.getDeclaredType())) {
                input = (SpaceWeatherAdvisoryType) je.getValue();
            } else {
                throw new IllegalArgumentException("Source is not a SWX JAXB element");
            }
        } else {
            throw new IllegalArgumentException("Source is not a SWX JAXB element");
        }
        SpaceWeatherAdvisoryProperties properties = new SpaceWeatherAdvisoryProperties();

        List<ConversionIssue> issues = SpaceWeatherAdvisoryIWXXMScanner.collectSpaceWeatherAdvisoryProperties(input, refCtx, properties, hints);
        result.addIssue(issues);

        if (result.getConversionIssues().size() > 0) {
            return null;
        }

        List<SpaceWeatherAnalysisProperties> analysisPropertiesList = properties.get(SpaceWeatherAdvisoryProperties.Name.ANALYSES, List.class).get();

        //ANALYSES
        List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();
        for (SpaceWeatherAnalysisProperties analysisProperties : analysisPropertiesList) {
            SpaceWeatherAdvisoryAnalysisImpl.Builder spaceWeatherAnalysis = SpaceWeatherAdvisoryAnalysisImpl.builder();

            Optional<PartialOrCompleteTimeInstant> analysisTime = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TIME,
                    PartialOrCompleteTimeInstant.class);
            if(analysisTime.isPresent()) {
                spaceWeatherAnalysis.setTime(analysisTime.get());
            }

            List<SpaceWeatherRegionProperties> regionPropertiesList = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.REGION, List.class).get();

            //REGIONS
            List<SpaceWeatherRegion> weatherRegions = new ArrayList<>();
            for (SpaceWeatherRegionProperties regionProperties : regionPropertiesList) {
                SpaceWeatherRegionImpl.Builder spaceWeatherRegion = SpaceWeatherRegionImpl.builder();

                Optional<AirspaceVolume> airspaceVolume = regionProperties.get(SpaceWeatherRegionProperties.Name.AIRSPACE_VOLUME, AirspaceVolume.class);
                if(airspaceVolume.isPresent()) {
                    spaceWeatherRegion.setAirSpaceVolume(airspaceVolume);
                }


                Optional<String> locationIndicator = regionProperties.get(SpaceWeatherRegionProperties.Name.LOCATION_INDICATOR, String.class);
                if (locationIndicator.isPresent()) {
                    spaceWeatherRegion.setLocationIndicator(locationIndicator);
                }
                weatherRegions.add(spaceWeatherRegion.build());
            }

            //Set region
            spaceWeatherAnalysis.setRegion(weatherRegions);
            Optional<Enum> type = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.ANALYSIS_TYPE, Enum.class);
            if (type.isPresent()) {
                spaceWeatherAnalysis.setAnalysisType((SpaceWeatherAdvisoryAnalysis.Type) type.get());
            }

            Optional<Boolean> noInformation = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_INFORMATION_AVAILABLE, Boolean.class);
            if (noInformation.isPresent()) {
                spaceWeatherAnalysis.setNoInformationAvailable(noInformation.get());
            } else {
                spaceWeatherAnalysis.setNoInformationAvailable(false);
            }

            Optional<Boolean> notExpected = analysisProperties.get(SpaceWeatherAnalysisProperties.Name.NO_PHENOMENON_EXPECTED, Boolean.class);
            if (notExpected.isPresent()) {
                spaceWeatherAnalysis.setNoPhenomenaExpected(notExpected.get());
            } else {
                spaceWeatherAnalysis.setNoPhenomenaExpected(false);
            }

            analyses.add(spaceWeatherAnalysis.build());
        }

        //ADVISORY
        SpaceWeatherAdvisoryImpl.Builder spaceWeatherAdvisory = SpaceWeatherAdvisoryImpl.builder();
        spaceWeatherAdvisory.addAllAnalyses(analyses);

        Optional<PartialOrCompleteTimeInstant> issueTime = properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUE_TIME, PartialOrCompleteTimeInstant.class);
        if (issueTime.isPresent()) {
            spaceWeatherAdvisory.setIssueTime(issueTime);
        }
        Optional<String> issuer = properties.get(SpaceWeatherAdvisoryProperties.Name.ISSUING_CENTER_NAME, String.class);
        if (issuer.isPresent()) {
            spaceWeatherAdvisory.setIssuingCenter(IssuingCenterImpl.builder().setName(issuer.get()).build());
        }
        final Optional<AdvisoryNumber> advisoryNumber = properties.get(SpaceWeatherAdvisoryProperties.Name.ADVISORY_NUMBER, AdvisoryNumber.class);
        if (advisoryNumber.isPresent()) {
            spaceWeatherAdvisory.setAdvisoryNumber(advisoryNumber.get());
        }
        Optional<AdvisoryNumber> replaceAdvisoryNumber = properties.get(SpaceWeatherAdvisoryProperties.Name.REPLACE_ADVISORY_NUMBER, AdvisoryNumber.class);
        if (replaceAdvisoryNumber.isPresent()) {
            spaceWeatherAdvisory.setReplaceAdvisoryNumber(replaceAdvisoryNumber);
        }
        final List<String> phenomena = properties.getList(SpaceWeatherAdvisoryProperties.Name.PHENOMENA, String.class);
        spaceWeatherAdvisory.addAllPhenomena(phenomena);

        final Optional<String> remarks = properties.get(SpaceWeatherAdvisoryProperties.Name.REMARKS, String.class);
        remarks.ifPresent(s -> spaceWeatherAdvisory.setRemarks(Collections.singletonList(s)));

        final Optional<NextAdvisory> nextAdvisory = properties.get(SpaceWeatherAdvisoryProperties.Name.NEXT_ADVISORY, NextAdvisory.class);
        nextAdvisory.ifPresent(spaceWeatherAdvisory::setNextAdvisory);

        return spaceWeatherAdvisory.build();
    }

    public static class FromDOM extends SpaceWeatherIWXXMParser<Document> {
        @Override
        protected Document parseAsDom(final Document input) throws ConversionException {
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
